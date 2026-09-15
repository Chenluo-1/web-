package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.vo.FileUploadVo;
import cn.ecom.service.FileService;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传（MinIO）
 */
@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /** 上传图片，返回访问 URL */
    @OperLog(module = "文件", type = "上传")
    @PostMapping("/upload")
    public ResponseResult<FileUploadVo> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return fileService.upload(file);
    }
}
