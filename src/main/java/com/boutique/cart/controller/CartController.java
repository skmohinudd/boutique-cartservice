package com.boutique.cart.controller;

import com.boutique.cart.dto.*;
import com.boutique.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts/{userId}")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
            @PathVariable UUID userId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(userId, request);
    }

    @GetMapping
    public CartResponse getCart(@PathVariable UUID userId) {
        return cartService.getCart(userId);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            @PathVariable UUID userId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(userId, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @PathVariable UUID userId,
            @PathVariable UUID productId
    ) {
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable UUID userId) {
        cartService.clearCart(userId);
    }
}
