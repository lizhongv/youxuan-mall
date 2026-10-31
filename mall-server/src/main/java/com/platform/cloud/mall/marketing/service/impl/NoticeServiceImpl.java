package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.common.util.HtmlSanitizer;
import com.platform.cloud.mall.goods.support.GoodsCacheNames;
import com.platform.cloud.mall.marketing.dto.NoticeQuery;
import com.platform.cloud.mall.marketing.dto.NoticeRequest;
import com.platform.cloud.mall.marketing.entity.Notice;
import com.platform.cloud.mall.marketing.mapper.NoticeMapper;
import com.platform.cloud.mall.marketing.service.NoticeService;
import com.platform.cloud.mall.marketing.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 商城公告。写操作使首页缓存失效。
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageVO<NoticeVO> page(NoticeQuery q) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<Notice>()
                .select(Notice.class, f -> !"content".equals(f.getColumn()))
                .like(StringUtils.hasText(q.getKeyword()), Notice::getTitle, q.getKeyword())
                .eq(q.getStatus() != null, Notice::getStatus, q.getStatus())
                .orderByAsc(Notice::getSort).orderByDesc(Notice::getId);
        return PageVO.of(noticeMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), NoticeServiceImpl::toVO);
    }

    @Override
    public NoticeVO get(Long id) {
        return toVO(mustGet(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public Long create(NoticeRequest req) {
        Notice n = new Notice();
        apply(n, req);
        noticeMapper.insert(n);
        return n.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void update(Long id, NoticeRequest req) {
        Notice n = mustGet(id);
        apply(n, req);
        if (noticeMapper.updateById(n) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void delete(Long id) {
        mustGet(id);
        noticeMapper.deleteById(id);
    }

    @Override
    public List<NoticeVO> listLatest(int limit) {
        List<NoticeVO> result = new ArrayList<>();
        noticeMapper.selectPage(new Page<>(1, limit, false), new LambdaQueryWrapper<Notice>()
                        .select(Notice::getId, Notice::getTitle, Notice::getSort, Notice::getStatus, Notice::getCreatedAt)
                        .eq(Notice::getStatus, 1)
                        .orderByAsc(Notice::getSort).orderByDesc(Notice::getId))
                .getRecords().forEach(n -> result.add(toVO(n)));
        return result;
    }

    @Override
    public NoticeVO getPublished(Long id) {
        Notice n = noticeMapper.selectById(id);
        if (n == null || n.getStatus() == null || n.getStatus() != 1) {
            throw new BusinessException("公告不存在");
        }
        return toVO(n);
    }

    private Notice mustGet(Long id) {
        Notice n = noticeMapper.selectById(id);
        if (n == null) {
            throw new BusinessException("公告不存在");
        }
        return n;
    }

    private static void apply(Notice n, NoticeRequest req) {
        n.setTitle(req.getTitle().trim());
        // 公告在商城端以 v-html 渲染，入库前做服务端 XSS 清洗
        n.setContent(req.getContent() == null ? "" : HtmlSanitizer.sanitize(req.getContent()));
        n.setSort(req.getSort() == null ? 0 : req.getSort());
        n.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    static NoticeVO toVO(Notice n) {
        NoticeVO vo = new NoticeVO();
        vo.setId(n.getId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setSort(n.getSort());
        vo.setStatus(n.getStatus());
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
