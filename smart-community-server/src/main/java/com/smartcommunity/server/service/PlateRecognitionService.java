package com.smartcommunity.server.service;

import com.baidu.aip.ocr.AipOcr;
import com.smartcommunity.server.config.PlateProperties;
import com.smartcommunity.server.entity.Vehicle;
import com.smartcommunity.server.mapper.VehicleMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PlateRecognitionService {

    private final AipOcr client;
    private final VehicleMapper vehicleMapper;
    private final OssService ossService;

    public PlateRecognitionService(PlateProperties plateProperties, VehicleMapper vehicleMapper, OssService ossService) {
        this.vehicleMapper = vehicleMapper;
        this.ossService = ossService;
        log.info("初始化百度OCR车牌识别服务: appId={}, apiKey={}", plateProperties.getAppId(), plateProperties.getApiKey());
        this.client = new AipOcr(plateProperties.getAppId(), plateProperties.getApiKey(), plateProperties.getSecretKey());
        client.setConnectionTimeoutInMillis(10000);
        client.setSocketTimeoutInMillis(20000);
        log.info("百度OCR车牌识别服务初始化完成");
    }

    public Map<String, Object> recognizePlate(String imageUrl) {
        log.info("开始车牌识别: imageUrl={}", imageUrl);
        
        try {
            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.error("图片下载失败或图片为空");
                return Map.of("success", false, "error", "图片下载失败");
            }
            log.info("图片下载成功，大小: {} bytes", imageBytes.length);

            HashMap<String, String> options = new HashMap<>();
            options.put("multi_detect", "false");

            JSONObject result = null;
            try {
                result = client.plateLicense(imageBytes, options);
            } catch (Exception e) {
                log.error("百度OCR API调用异常: {}", e.getMessage(), e);
                return Map.of("success", false, "error", "OCR API调用失败: " + e.getMessage());
            }

            if (result == null) {
                log.error("百度OCR返回结果为空");
                return Map.of("success", false, "error", "OCR返回结果为空");
            }

            log.info("百度OCR车牌识别响应: {}", result.toString());

            if (result.has("error_code")) {
                String errorCode = result.optString("error_code", "");
                String errorMsg = result.optString("error_msg", "未知错误");
                log.error("百度OCR车牌识别失败: error_code={}, error_msg={}", errorCode, errorMsg);
                return Map.of("success", false, "error", "OCR识别失败: " + errorMsg);
            }

            JSONObject wordsResult = result.optJSONObject("words_result");
            if (wordsResult != null) {
                String plateNumber = wordsResult.optString("number", "").toUpperCase();
                String color = wordsResult.optString("color", "");
                
                if (!plateNumber.isEmpty()) {
                    log.info("车牌识别成功: plateNumber={}, color={}", plateNumber, color);
                    
                    Vehicle vehicle = vehicleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Vehicle>()
                            .eq(Vehicle::getPlateNumber, plateNumber)
                            .eq(Vehicle::getDeleted, 0));

                    if (vehicle != null) {
                        return Map.of(
                            "success", true,
                            "plateNumber", plateNumber,
                            "color", color,
                            "matched", true,
                            "vehicleId", vehicle.getVehicleId(),
                            "personId", vehicle.getPersonId(),
                            "hasParkingSpace", vehicle.getHasParkingSpace()
                        );
                    } else {
                        return Map.of(
                            "success", true,
                            "plateNumber", plateNumber,
                            "color", color,
                            "matched", false
                        );
                    }
                } else {
                    log.info("未识别到车牌号");
                    return Map.of("success", false, "error", "未识别到车牌号");
                }
            } else {
                log.info("words_result为空");
                return Map.of("success", false, "error", "识别结果为空");
            }
        } catch (Exception e) {
            log.error("百度OCR车牌识别异常: error={}", e.getMessage(), e);
            return Map.of("success", false, "error", "系统异常: " + e.getMessage());
        }
    }

    private byte[] downloadImage(String imageUrl) {
        if (imageUrl == null) return null;
        
        try {
            String urlToUse = imageUrl;
            if (imageUrl.contains("aliyuncs.com") && !imageUrl.contains("smart-community-v1")) {
                urlToUse = ossService.generateSignedUrl(imageUrl, 300);
                log.info("使用签名URL下载图片: {}", urlToUse.substring(0, Math.min(80, urlToUse.length())));
            }

            HttpURLConnection conn = (HttpURLConnection) URI.create(urlToUse).toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/*");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.error("图片下载失败，HTTP状态码: {}", responseCode);
                return null;
            }

            String contentType = conn.getContentType();
            log.info("图片Content-Type: {}", contentType);

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                byte[] result = bos.toByteArray();
                log.info("图片下载成功，大小: {} bytes", result.length);
                return result;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.error("图片下载异常: {}", e.getMessage(), e);
            return null;
        }
    }
}