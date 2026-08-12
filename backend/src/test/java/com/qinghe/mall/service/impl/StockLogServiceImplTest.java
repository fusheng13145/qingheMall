package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.StockLogDAO;
import com.qinghe.mall.dataobject.StockLogDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * StockLogServiceImpl 分支覆盖补充（T3 续补）。
 * 目标：record() 的短路校验分支（productDetailId / changeType 为空直接返回）。
 */
@ExtendWith(MockitoExtension.class)
class StockLogServiceImplTest {

    @Mock
    private StockLogDAO stockLogDAO;

    @InjectMocks
    private StockLogServiceImpl service;

    @Test
    void record_normal_shouldInsert() {
        service.record("d1", "p1", "N20260811001", "DEDUCT", 5, 100, 95);
        verify(stockLogDAO).insert(any(StockLogDO.class));
    }

    @Test
    void record_blankProductDetailId_shouldSkip() {
        service.record("  ", "p1", "N20260811002", "DEDUCT", 5, 100, 95);
        verify(stockLogDAO, never()).insert(any());
    }

    @Test
    void record_blankChangeType_shouldSkip() {
        service.record("d1", "p1", "N20260811003", "  ", 5, 100, 95);
        verify(stockLogDAO, never()).insert(any());
    }

    @Test
    void record_nullProductDetailId_shouldSkip() {
        service.record(null, "p1", "N20260811004", "DEDUCT", 5, 100, 95);
        verify(stockLogDAO, never()).insert(any());
    }
}
