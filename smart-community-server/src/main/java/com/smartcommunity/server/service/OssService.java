package com.smartcommunity.server.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.smartcommunity.server.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
public class OssService {

    private static final int SIGNED_URL_EXPIRE_SECONDS = 3600; // 签名 URL 默认1小时有效

    private final OssProperties ossProperties;

    public OssService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文件到阿里云OSS（使用默认 bucket）
     */
    public String upload(MultipartFile file, String dir) throws Exception {
        return upload(file, dir, "default");
    }

    /**
     * 上传文件到阿里云OSS（文件保持私有，通过签名 URL 访问）
     */
    public String upload(MultipartFile file, String dir, String bucketKey) throws Exception {
        OssProperties.BucketConfig config = ossProperties.getBucket(bucketKey);
        if (config == null) {
            throw new IllegalArgumentException("未找到 bucket 配置: " + bucketKey);
        }

        String bucketName = config.getBucketName();
        String endpoint = config.getEndpoint() != null ? config.getEndpoint() : ossProperties.getEndpoint();
        String ak = config.getAccessKeyId() != null ? config.getAccessKeyId() : ossProperties.getAccessKeyId();
        String sk = config.getAccessKeySecret() != null ? config.getAccessKeySecret() : ossProperties.getAccessKeySecret();

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = dir + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        OSS ossClient = new OSSClientBuilder().build(endpoint, ak, sk);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, fileName, inputStream);
            ossClient.putObject(putObjectRequest);
        } finally {
            ossClient.shutdown();
        }

        return "https://" + bucketName + "." + endpoint + "/" + fileName;
    }

    /**
     * 生成签名 URL（用于私有文件临时访问）
     * @param objectUrl  OSS 文件完整 URL（如 https://bucket.endpoint/dir/file.jpg）
     * @param expireSeconds 过期秒数（默认 3600 = 1小时）
     * @return 带签名的临时访问 URL
     */
    public String generateSignedUrl(String objectUrl, Integer expireSeconds) {
        if (objectUrl == null || objectUrl.isBlank()) return null;

        // 从完整 URL 中解析出 bucketName、endpoint 和 fileName
        // URL 格式: https://bucketName.endpoint/fileName
        String url = objectUrl.replace("https://", "").replace("http://", "");
        int firstDot = url.indexOf('.');
        if (firstDot <= 0) return objectUrl;

        String bucketName = url.substring(0, firstDot);
        String remaining = url.substring(firstDot + 1);
        int firstSlash = remaining.indexOf('/');
        String endpoint;
        String fileName;
        if (firstSlash > 0) {
            endpoint = remaining.substring(0, firstSlash);
            fileName = remaining.substring(firstSlash + 1);
        } else {
            endpoint = remaining;
            fileName = "";
        }

        // 根据 bucketName 查找对应配置的凭证
        OssProperties.BucketConfig config = findConfigByBucketName(bucketName);
        String ak = config != null && config.getAccessKeyId() != null ? config.getAccessKeyId() : ossProperties.getAccessKeyId();
        String sk = config != null && config.getAccessKeySecret() != null ? config.getAccessKeySecret() : ossProperties.getAccessKeySecret();

        OSS ossClient = new OSSClientBuilder().build(endpoint, ak, sk);
        try {
            int expire = expireSeconds != null ? expireSeconds : SIGNED_URL_EXPIRE_SECONDS;
            Date expiration = new Date(System.currentTimeMillis() + expire * 1000L);
            URL signedUrl = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
            return signedUrl.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    private OssProperties.BucketConfig findConfigByBucketName(String bucketName) {
        for (OssProperties.BucketConfig config : ossProperties.getBuckets().values()) {
            if (bucketName.equals(config.getBucketName())) {
                return config;
            }
        }
        return null;
    }
}
