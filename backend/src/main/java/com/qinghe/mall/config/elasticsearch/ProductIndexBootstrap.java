package com.qinghe.mall.config.elasticsearch;

import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.ProductDO;
import com.qinghe.mall.model.Product;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 应用启动时商品索引初始化（阶段二 B-②）。
 *
 *  - dev 环境：删除旧索引并全量重建导入（本地验收，保证索引与 DB 一致）。
 *  - 非 dev：仅确保索引存在（不删数据，依赖写链路双写为准实时）。
 *
 * 任何异常仅告警，应用启动不依赖 ES 可用性（ES 不可用时自动走 MySQL 降级）。
 */
@Component
public class ProductIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexBootstrap.class);

    @Autowired
    private ProductIndexService indexService;
    @Autowired
    private ProductDAO productDAO;
    @Autowired
    private org.springframework.core.env.Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                log.info("[ES] dev 环境：重建商品索引并全量导入");
                if (indexService.indexExists()) {
                    indexService.deleteIndex();
                }
                indexService.createIndex();
                List<ProductDO> all = productDAO.queryAll(null, null, null, null);
                List<Product> products = new ArrayList<>();
                for (ProductDO d : all) {
                    products.add(d.convertToModel());
                }
                indexService.bulkIndex(products);
                log.info("[ES] 全量导入 {} 条商品至索引 {}", products.size(), ProductIndexService.INDEX_NAME);
            } else {
                log.info("[ES] 非 dev 环境：确保商品索引存在");
                indexService.ensureIndex();
            }
        } catch (Exception e) {
            log.warn("[ES] 启动建索引失败（将走 MySQL 降级）: {}", e.getMessage());
        }
    }
}
