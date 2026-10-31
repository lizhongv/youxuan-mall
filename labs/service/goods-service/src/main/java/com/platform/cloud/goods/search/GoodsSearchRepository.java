package com.platform.cloud.goods.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsSearchRepository extends ElasticsearchRepository<GoodsSearchDoc, Long> {
}
