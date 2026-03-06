package com.atguigu.exam.service;


import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务
 * 支持MinIO和本地文件存储两种方式
 */

public interface FileUploadService {

    /**
     * 文件上传服务
     * @param folder 要传到 minio 的哪个文件夹(轮播图：banners; 视频：videos)
     * @param file 文件
     * @return 返回文件地址
     */
    String uploadFile(String folder,MultipartFile file);
} 