package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.LogisticsDAO;
import com.qinghe.mall.dao.LogisticsTraceDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.LogisticsDO;
import com.qinghe.mall.dataobject.LogisticsTraceDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Logistics;
import com.qinghe.mall.model.LogisticsTrace;
import com.qinghe.mall.service.LogisticsService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 物流服务实现（P2-18）。
 *
 * 发货 = 订单状态 CAS + 物流建档 + 首条轨迹，经 TransactionTemplate 同事务提交：
 * 任一步失败整体回滚，杜绝「订单已发货但无物流档案」的中间态。
 * （orderService.shipOrder 内部同样使用 TransactionTemplate，默认 PROPAGATION_REQUIRED
 * 加入外层事务，不会提前提交。）
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    /** 轨迹文案：推进到各状态时的标准描述 */
    private static final String TRACE_IN_TRANSIT = "包裹运输中，正发往收货地址";
    private static final String TRACE_DELIVERING = "包裹已到达派送站点，快递员正在派送";
    private static final String TRACE_SIGNED = "包裹已签收，感谢您的使用";

    @Autowired
    private LogisticsDAO logisticsDAO;

    @Autowired
    private LogisticsTraceDAO logisticsTraceDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public Logistics ship(String orderNumber, String company, String trackingNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StringUtils.isBlank(company) || StringUtils.isBlank(trackingNumber)) {
            throw new BusinessException("请填写承运商与运单号");
        }
        String companyTrimmed = company.trim();
        String trackingTrimmed = trackingNumber.trim();
        if (companyTrimmed.length() > 64 || trackingTrimmed.length() > 64) {
            throw new BusinessException("承运商或运单号过长（最多 64 字符）");
        }
        if (orderDAO.findByOrderNumber(orderNumber) == null) {
            throw new BusinessException("订单不存在");
        }
        // 重复发货守卫：一单一物流档案（uk_order_number 兜底）
        if (logisticsDAO.findByOrderNumber(orderNumber) != null) {
            throw new BusinessException("该订单已录入物流信息，请勿重复发货");
        }

        transactionTemplate.execute(status -> {
            // 订单 CAS：仅已付款 → 已发货（状态异常/并发竞争失败时抛业务异常）
            orderService.shipOrder(orderNumber);

            Date now = new Date();
            LogisticsDO logisticsDO = new LogisticsDO();
            logisticsDO.setId(UUIDUtils.uuid());
            logisticsDO.setOrderNumber(orderNumber);
            logisticsDO.setCompany(companyTrimmed);
            logisticsDO.setTrackingNumber(trackingTrimmed);
            logisticsDO.setStatus(LogisticsDO.STATUS_SHIPPED);
            logisticsDO.setGmtCreated(now);
            logisticsDO.setGmtModified(now);
            logisticsDAO.insert(logisticsDO);

            insertTrace(orderNumber,
                    "商家已发货，承运商【" + companyTrimmed + "】，运单号 " + trackingTrimmed, now);
            return true;
        });
        return track(orderNumber);
    }

    @Override
    public Logistics advance(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        LogisticsDO logisticsDO = logisticsDAO.findByOrderNumber(orderNumber);
        if (logisticsDO == null) {
            throw new BusinessException("该订单暂无物流信息");
        }
        String current = logisticsDO.getStatus();
        String next = nextStatus(current);
        if (next == null) {
            throw new BusinessException("物流已签收，无需继续推进");
        }
        String traceText = traceTextFor(next);

        transactionTemplate.execute(status -> {
            // CAS 推进：仅当前状态仍为 current 时生效，防并发重复推进
            int updated = logisticsDAO.updateStatusWithGuard(orderNumber, current, next);
            if (updated <= 0) {
                throw new BusinessException("物流状态已变化，请刷新后重试");
            }
            insertTrace(orderNumber, traceText, new Date());
            return true;
        });
        return track(orderNumber);
    }

    @Override
    public Logistics track(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        LogisticsDO logisticsDO = logisticsDAO.findByOrderNumber(orderNumber);
        if (logisticsDO == null) {
            return null;
        }
        Logistics logistics = new Logistics();
        logistics.setOrderNumber(logisticsDO.getOrderNumber());
        logistics.setCompany(logisticsDO.getCompany());
        logistics.setTrackingNumber(logisticsDO.getTrackingNumber());
        logistics.setStatus(logisticsDO.getStatus());
        logistics.setGmtCreated(logisticsDO.getGmtCreated());
        List<LogisticsTrace> traces = new ArrayList<>();
        for (LogisticsTraceDO traceDO : logisticsTraceDAO.findByOrderNumber(orderNumber)) {
            traces.add(new LogisticsTrace(traceDO.getDescription(), traceDO.getTraceTime()));
        }
        logistics.setTraces(traces);
        return logistics;
    }

    /** 状态管道的下一站；终态 SIGNED 返回 null */
    private String nextStatus(String current) {
        if (LogisticsDO.STATUS_SHIPPED.equals(current)) {
            return LogisticsDO.STATUS_IN_TRANSIT;
        }
        if (LogisticsDO.STATUS_IN_TRANSIT.equals(current)) {
            return LogisticsDO.STATUS_DELIVERING;
        }
        if (LogisticsDO.STATUS_DELIVERING.equals(current)) {
            return LogisticsDO.STATUS_SIGNED;
        }
        return null;
    }

    private String traceTextFor(String targetStatus) {
        if (LogisticsDO.STATUS_IN_TRANSIT.equals(targetStatus)) {
            return TRACE_IN_TRANSIT;
        }
        if (LogisticsDO.STATUS_DELIVERING.equals(targetStatus)) {
            return TRACE_DELIVERING;
        }
        return TRACE_SIGNED;
    }

    private void insertTrace(String orderNumber, String description, Date traceTime) {
        LogisticsTraceDO traceDO = new LogisticsTraceDO();
        traceDO.setOrderNumber(orderNumber);
        traceDO.setDescription(description);
        traceDO.setTraceTime(traceTime);
        traceDO.setGmtCreated(traceTime);
        logisticsTraceDAO.insert(traceDO);
    }
}
