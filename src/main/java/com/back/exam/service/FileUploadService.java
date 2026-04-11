package com.back.exam.service;


import org.springframework.web.multipart.MultipartFile;


public interface FileUploadService {

    String uploadFile(String folder,MultipartFile file);
} 