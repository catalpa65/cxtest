package com.example.ecommerce.modules.mq.repository.mysql;

import com.example.ecommerce.modules.mq.model.AsyncProcessLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MqProcessLogMapper {

    @Insert("""
            INSERT INTO mq_process_logs (
                event_id, type, order_id, transaction_id, attempts, status, message, processed_at
            )
            VALUES (
                #{eventId}, #{type}, #{orderId}, #{transactionId}, #{attempts}, #{status}, #{message}, #{at}
            )
            """)
    int insert(AsyncProcessLog log);

    @Select("""
            SELECT event_id, type, order_id, transaction_id, attempts, status, message, processed_at AS at
            FROM mq_process_logs
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<AsyncProcessLog> listLatest(@Param("limit") int limit);

    @Delete("""
            DELETE FROM mq_process_logs
            WHERE id NOT IN (
                SELECT id FROM (
                    SELECT id FROM mq_process_logs ORDER BY id DESC LIMIT #{retainSize}
                ) as keep_rows
            )
            """)
    int trimToRetainSize(@Param("retainSize") int retainSize);
}
