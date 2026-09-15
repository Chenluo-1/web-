package cn.ecom.service;

import cn.ecom.domain.pojo.Review;
import cn.ecom.domain.vo.ReviewVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ReviewService {

    /** 提交评价（简化版：仅已完成订单可评，一个明细只能评一次） */
    void submitReview(Long userId, Review review);

    /** 商品评价分页（游客可见） */
    Page<ReviewVo> selectProductReviews(Integer pageNum, Integer pageSize, Long productId);

    /** 商品平均评分（游客可见） */
    Double getAvgRating(Long productId);

    /** 当前用户在某订单下已提交的评价（用于前端隐藏已评按钮） */
    List<Review> selectMyByOrder(Long userId, String orderNo);
}
