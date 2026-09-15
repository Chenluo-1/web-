package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.pojo.OrderItem;
import cn.ecom.domain.pojo.Review;
import cn.ecom.domain.vo.ReviewVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.OrderItemMapper;
import cn.ecom.mapper.OrderMapper;
import cn.ecom.mapper.ReviewMapper;
import cn.ecom.service.ReviewService;
import cn.ecom.utils.AppHttpCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public void submitReview(Long userId, Review review) {
        if (review.getOrderItemId() == null || review.getRating() == null
                || review.getRating() < 1 || review.getRating() > 5) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "评分需为 1-5 星");
        }

        OrderItem item = orderItemMapper.selectById(review.getOrderItemId());
        if (item == null) {
            throw new BusinessException(AppHttpCodeEnum.NOT_FOUND, "订单明细不存在");
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, item.getOrderNo()));
        if (order == null) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_NOT_EXIST);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(AppHttpCodeEnum.NO_OPERATION_AUTHORITY, "只能评价自己的订单");
        }
        // 简化版口径：订单完成（已收货）后方可评价
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new BusinessException(AppHttpCodeEnum.ORDER_STATUS_ERROR, "订单完成后才能评价");
        }

        Integer count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderItemId, item.getId()));
        if (count > 0) {
            throw new BusinessException(AppHttpCodeEnum.REVIEW_REPEAT);
        }

        review.setOrderNo(item.getOrderNo())
                .setUserId(userId)
                .setProductId(item.getProductId())
                .setSkuId(item.getSkuId())
                .setStatus(1)
                .setImages(null);
        if (review.getContent() != null) {
            review.setContent(review.getContent().trim());
        }
        reviewMapper.insert(review);
    }

    @Override
    public Page<ReviewVo> selectProductReviews(Integer pageNum, Integer pageSize, Long productId) {
        Page<ReviewVo> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 5 : pageSize);
        return (Page<ReviewVo>) reviewMapper.selectProductReviews(page, productId);
    }

    @Override
    public Double getAvgRating(Long productId) {
        Double avg = reviewMapper.selectAvgRating(productId);
        return avg == null ? 0D : avg;
    }

    @Override
    public List<Review> selectMyByOrder(Long userId, String orderNo) {
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getOrderNo, orderNo));
    }
}
