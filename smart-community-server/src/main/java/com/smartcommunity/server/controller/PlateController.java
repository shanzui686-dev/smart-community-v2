package com.smartcommunity.server.controller;

import com.smartcommunity.server.common.Result;
import com.smartcommunity.server.entity.InOutRecord;
import com.smartcommunity.server.mapper.InOutRecordMapper;
import com.smartcommunity.server.service.PlateRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/plate")
public class PlateController {

    private final PlateRecognitionService plateRecognitionService;
    private final InOutRecordMapper recordMapper;

    public PlateController(PlateRecognitionService plateRecognitionService, InOutRecordMapper recordMapper) {
        this.plateRecognitionService = plateRecognitionService;
        this.recordMapper = recordMapper;
    }

    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestParam String imageUrl) {
        log.info("车牌识别请求: imageUrl={}", imageUrl);
        Map<String, Object> result = plateRecognitionService.recognizePlate(imageUrl);
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        }
        return Result.error("车牌识别失败");
    }

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
        record.setVerifyType(4);
        record.setVerified(1);
        recordMapper.insert(record);
        return Result.success();
    }
}