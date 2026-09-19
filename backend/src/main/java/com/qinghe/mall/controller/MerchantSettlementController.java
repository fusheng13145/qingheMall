package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.SettlementBillDO;
import com.qinghe.mall.dataobject.SettlementLedgerDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.MerchantAuthService;
import com.qinghe.mall.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端结算（A2，v1.5）：结算概览 / 分账流水 / 结算单。
 *
 * 所有接口经 {@link MerchantAuthService#checkMerchant} 六重守卫，
 * 数据仅返回当前登录商家自己的流水与结算单。
 */
@RestController
@RequestMapping("/api/merchant/settlement")
public class MerchantSettlementController {

    @Autowired
    private MerchantAuthService merchantAuthService;

    @Autowired
    private SettlementService settlementService;

    /** 结算概览：余额 / 累计净入 / 累计佣金 / 已放款合计 */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(settlementService.merchantSummary(merchant.getId()));
    }

    /** 分账流水分页（仅本店） */
    @GetMapping("/ledger")
    public Result<Paging<SettlementLedgerDO>> ledger(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(settlementService.merchantLedger(merchant.getId(), pageNum, pageSize));
    }

    /** 结算单分页（仅本店） */
    @GetMapping("/bills")
    public Result<Paging<SettlementBillDO>> bills(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        MerchantDO merchant = merchantAuthService.checkMerchant(request);
        return Result.success(settlementService.merchantBills(merchant.getId(), pageNum, pageSize));
    }
}
