package com.boutique.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String sku,
        String name,
        String imageUrl,
        BigDecimal unitPrice,
        String currency,
        Integer quantity,
        Integer sellableQuantity,
        boolean available,
        BigDecimal lineTotal
) {
}
