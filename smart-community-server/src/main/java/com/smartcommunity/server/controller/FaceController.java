package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.entity.Person;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.mapper.PersonMapper;
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
    private final PersonMapper personMapper;

    public FaceController(FaceRecognitionService faceRecognitionService, InOutRecordMapper recordMapper,
                          PersonMapper personMapper) {
        this.faceRecognitionService = faceRecognitionService;
        this.recordMapper = recordMapper;
        this.personMapper = personMapper;
    }

    @Operation(summary = "人脸搜索（1:N），找到匹配的居民（同时校验数据库是否仍存有该人脸）")
    @PostMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam("imageUrl") String imageUrl) {
        Map<String, Object> result = faceRecognitionService.searchFace(imageUrl);
        if (result != null) {
            Object personIdObj = result.get("personId");
            if (personIdObj != null) {
                Long personId = Long.valueOf(personIdObj.toString());
                Person person = personMapper.selectById(personId);
                // 数据库中该居民人脸照片已被删除或未成功注册百度AI → 视为未匹配
                if (person == null
                    || person.getFaceUrl() == null || person.getFaceUrl().isBlank()
                    || person.getFaceId() == null || person.getFaceId().isBlank()) {
                    return Result.success(Map.of("matched", false, "message", "未找到匹配的居民"));
                }
            }
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
