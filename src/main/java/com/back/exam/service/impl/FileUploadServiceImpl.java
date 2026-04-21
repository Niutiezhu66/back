package com.back.exam.service.impl;

import com.back.exam.service.FileUploadService;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public String uploadFile(String folder, MultipartFile file) {
        try {
            //判断minio的桶是否存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {// 如果不存在则创建
                String policy = """
                    {
                          "Statement" : [ {
                            "Action" : "s3:GetObject",
                            "Effect" : "Allow",
                            "Principal" : "*",
                            "Resource" : "arn:aws:s3:::%s/*"
                          } ],
                          "Version" : "2012-10-17"
                    }""".formatted(bucket);
                //创建桶
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
                //设置桶的权限
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucket)
                                .config(policy).build()
                );
            }


            String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String objectName = buildObjectName(folder, date, file.getOriginalFilename());
            // 获取文件流
            InputStream fileInputStream = file.getInputStream();
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket) //传到哪个桶
                            .object(objectName) //folder+"/" 会自己创建对应文件夹
                            .stream(fileInputStream, file.getSize(), -1) //输入流
                            .contentType(file.getContentType()) //文件类型
                            .build()
            );

            // 返回给前端浏览器可直接访问的 URL
            return buildPublicUrl(objectName);
        } catch (ServerException | XmlParserException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException | InsufficientDataException | ErrorResponseException | IOException |
                 InternalException e) {
            throw new RuntimeException(e); //抛出异常
        }
    }

    private String buildObjectName(String folder, String date, String originalFilename) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String extension = extractExtension(originalFilename);
        String safeName = extension.isEmpty() ? uuid : uuid + "." + extension;
        return folder + "/" + date + "/" + safeName;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).trim();
    }

    private String buildPublicUrl(String objectName) {
        return "/api/files/minio/" + encodePathSegment(bucket) + "/" + encodeObjectPath(objectName);
    }

    private String encodeObjectPath(String objectName) {
        return Arrays.stream(objectName.split("/"))
                .map(this::encodePathSegment)
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
