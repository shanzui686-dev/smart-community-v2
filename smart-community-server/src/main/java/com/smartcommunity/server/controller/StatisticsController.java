package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.OperationLogService;
import com.smartcommunity.server.vo.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计", description = "系统数据统计")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final OperationLogService operationLogService;

    public StatisticsController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "获取首页统计数据")
    @RequirePermission("statistics:dashboard")
    @GetMapping("/dashboard")
    public Result<StatisticsVO> getDashboard() {
        return Result.success(operationLogService.getStatistics());
    }
}
