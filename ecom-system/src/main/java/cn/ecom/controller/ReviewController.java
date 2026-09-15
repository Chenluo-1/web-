package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.Review;
import cn.ecom.domain.vo.ReviewVo;
import cn.ecom.service.ReviewService;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品评价（简化实现：已完成订单可评，一次评价星级+文字）
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 提交评价（登录用户，仅自己的已完成订单） */
    @OperLog(module = "评价", type = "新增")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Review review) {
        reviewService.submitReview(SecurityUtils.getUserId(), review);
        return ResponseResult.okResult();
    }

    /** 商品评价分页（游客可看） */
    @GetMapping("/product/{productId}")
    public ResponseResult<Page<ReviewVo>> selectByProduct(@PathVariable Long productId,
                                                          Integer pageNum, Integer pageSize) {
        return ResponseResult.okResult(reviewService.selectProductReviews(pageNum, pageSize, productId));
    }

    /** 商品平均评分（游客可看） */
    @GetMapping("/product/{productId}/avg")
    public ResponseResult<Double> avgRating(@PathVariable Long productId) {
        return ResponseResult.okResult(reviewService.getAvgRating(productId));
    }

    /** 我在某订单下已提交的评价（用于隐藏已评按钮） */
    @GetMapping("/order/{orderNo}")
    public ResponseResult<List<Review>> selectMyByOrder(@PathVariable String orderNo) {
        return ResponseResult.okResult(reviewService.selectMyByOrder(SecurityUtils.getUserId(), orderNo));
    }
}
