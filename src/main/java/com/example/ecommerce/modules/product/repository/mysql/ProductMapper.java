package com.example.ecommerce.modules.product.repository.mysql;

import com.example.ecommerce.modules.product.model.Product;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Insert("""
            INSERT INTO products (name, description, price, stock, status, created_at, updated_at)
            VALUES (#{name}, #{description}, #{price}, #{stock}, #{status}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("""
            UPDATE products
            SET name = #{name},
                description = #{description},
                price = #{price},
                stock = #{stock},
                status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(Product product);

    @Select("""
            SELECT id, name, description, price, stock, status, created_at, updated_at
            FROM products
            WHERE id = #{id}
            LIMIT 1
            """)
    Product findById(@Param("id") Long id);

    @Select("""
            SELECT id, name, description, price, stock, status, created_at, updated_at
            FROM products
            """)
    List<Product> findAll();

    @Update("""
            UPDATE products
            SET stock = stock - #{quantity},
                updated_at = NOW(3)
            WHERE id = #{productId}
              AND stock >= #{quantity}
            """)
    int reserveStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("""
            UPDATE products
            SET stock = stock + #{quantity},
                updated_at = NOW(3)
            WHERE id = #{productId}
            """)
    int releaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
