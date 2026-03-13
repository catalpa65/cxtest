package com.example.ecommerce.modules.mq.repository.mysql;

import com.example.ecommerce.modules.mq.model.DeadLetterMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MqDeadLetterMapper {

    @Insert("""
            INSERT INTO mq_dead_letters (
                event_id, type, order_id, transaction_id, attempts, last_error, failed_at
            )
            VALUES (
                #{eventId}, #{type}, #{orderId}, #{transactionId}, #{attempts}, #{lastError}, #{failedAt}
            )
            ON DUPLICATE KEY UPDATE
                type = VALUES(type),
                order_id = VALUES(order_id),
                transaction_id = VALUES(transaction_id),
                attempts = VALUES(attempts),
                last_error = VALUES(last_error),
                failed_at = VALUES(failed_at)
            """)
    int upsert(DeadLetterMessage deadLetterMessage);

    @Select("""
            SELECT event_id, type, order_id, transaction_id, attempts, last_error, failed_at
            FROM mq_dead_letters
            ORDER BY failed_at DESC
            """)
    List<DeadLetterMessage> listAll();

    @Select("""
            SELECT event_id, type, order_id, transaction_id, attempts, last_error, failed_at
            FROM mq_dead_letters
            WHERE event_id = #{eventId}
            LIMIT 1
            """)
    DeadLetterMessage findByEventId(@Param("eventId") String eventId);

    @Delete("""
            DELETE FROM mq_dead_letters
            WHERE event_id = #{eventId}
            """)
    int deleteByEventId(@Param("eventId") String eventId);
}
