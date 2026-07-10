package com.smartcommunity.server.service;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;
import com.smartcommunity.server.config.FaceProperties;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FaceRecognitionService {

    private final AipFace client;
    private final String groupId;
    private final OssService ossService;

    public FaceRecognitionService(FaceProperties faceProperties, OssService ossService) {
        this.groupId = faceProperties.getGroupId();
        this.ossService = ossService;
        this.client = new AipFace(faceProperties.getAppId(), faceProperties.getApiKey(), faceProperties.getSecretKey());
        client.setConnectionTimeoutInMillis(5000);
        client.setSocketTimeoutInMillis(10000);
        log.info("百度AI人脸识别服务初始化完成, groupId: {}", groupId);
    }

    /**
     * 注册人脸到人脸库
     */
    public String registerFace(String imageUrl, String personId) {
        try {
            HashMap<String, String> options = new HashMap<>();
            options.put("quality_control", "LOW");
            options.put("liveness_control", "NONE");

            String image = resolveImage(imageUrl);
            if (image.isEmpty()) {
                throw new RuntimeException("无法下载人脸图片，请检查OSS配置或稍后重试");
            }
            String imageType = image.startsWith("http") ? "URL" : "BASE64";

            JSONObject result = client.addUser(image, imageType, groupId, personId, options);
            log.info("百度人脸注册响应: {}", result.toString());

            String errorCode = result.optString("error_code", "");
            if ("0".equals(errorCode) || "223105".equals(errorCode)) {
                JSONObject resData = result.optJSONObject("result");
                String faceToken = resData != null ? resData.optString("face_token", personId) : personId;
                log.info("百度人脸注册成功: personId={}, faceToken={}", personId, faceToken);
                return faceToken;
            }
            String errMsg = result.optString("error_msg", "未知错误");
            log.error("百度人脸注册失败: personId={}, error={}", personId, errMsg);
            throw new RuntimeException("人脸注册失败: " + errMsg);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度人脸注册异常: personId={}, error={}", personId, e.getMessage());
            throw new RuntimeException("人脸注册服务异常: " + e.getMessage());
        }
    }

    /**
     * 人脸搜索（1:N 比对）
     */
    public Map<String, Object> searchFace(String imageUrl) {
        try {
            HashMap<String, Object> options = new HashMap<>();
            options.put("quality_control", "LOW");
            options.put("liveness_control", "NONE");
            options.put("max_user_num", 1);

            String image = resolveImage(imageUrl);
            if (image.isEmpty()) {
                throw new RuntimeException("无法下载人脸图片，请检查OSS配置");
            }
            String imageType = image.startsWith("http") ? "URL" : "BASE64";

            JSONObject result = client.search(image, imageType, groupId, options);
            log.info("百度人脸搜索响应: {}", result.toString());

            String errorCode = result.optString("error_code", "");
            if ("0".equals(errorCode)) {
                JSONObject resData = result.optJSONObject("result");
                if (resData != null) {
                    JSONArray userList = resData.optJSONArray("user_list");
                    if (userList != null && userList.length() > 0) {
                        JSONObject topUser = userList.getJSONObject(0);
                        double score = topUser.optDouble("score", 0);
                        String userId = topUser.optString("user_id", "");
                        if (score > 80) {
                            log.info("百度人脸搜索成功: userId={}, score={}", userId, score);
                            return Map.of("personId", userId, "score", Math.round(score * 100.0) / 100.0);
                        }
                        log.info("百度人脸搜索低置信度: score={}", score);
                    }
                }
                // error_code=0 但没有匹配用户 → 正常未找到
                return null;
            } else if ("222207".equals(errorCode)) {
                log.info("百度人脸搜索未找到匹配");
                return null;
            } else {
                // 其他错误码：图片质量问题等，抛异常阻断
                String errMsg = result.optString("error_msg", "未知错误");
                log.error("百度人脸搜索失败: error={}", errMsg);
                throw new RuntimeException("人脸搜索失败: " + errMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度人脸搜索异常: error={}", e.getMessage());
            throw new RuntimeException("人脸搜索服务异常: " + e.getMessage());
        }
    }

    /**
     * 1:1 人脸比对
     */
    public Float compareFace(String imageUrl1, String imageUrl2) {
        try {
            String img1 = resolveImage(imageUrl1);
            String img2 = resolveImage(imageUrl2);
            String type1 = img1.startsWith("http") ? "URL" : "BASE64";
            String type2 = img2.startsWith("http") ? "URL" : "BASE64";

            MatchRequest req1 = new MatchRequest(img1, type1);
            MatchRequest req2 = new MatchRequest(img2, type2);
            List<MatchRequest> requests = new ArrayList<>();
            requests.add(req1);
            requests.add(req2);

            JSONObject result = client.match(requests);
            log.info("百度人脸比对响应: {}", result.toString());

            String errorCode = result.optString("error_code", "");
            if ("0".equals(errorCode)) {
                JSONObject resData = result.optJSONObject("result");
                if (resData != null) {
                    float score = (float) resData.optDouble("score", 0);
                    log.info("百度人脸比对完成: score={}", score);
                    return score;
                }
            }
            log.error("百度人脸比对失败: error={}", result.optString("error_msg"));
        } catch (Exception e) {
            log.error("百度人脸比对异常: error={}", e.getMessage());
        }
        return null;
    }

    /**
     * 删除人脸
     */
    public void deleteFace(String faceToken, String personId) {
        try {
            HashMap<String, String> options = new HashMap<>();
            JSONObject result = client.faceDelete(personId, groupId, faceToken, options);
            log.info("百度人脸删除: personId={}, result={}", personId, result.optString("error_msg"));
        } catch (Exception e) {
            log.error("百度人脸删除异常: personId={}, error={}", personId, e.getMessage());
        }
    }

    /**
     * 解析图片：如果是 OSS 私有 Bucket 的 URL，先下载转 base64；
     * 如果是公共可访问的 URL，直接返回原 URL
     */
    private String resolveImage(String imageUrl) {
        if (imageUrl == null) return "";
        // 公共读的 Bucket，百度可以直接下载
        if (imageUrl.contains("smart-community-v1")) return imageUrl;
        // 非 OSS URL（如百度图片），直接返回
        if (!imageUrl.contains("aliyuncs.com")) return imageUrl;

        // OSS 私有 Bucket → 生成签名 URL 后下载转 base64
        try {
            String signedUrl = ossService.generateSignedUrl(imageUrl, 300);
            log.info("下载 OSS 图片: {}", signedUrl.substring(0, Math.min(80, signedUrl.length())));

            HttpURLConnection conn = (HttpURLConnection) URI.create(signedUrl).toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.connect();

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
                log.info("OSS 图片下载成功, base64 长度: {}", base64.length());
                return base64;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.error("OSS 图片下载失败, 尝试直接用签名 URL: {}", e.getMessage());
            // 兜底：用签名 URL 直接传给百度（较短时效可能成功）
            return ossService.generateSignedUrl(imageUrl, 300);
        }
    }
}
