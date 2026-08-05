package com.boutique.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID userId,
        List<CartItemResponse> items,
        Integer totalItems,
        BigDecimal subtotal,
        String currency
) {
}
