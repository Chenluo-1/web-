package cn.ecom.service.impl;

import cn.ecom.domain.vo.FileUploadVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.service.FileService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.ResponseResult;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务：MinIO 存储（照 seig 的上传逻辑 + 类型/大小校验 + 防重名）
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXT = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp"));

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    public FileServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public ResponseResult<FileUploadVo> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(AppHttpCodeEnum.FILE_UPLOAD_ERROR, "请选择文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(AppHttpCodeEnum.FILE_UPLOAD_ERROR, "仅支持图片格式：jpg/jpeg/png/gif/webp/bmp");
        }

        // 桶不存在则创建，并设为公共读（前端 <img> 直连展示）
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":[\"s3:GetObject\"],"
                    + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},"
                    + "\"Resource\":\"arn:aws:s3:::" + bucket + "/*\"}]}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        }

        String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        String url = endpoint + "/" + bucket + "/" + objectName;
        return ResponseResult.okResult(new FileUploadVo(url, original));
    }
}
