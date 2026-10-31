package com.platform.cloud.goods.search;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.goods.spu.entity.GoodsEntity;
import com.platform.cloud.bootstrap.goods.spu.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSearchService {

    private final GoodsSearchRepository repository;
    private final ElasticsearchOperations es;
    private final GoodsMapper goodsMapper;

    /** 把 DB 中所有 SPU 灌入 ES（启动时调一次；真实环境用 Canal） */
    public int reindexAll() {
        List<GoodsEntity> all = goodsMapper.selectList(null);
        List<GoodsSearchDoc> docs = all.stream().map(this::toDoc).toList();
        if (!docs.isEmpty()) {
            repository.saveAll(docs);
        }
        log.info("Reindex completed: {} goods → ES", docs.size());
        return docs.size();
    }

    public void index(GoodsEntity entity) {
        repository.save(toDoc(entity));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** 全文检索（match name + subtitle，按 sortOrder + 价格）+ 分页 */
    public PageVO<GoodsSearchDoc> search(String keyword, int page, int size) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> {
                    if (StringUtils.hasText(keyword)) {
                        return q.multiMatch(m -> m.fields("name^3", "subtitle")
                                .query(keyword)
                                .fuzziness("AUTO"));
                    }
                    return q.matchAll(ma -> ma);
                })
                .withPageable(PageRequest.of(Math.max(0, page - 1), size))
                .build();
        SearchHits<GoodsSearchDoc> hits = es.search(query, GoodsSearchDoc.class);

        PageVO<GoodsSearchDoc> vo = new PageVO<>();
        vo.setRecords(hits.getSearchHits().stream().map(SearchHit::getContent).toList());
        vo.setTotal(hits.getTotalHits());
        vo.setPage(page);
        vo.setSize(size);
        vo.setPages((vo.getTotal() + size - 1) / size);
        return vo;
    }

    private GoodsSearchDoc toDoc(GoodsEntity g) {
        GoodsSearchDoc d = new GoodsSearchDoc();
        d.setId(g.getId());
        d.setGoodsNo(g.getGoodsNo());
        d.setName(g.getName());
        d.setSubtitle(g.getSubtitle());
        d.setCategoryId(g.getCategoryId());
        d.setBrandId(g.getBrandId());
        d.setCoverImage(g.getCoverImage());
        d.setMinPrice(g.getMinPrice());
        d.setMaxPrice(g.getMaxPrice());
        d.setStatus(g.getStatus() == null ? 0 : g.getStatus().getCode());
        d.setSortOrder(g.getSortOrder());
        d.setUpdatedAt(g.getUpdatedAt());
        return d;
    }
}
