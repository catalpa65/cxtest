package com.example.ecommerce.modules.cart.service;

import com.example.ecommerce.common.auth.AuthContext;
import com.example.ecommerce.common.auth.UserPrincipal;
import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.cart.dto.AddCartItemRequest;
import com.example.ecommerce.modules.cart.dto.CartItemResponse;
import com.example.ecommerce.modules.cart.dto.CartResponse;
import com.example.ecommerce.modules.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.modules.cart.repository.CartRepository;
import com.example.ecommerce.modules.product.model.Product;
import com.example.ecommerce.modules.product.model.ProductStatus;
import com.example.ecommerce.modules.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartResponse addItem(AddCartItemRequest request) {
        Long userId = currentUserId();
        Product product = validateAddableProduct(request.getProductId());
        cartRepository.addQuantity(userId, product.getId(), request.getQuantity());
        return buildCart(userId);
    }

    public CartResponse updateItem(Long productId, UpdateCartItemRequest request) {
        Long userId = currentUserId();
        validateAddableProduct(productId);
        cartRepository.setQuantity(userId, productId, request.getQuantity());
        return buildCart(userId);
    }

    public CartResponse removeItem(Long productId) {
        Long userId = currentUserId();
        cartRepository.removeItem(userId, productId);
        return buildCart(userId);
    }

    public CartResponse detail() {
        return buildCart(currentUserId());
    }

    private CartResponse buildCart(Long userId) {
        Map<Long, Integer> items = cartRepository.getItems(userId);
        if (items.isEmpty()) {
            return CartResponse.empty();
        }

        List<CartItemResponse> cartItems = items.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> {
                    Product product = productRepository.findById(entry.getKey())
                            .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "购物车商品不存在: " + entry.getKey()));
                    BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
                    return new CartItemResponse(
                            product.getId(),
                            product.getName(),
                            product.getPrice(),
                            entry.getValue(),
                            lineAmount,
                            product.getStatus()
                    );
                })
                .toList();

        int totalQuantity = cartItems.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = cartItems.stream()
                .map(CartItemResponse::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cartItems, totalQuantity, totalAmount);
    }

    private Product validateAddableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在: " + productId));
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品未上架，无法加入购物车: " + productId);
        }
        return product;
    }

    private Long currentUserId() {
        UserPrincipal principal = AuthContext.get();
        if (principal == null || principal.userId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return principal.userId();
    }
}
