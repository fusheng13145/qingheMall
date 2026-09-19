package com.qinghe.mall.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 契约快照测试（C1，v1.4 §9.3）——防止对外接口漂移。
 *
 * 扫描全部 @RestController 的 HTTP 端点（方法 + 路径），与
 * src/test/resources/api-contract-snapshot.json 快照逐条比对：
 * 新增 / 删除 / 改路径 / 改方法都会使测试失败，迫使变更方显式重建快照
 * （评审时对端点 diff 一目了然）。
 *
 * 重建快照：mvn test -Dtest=ApiContractSnapshotTest -Dcontract.update=true
 */
class ApiContractSnapshotTest {

    private static final String SCAN_PACKAGE = "com.qinghe.mall.controller";
    private static final Path SNAPSHOT_SRC = Paths.get("src/test/resources/api-contract-snapshot.json");
    private static final boolean UPDATE = Boolean.getBoolean("contract.update");

    @Test
    void apiEndpoints_matchContractSnapshot() throws Exception {
        List<String> actual = scanEndpoints();
        assertTrue(!actual.isEmpty(), "未扫描到任何端点，扫描逻辑或包路径失效");

        if (UPDATE) {
            writeSnapshot(actual);
            System.out.println("[contract] snapshot updated: " + actual.size() + " endpoints");
            return;
        }

        List<String> expected = readSnapshot();
        List<String> added = new ArrayList<>(actual);
        added.removeAll(expected);
        List<String> removed = new ArrayList<>(expected);
        removed.removeAll(actual);

        if (!added.isEmpty() || !removed.isEmpty()) {
            assertEquals(expected, actual, "API 契约漂移！新增端点=" + added
                    + "，移除端点=" + removed
                    + "。若变更是有意的，请执行：mvn test -Dtest=ApiContractSnapshotTest -Dcontract.update=true 重建快照并在评审中说明。");
        }
        assertEquals(expected.size(), actual.size());
    }

    /** 扫描全部 @RestController 的端点，返回排序后的 "METHOD path" 列表 */
    private List<String> scanEndpoints() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(SCAN_PACKAGE);

        List<String> endpoints = new ArrayList<>();
        for (BeanDefinition bd : candidates) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            String prefix = "";
            RequestMapping base = clazz.getAnnotation(RequestMapping.class);
            if (base != null && base.value().length > 0) {
                prefix = normalize(base.value()[0]);
            }
            for (Method m : clazz.getDeclaredMethods()) {
                collect(endpoints, prefix, GetMapping.class, m, "GET");
                collect(endpoints, prefix, PostMapping.class, m, "POST");
                collect(endpoints, prefix, PutMapping.class, m, "PUT");
                collect(endpoints, prefix, DeleteMapping.class, m, "DELETE");
                collect(endpoints, prefix, PatchMapping.class, m, "PATCH");
                RequestMapping rm = m.getAnnotation(RequestMapping.class);
                if (rm != null) {
                    if (rm.method().length == 0) {
                        for (String p : rm.path()) {
                            endpoints.add("ANY " + prefix + normalize(p));
                        }
                    } else {
                        for (RequestMethod method : rm.method()) {
                            for (String p : rm.path()) {
                                endpoints.add(method.name() + " " + prefix + normalize(p));
                            }
                        }
                    }
                }
            }
        }
        endpoints.sort(Comparator.naturalOrder());
        return endpoints;
    }

    private void collect(List<String> endpoints, String prefix, Class<? extends java.lang.annotation.Annotation> type,
                         Method m, String method) {
        java.lang.annotation.Annotation a = m.getAnnotation(type);
        if (a == null) {
            return;
        }
        try {
            String[] paths = (String[]) type.getMethod("value").invoke(a);
            if (paths.length == 0) {
                endpoints.add(method + " " + prefix);
            } else {
                for (String p : paths) {
                    endpoints.add(method + " " + prefix + normalize(p));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取映射注解失败: " + type, e);
        }
    }

    private String normalize(String p) {
        if (p == null || p.isEmpty()) {
            return "";
        }
        return p.startsWith("/") ? p : "/" + p;
    }

    private List<String> readSnapshot() throws Exception {
        Path inClasspath = Paths.get(getClass().getResource("/api-contract-snapshot.json").toURI());
        String json = Files.readString(inClasspath);
        Snapshot snapshot = new ObjectMapper().readValue(json, Snapshot.class);
        return snapshot.endpoints;
    }

    private void writeSnapshot(List<String> endpoints) throws Exception {
        File parent = SNAPSHOT_SRC.toFile().getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("快照目录创建失败: " + parent.getAbsolutePath());
        }
        Snapshot snapshot = new Snapshot();
        snapshot.endpoints = endpoints;
        new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(SNAPSHOT_SRC.toFile(), snapshot);
    }

    /** 快照文件结构（Jackson 绑定） */
    public static class Snapshot {
        public String comment = "由 ApiContractSnapshotTest 生成；API 有意变更后执行 "
                + "mvn test -Dtest=ApiContractSnapshotTest -Dcontract.update=true 重建";
        public List<String> endpoints = new ArrayList<>();
    }
}
