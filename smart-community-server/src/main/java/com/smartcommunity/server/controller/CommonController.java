package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "通用接口", description = "文件上传等公共接口")
@RestController
@RequestMapping("/api/common")
public class CommonController {

    private final OssService ossService;

    public CommonController(OssService ossService) {
        this.ossService = ossService;
    }

    @Operation(summary = "获取OSS签名URL（用于私有文件临时访问）")
    @GetMapping("/signed-url")
    public Result<String> getSignedUrl(@RequestParam String url,
                                       @RequestParam(required = false) Integer expire) {
        try {
            String signedUrl = ossService.generateSignedUrl(url, expire);
            return Result.success(signedUrl);
        } catch (Exception e) {
            return Result.error("生成签名URL失败: " + e.getMessage());
        }
    }

    @Operation(summary = "上传文件到OSS")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "common") String dir,
                                 @RequestParam(defaultValue = "default") String bucket) {
        try {
            String url = ossService.upload(file, dir, bucket);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
