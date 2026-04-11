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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

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
            // 生成唯一文件名(文件夹名/日期/UUID-初始文件名)
            String fileName = folder+"/"+date+"/"+ UUID.randomUUID().toString().replaceAll("-","") + "-" + file.getOriginalFilename();
            // 获取文件流
            InputStream fileInputStream = file.getInputStream();
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket) //传到哪个桶
                            .object(fileName) //folder+"/" 会自己创建对应文件夹
                            .stream(fileInputStream, file.getSize(), -1) //输入流
                            .contentType(file.getContentType()) //文件类型
                            .build()
            );

            // 返回可访问的 URL
            return endpoint + "/" + bucket + "/" + fileName;
        } catch (ServerException | XmlParserException | InvalidResponseException | InvalidKeyException |
                 NoSuchAlgorithmException | InsufficientDataException | ErrorResponseException | IOException |
                 InternalException e) {
            throw new RuntimeException(e); //抛出异常
        }
    }




}
