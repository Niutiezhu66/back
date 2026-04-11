package com.back.exam.service;

import com.back.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface BannerService extends IService<Banner> {

    String upLoadBannerImage(MultipartFile file);
}