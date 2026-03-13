package com.example.ecommerce.modules.user.repository.mysql;

import com.example.ecommerce.modules.user.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (email, password_hash, nickname, created_at, updated_at)
            VALUES (#{email}, #{passwordHash}, #{nickname}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
            UPDATE users
            SET email = #{email},
                password_hash = #{passwordHash},
                nickname = #{nickname},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(User user);

    @Select("""
            SELECT id, email, password_hash, nickname, created_at, updated_at
            FROM users
            WHERE email = #{email}
            LIMIT 1
            """)
    User findByEmail(@Param("email") String email);

    @Select("""
            SELECT id, email, password_hash, nickname, created_at, updated_at
            FROM users
            WHERE id = #{id}
            LIMIT 1
            """)
    User findById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM users
            WHERE email = #{email}
            """)
    int countByEmail(@Param("email") String email);
}
