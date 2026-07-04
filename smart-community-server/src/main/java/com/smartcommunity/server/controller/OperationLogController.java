package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.OperationLog;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志", description = "系统操作日志查询")
@RestController
@RequestMapping("/api/system/log")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "分页查询操作日志")
    @RequirePermission("system:log:list")
    @GetMapping("/list")
    public Result<PageResult<OperationLog>> list(PageQuery pageQuery,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(operationLogService.pageList(pageQuery, keyword));
    }
}
