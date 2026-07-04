package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.PageResult;
import com.smartcommunity.server.common.PageQuery;
import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.security.RequirePermission;
import com.smartcommunity.server.service.InOutRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "出入记录管理", description = "居民出入记录查询")
@RestController
@RequestMapping("/api/access/record")
public class InOutRecordController {

    private final InOutRecordService recordService;

    public InOutRecordController(InOutRecordService recordService) {
        this.recordService = recordService;
    }

    @Operation(summary = "分页查询出入记录")
    @RequirePermission("access:record:list")
    @GetMapping("/list")
    public Result<PageResult<InOutRecord>> list(PageQuery pageQuery,
                                                 @RequestParam(required = false) String personName,
                                                 @RequestParam(required = false) Long communityId,
                                                 @RequestParam(required = false) Integer type,
                                                 @RequestParam(required = false) String startTime,
                                                 @RequestParam(required = false) String endTime) {
        return Result.success(recordService.pageList(pageQuery, personName, communityId, type, startTime, endTime));
    }
}
