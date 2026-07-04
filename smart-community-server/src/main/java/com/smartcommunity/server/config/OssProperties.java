package com.smartcommunity.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {
    /** 全局默认 endpoint */
    private String endpoint;
    /** 全局默认 AccessKey */
    private String accessKeyId;
    /** 全局默认 SecretKey */
    private String accessKeySecret;
    /** 多 Bucket 配置，key 为标识，value 为 Bucket 详细配置 */
    private Map<String, BucketConfig> buckets = new HashMap<>();

    @Data
    public static class BucketConfig {
        /** Bucket 名称 */
        private String bucketName;
        /** 独立 endpoint（可选，不填则用全局） */
        private String endpoint;
        /** 独立 AccessKey（可选，不填则用全局） */
        private String accessKeyId;
        /** 独立 SecretKey（可选，不填则用全局） */
        private String accessKeySecret;
    }

    public BucketConfig getBucket(String key) {
        return buckets.getOrDefault(key, buckets.get("default"));
    }
}
