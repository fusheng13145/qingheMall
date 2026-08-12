package com.qinghe.mall.config.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ProductIndexService 单元测试（Mock ElasticsearchClient，不依赖真实 ES）。
 * 覆盖：index 以商品 id 为 _id、search 布尔查询构建与结果映射、ensureIndex 存在性分支、delete。
 */
class ProductIndexServiceTest {

    private ElasticsearchClient client;
    private ProductIndexService service;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        service = new ProductIndexService();
        ReflectionTestUtils.setField(service, "client", client);
    }

    private Product sample() {
        Product p = new Product();
        p.setId("p1");
        p.setName("测试手机");
        p.setBrand("Nike");
        p.setStatus("ON");
        p.setPrice(new BigDecimal("299"));
        p.setPurchaseNum(5);
        return p;
    }

    @Test
    @DisplayName("index 写入时以商品 id 作为 _id 且文档字段正确")
    void indexUsesProductId() throws Exception {
        when(client.index(any(IndexRequest.class))).thenReturn(mock(IndexResponse.class));

        service.index(sample());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IndexRequest<ProductDocument>> captor =
                (ArgumentCaptor<IndexRequest<ProductDocument>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(IndexRequest.class);
        verify(client).index(captor.capture());
        IndexRequest<ProductDocument> req = captor.getValue();
        assertThat(req.index()).contains("products");
        assertThat(req.id()).isEqualTo("p1");
        assertThat(req.document().getName()).isEqualTo("测试手机");
    }

    @Test
    @DisplayName("search 构建布尔查询：关键词 multiMatch + 状态 term 过滤 + 分页/排序")
    void searchBuildsBoolQuery() throws Exception {
        SearchResponse<ProductDocument> resp = mock(SearchResponse.class);
        HitsMetadata<ProductDocument> hitsMeta = mock(HitsMetadata.class);
        when(resp.hits()).thenReturn(hitsMeta);
        when(hitsMeta.total()).thenReturn(null);
        when(hitsMeta.hits()).thenReturn(List.of());
        when(client.search(any(SearchRequest.class), eq(ProductDocument.class))).thenReturn(resp);

        service.search("手机", null, "ON", null, "price_asc", 2, 15);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<SearchRequest> captor = (ArgumentCaptor<SearchRequest>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(captor.capture(), eq(ProductDocument.class));
        SearchRequest req = captor.getValue();
        assertThat(req.index()).contains("products");
        assertThat(req.from()).isEqualTo(15); // (2-1)*15
        assertThat(req.size()).isEqualTo(15);
        assertThat(req.sort()).isNotEmpty();
        assertThat(req.sort().get(0).isField()).isTrue();
        assertThat(req.sort().get(0).field().field()).isEqualTo("price");

        assertThat(req.query().isBool()).isTrue();
        // 关键词 → must 为 multiMatch；状态 → filter 含 term
        assertThat(req.query().bool().must().get(0).isMultiMatch()).isTrue();
        assertThat(req.query().bool().filter()).isNotEmpty();
        assertThat(req.query().bool().filter().get(0).isTerm()).isTrue();
    }

    @Test
    @DisplayName("search 结果映射为 Paging（含 total 与文档转换）")
    void searchMapsToPaging() throws Exception {
        SearchResponse<ProductDocument> resp = mock(SearchResponse.class);
        HitsMetadata<ProductDocument> hitsMeta = mock(HitsMetadata.class);
        Hit<ProductDocument> hit = mock(Hit.class);
        ProductDocument doc = new ProductDocument();
        doc.setId("p1");
        doc.setName("映射商品");
        doc.setBrand("Nike");
        doc.setStatus("ON");
        doc.setPrice(new BigDecimal("99"));
        doc.setPurchaseNum(3);
        when(hit.source()).thenReturn(doc);
        when(hitsMeta.hits()).thenReturn(List.of(hit));
        TotalHits total = mock(TotalHits.class);
        when(total.value()).thenReturn(1L);
        when(hitsMeta.total()).thenReturn(total);
        when(resp.hits()).thenReturn(hitsMeta);
        when(client.search(any(SearchRequest.class), eq(ProductDocument.class))).thenReturn(resp);

        Paging<Product> paging = service.search(null, null, "ON", null, null, 1, 10);
        assertThat(paging.getTotalCount()).isEqualTo(1);
        assertThat(paging.getData()).hasSize(1);
        assertThat(paging.getData().get(0).getName()).isEqualTo("映射商品");
    }

    @Test
    @DisplayName("ensureIndex 索引缺失时创建")
    void ensureIndexCreatesWhenAbsent() throws Exception {
        ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);
        BooleanResponse existsResp = mock(BooleanResponse.class);
        when(existsResp.value()).thenReturn(false);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(existsResp);
        when(indices.create(any(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.class)))
                .thenReturn(mock(co.elastic.clients.elasticsearch.indices.CreateIndexResponse.class));

        service.ensureIndex();

        verify(indices).create(any(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.class));
    }

    @Test
    @DisplayName("ensureIndex 索引已存在时不重复创建")
    void ensureIndexSkipsWhenPresent() throws Exception {
        ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);
        BooleanResponse existsResp = mock(BooleanResponse.class);
        when(existsResp.value()).thenReturn(true);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(existsResp);

        service.ensureIndex();

        verify(indices, never()).create(any(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.class));
    }

    @Test
    @DisplayName("delete 调用 ES 删除并传入 id")
    void deleteCallsEs() throws Exception {
        when(client.delete(any(co.elastic.clients.elasticsearch.core.DeleteRequest.class)))
                .thenReturn(mock(co.elastic.clients.elasticsearch.core.DeleteResponse.class));

        service.delete("p1");

        verify(client).delete(any(co.elastic.clients.elasticsearch.core.DeleteRequest.class));
    }
}
