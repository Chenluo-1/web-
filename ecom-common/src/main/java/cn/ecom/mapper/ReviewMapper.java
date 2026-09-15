package cn.ecom.mapper;

import cn.ecom.domain.pojo.Review;
import cn.ecom.domain.vo.ReviewVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT r.id, r.rating, r.content, r.create_time AS createTime, u.nick_name AS nickName " +
            "FROM ec_review r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.product_id = #{productId} AND r.status = 1 AND r.del_flag = 0 " +
            "ORDER BY r.id DESC")
    IPage<ReviewVo> selectProductReviews(IPage<ReviewVo> page, @Param("productId") Long productId);

    @Select("SELECT IFNULL(ROUND(AVG(rating), 1), 0) FROM ec_review " +
            "WHERE product_id = #{productId} AND status = 1 AND del_flag = 0")
    Double selectAvgRating(@Param("productId") Long productId);
}
