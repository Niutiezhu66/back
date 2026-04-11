package com.back.exam.service.impl;
import com.back.exam.entity.Banner;
import com.back.exam.mapper.BannerMapper;
import com.back.exam.service.BannerService;

import com.back.exam.service.FileUploadService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
    @Autowired
    private FileUploadService fileUploadService;

    //上传轮播图
    @Override
    public String upLoadBannerImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空");
        }
        //文件为图片时，contentType以 image 开头
        String contentType = file.getContentType();
        if (ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image") ) {
            throw new RuntimeException("文件类型错误");
        }
        //上传图片
        String fileUrl = fileUploadService.uploadFile("banners", file);
        return fileUrl;
    }

}