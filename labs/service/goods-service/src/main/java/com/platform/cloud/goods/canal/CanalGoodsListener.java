package com.platform.cloud.goods.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.platform.cloud.bootstrap.goods.spu.entity.GoodsEntity;
import com.platform.cloud.bootstrap.goods.spu.mapper.GoodsMapper;
import com.platform.cloud.goods.search.GoodsSearchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canal TCP Pull 客户端。后台线程不停从 canal-server :11111 拉 binlog 事件，
 * 根据表名和操作类型同步到 ES。
 * <p>仅 goods 表（SPU）变化时回查 DB 并 upsert/delete ES 文档。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanalGoodsListener {

    @Value("${canal.host:localhost}")
    private String canalHost;

    @Value("${canal.port:11111}")
    private int canalPort;

    @Value("${canal.destination:example}")
    private String destination;

    @Value("${canal.subscribe:platform_cloud\\.goods.*}")
    private String subscribe;

    private final GoodsMapper goodsMapper;
    private final GoodsSearchService searchService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @PostConstruct
    public void start() {
        running.set(true);
        worker = new Thread(this::loop, "canal-goods-listener");
        worker.setDaemon(true);
        worker.start();
        log.info("Canal listener started: {}:{} destination={} filter={}", canalHost, canalPort, destination, subscribe);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (worker != null) worker.interrupt();
    }

    private void loop() {
        while (running.get()) {
            CanalConnector conn = null;
            try {
                conn = CanalConnectors.newSingleConnector(
                        new InetSocketAddress(canalHost, canalPort),
                        destination, "", "");
                conn.connect();
                conn.subscribe(subscribe);
                conn.rollback();
                log.info("[Canal] connected, polling for events...");
                while (running.get()) {
                    Message message = conn.getWithoutAck(100, 1000L, java.util.concurrent.TimeUnit.MILLISECONDS);
                    long batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {
                        Thread.sleep(500);
                    } else {
                        handle(message.getEntries());
                        conn.ack(batchId);
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.warn("[Canal] connection lost, retry in 5s: {}", e.getMessage());
                    sleepQuiet(5000);
                }
            } finally {
                if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private void handle(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) continue;
            CanalEntry.RowChange rc;
            try {
                rc = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (InvalidProtocolBufferException ex) {
                log.warn("[Canal] parse rowchange failed", ex);
                continue;
            }
            String table = entry.getHeader().getTableName();
            CanalEntry.EventType type = rc.getEventType();
            log.info("[Canal] event db={} table={} type={} rows={}",
                    entry.getHeader().getSchemaName(), table, type, rc.getRowDatasCount());

            if (!"goods".equals(table)) continue;

            for (CanalEntry.RowData row : rc.getRowDatasList()) {
                Long id = extractId(type == CanalEntry.EventType.DELETE ? row.getBeforeColumnsList() : row.getAfterColumnsList());
                if (id == null) continue;
                if (type == CanalEntry.EventType.DELETE) {
                    searchService.delete(id);
                    log.info("[Canal] ES 删除 goodsId={}", id);
                } else {
                    GoodsEntity full = goodsMapper.selectById(id);
                    if (full != null) {
                        searchService.index(full);
                        log.info("[Canal] ES upsert goodsId={} name={}", id, full.getName());
                    }
                }
            }
        }
    }

    private Long extractId(List<CanalEntry.Column> cols) {
        for (CanalEntry.Column c : cols) {
            if ("id".equalsIgnoreCase(c.getName())) {
                try { return Long.parseLong(c.getValue()); }
                catch (NumberFormatException e) { return null; }
            }
        }
        return null;
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
