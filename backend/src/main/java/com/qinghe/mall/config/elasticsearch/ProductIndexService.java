package com.qinghe.mall.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 商品 Elasticsearch 索引服务（阶段二 B-② 全文检索）。
 *
 * 职责：索引管理（建/删/存在性）、文档写（单条/批量）、搜索（布尔查询 + 中文 standard 分词）。
 * 设计原则：
 *  - 搜索失败时由调用方（ProductServiceImpl）降级 MySQL；
 *  - 写操作（index/delete/bulkIndex）内部吞异常，故障不影响主流程（与缓存 evict 同构）。
 */
@Service
public class ProductIndexService {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexService.class);
    public static final String INDEX_NAME = "products";

    @Autowired
    private ElasticsearchClient client;

    // ===================== 索引管理 =====================

    public boolean indexExists() throws IOException {
        return client.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
    }

    public void ensureIndex() {
        try {
            if (!indexExists()) {
                createIndex();
                log.info("[ES] 索引 {} 不存在，已创建", INDEX_NAME);
            }
        } catch (IOException e) {
            log.warn("[ES] ensureIndex 检查失败（将走 MySQL 降级）: {}", e.getMessage());
        }
    }

    public void createIndex() throws IOException {
        CreateIndexRequest req = CreateIndexRequest.of(c -> c
            .index(INDEX_NAME)
            .mappings(m -> m
                .properties("id", p -> p.keyword(k -> k))
                .properties("name", p -> p.text(t -> t.analyzer("standard")
                    .fields("raw", f -> f.keyword(k -> k))))
                .properties("brand", p -> p.keyword(k -> k))
                .properties("merchantId", p -> p.long_(l -> l))
                .properties("status", p -> p.keyword(k -> k))
                .properties("price", p -> p.double_(d -> d))
                .properties("purchaseNum", p -> p.integer(i -> i))
                .properties("productIntro", p -> p.text(t -> t.analyzer("standard")))
                .properties("gmtModified", p -> p.date(d -> d))
            )
        );
        client.indices().create(req);
    }

    public void deleteIndex() throws IOException {
        if (indexExists()) {
            client.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
        }
    }

    // ===================== 文档写 =====================

    public void index(Product product) {
        try {
            ProductDocument doc = toDocument(product);
            IndexRequest<ProductDocument> req = IndexRequest.of(i -> i.index(INDEX_NAME).id(doc.getId()).document(doc));
            client.index(req);
        } catch (Exception e) {
            log.warn("[ES] 索引商品 {} 失败（不影响主流程）: {}", product.getId(), e.getMessage());
        }
    }

    public void delete(String id) {
        try {
            DeleteRequest req = DeleteRequest.of(d -> d.index(INDEX_NAME).id(id));
            client.delete(req);
        } catch (Exception e) {
            log.warn("[ES] 删除索引商品 {} 失败（不影响主流程）: {}", id, e.getMessage());
        }
    }

    public void bulkIndex(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            for (Product p : products) {
                ProductDocument doc = toDocument(p);
                bulk.operations(op -> op.index(idx -> idx.index(INDEX_NAME).id(doc.getId()).document(doc)));
            }
            client.bulk(bulk.build());
        } catch (Exception e) {
            log.warn("[ES] 批量索引失败（{} 条，不影响主流程）: {}", products.size(), e.getMessage());
        }
    }

    // ===================== 搜索 =====================

    public Paging<Product> search(String keyword, String brand, String status, Long merchantId,
                                  String sort, int page, int size) throws IOException {
        int from = Math.max(0, (page - 1) * size);

        Query query = Query.of(q -> q.bool(b -> {
            if (StringUtils.isNotBlank(keyword)) {
                b.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields(List.of("name", "productIntro"))
                    .type(TextQueryType.BestFields)
                    .operator(Operator.Or)));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }
            if (StringUtils.isNotBlank(brand)) {
                b.filter(f -> f.term(t -> t.field("brand").value(FieldValue.of(brand))));
            }
            if (StringUtils.isNotBlank(status)) {
                b.filter(f -> f.term(t -> t.field("status").value(FieldValue.of(status))));
            }
            if (merchantId != null) {
                b.filter(f -> f.term(t -> t.field("merchantId").value(FieldValue.of(merchantId))));
            }
            return b;
        }));

        List<SortOptions> sorts = buildSort(sort);

        SearchRequest req = SearchRequest.of(s -> s
            .index(INDEX_NAME)
            .query(query)
            .from(from)
            .size(size)
            .sort(sorts)
            .trackTotalHits(t -> t.enabled(true)));
        SearchResponse<ProductDocument> resp = client.search(req, ProductDocument.class);

        long total = 0;
        if (resp.hits().total() != null) {
            TotalHits totalHits = resp.hits().total();
            total = totalHits.value();
        }
        List<Product> data = new ArrayList<>();
        for (Hit<ProductDocument> hit : resp.hits().hits()) {
            if (hit.source() != null) {
                data.add(toModel(hit.source()));
            }
        }
        Paging<Product> paging = new Paging<>();
        paging.setPageNum(page);
        paging.setPageSize(size);
        paging.setTotalCount(total);
        paging.setTotalPage(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        paging.setData(data);
        return paging;
    }

    private List<SortOptions> buildSort(String sort) {
        List<SortOptions> sorts = new ArrayList<>();
        if ("price_asc".equals(sort)) {
            sorts.add(SortOptions.of(o -> o.field(f -> f.field("price").order(SortOrder.Asc))));
        } else if ("price_desc".equals(sort)) {
            sorts.add(SortOptions.of(o -> o.field(f -> f.field("price").order(SortOrder.Desc))));
        } else if ("sales_desc".equals(sort)) {
            sorts.add(SortOptions.of(o -> o.field(f -> f.field("purchaseNum").order(SortOrder.Desc))));
        } else {
            sorts.add(SortOptions.of(o -> o.field(f -> f.field("gmtModified").order(SortOrder.Desc))));
        }
        return sorts;
    }

    // ===================== 映射 =====================

    private ProductDocument toDocument(Product p) {
        ProductDocument d = new ProductDocument();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setBrand(p.getBrand());
        d.setMerchantId(p.getMerchantId());
        d.setStatus(p.getStatus());
        d.setPrice(p.getPrice());
        d.setPurchaseNum(p.getPurchaseNum());
        d.setProductIntro(p.getProductIntro());
        d.setGmtModified(p.getGmtModified());
        return d;
    }

    private Product toModel(ProductDocument d) {
        Product p = new Product();
        p.setId(d.getId());
        p.setName(d.getName());
        p.setBrand(d.getBrand());
        p.setMerchantId(d.getMerchantId());
        p.setStatus(d.getStatus());
        p.setPrice(d.getPrice());
        p.setPurchaseNum(d.getPurchaseNum());
        p.setProductIntro(d.getProductIntro());
        p.setGmtModified(d.getGmtModified());
        return p;
    }
}
