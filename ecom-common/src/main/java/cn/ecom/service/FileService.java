package cn.ecom.service;

import cn.ecom.domain.vo.FileUploadVo;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /** 上传图片到 MinIO，返回访问 URL */
    ResponseResult<FileUploadVo> upload(MultipartFile file) throws Exception;
}
