package com.smartcommunity.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baidu.ai")
public class FaceProperties {
    private String appId;
    private String apiKey;
    private String secretKey;
    /** 人脸库分组 ID */
    private String groupId = "smart_community";
}
