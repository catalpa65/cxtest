package com.example.ecommerce.modules.order.repository.mysql;

import com.example.ecommerce.modules.order.model.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
            INSERT INTO orders (
                user_id, status, total_amount, expires_at, payment_transaction_id,
                paid_at, completed_at, created_at, updated_at
            )
            VALUES (
                #{userId}, #{status}, #{totalAmount}, #{expiresAt}, #{paymentTransactionId},
                #{paidAt}, #{completedAt}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("""
            UPDATE orders
            SET user_id = #{userId},
                status = #{status},
                total_amount = #{totalAmount},
                expires_at = #{expiresAt},
                payment_transaction_id = #{paymentTransactionId},
                paid_at = #{paidAt},
                completed_at = #{completedAt},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(Order order);

    @Select("""
            SELECT id, user_id, status, total_amount, expires_at, payment_transaction_id,
                   paid_at, completed_at, created_at, updated_at
            FROM orders
            WHERE id = #{id}
            LIMIT 1
            """)
    Order findById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, status, total_amount, expires_at, payment_transaction_id,
                   paid_at, completed_at, created_at, updated_at
            FROM orders
            WHERE user_id = #{userId}
            """)
    List<Order> findByUserId(@Param("userId") Long userId);
}
