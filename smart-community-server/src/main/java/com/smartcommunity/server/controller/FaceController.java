package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.service.FaceRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "人脸识别", description = "百度AI人脸搜索、比对")
@RestController
@RequestMapping("/api/face")
public class FaceController {

    private final FaceRecognitionService faceRecognitionService;
    private final InOutRecordMapper recordMapper;

    public FaceController(FaceRecognitionService faceRecognitionService, InOutRecordMapper recordMapper) {
        this.faceRecognitionService = faceRecognitionService;
        this.recordMapper = recordMapper;
    }

    @Operation(summary = "人脸搜索（1:N），找到匹配的居民")
    @PostMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam("imageUrl") String imageUrl) {
        Map<String, Object> result = faceRecognitionService.searchFace(imageUrl);
        if (result != null) {
            return Result.success(Map.of(
                    "personId", result.get("personId"),
                    "score", result.get("score"),
                    "matched", true
            ));
        }
        return Result.success(Map.of("matched", false, "message", "未找到匹配的居民"));
    }

    @Operation(summary = "人脸识别后记录出入（1=进入, 2=外出）")
    @PostMapping("/record")
    public Result<Void> record(@RequestParam String personName,
                                @RequestParam Long communityId,
                                @RequestParam String houseNo,
                                @RequestParam Long cameraId,
                                @RequestParam String photoUrl,
                                @RequestParam Integer direction) {
        InOutRecord record = new InOutRecord();
        record.setPersonName(personName);
        record.setCommunityId(communityId);
        record.setType(direction);
        record.setTime(LocalDateTime.now());
        record.setLocation(houseNo);
        record.setPhotoUrl(photoUrl);
        record.setCameraId(cameraId);
        record.setVerifyType(1); // 1=人脸识别
        record.setVerified(1);
        recordMapper.insert(record);
        return Result.success();
    }

    @Operation(summary = "1:1 人脸比对")
    @PostMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestParam("imageUrl1") String imageUrl1,
                                               @RequestParam("imageUrl2") String imageUrl2) {
        Float score = faceRecognitionService.compareFace(imageUrl1, imageUrl2);
        if (score != null) {
            boolean passed = score >= 80;
            return Result.success(Map.of("score", score, "passed", passed));
        }
        return Result.error("人脸比对失败");
    }
}
