package com.example.ecommerce.modules.order.repository.mysql;

import com.example.ecommerce.modules.order.model.OrderItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Delete("""
            DELETE FROM order_items
            WHERE order_id = #{orderId}
            """)
    int deleteByOrderId(@Param("orderId") Long orderId);

    @Insert("""
            INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, line_amount)
            VALUES (#{orderId}, #{item.productId}, #{item.productName}, #{item.unitPrice}, #{item.quantity}, #{item.lineAmount})
            """)
    int insert(@Param("orderId") Long orderId, @Param("item") OrderItem item);

    @Select("""
            SELECT product_id, product_name, unit_price, quantity, line_amount
            FROM order_items
            WHERE order_id = #{orderId}
            ORDER BY id ASC
            """)
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}
