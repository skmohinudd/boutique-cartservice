package com.boutique.cart.service;

import com.boutique.cart.client.InventoryClient;
import com.boutique.cart.client.ProductClient;
import com.boutique.cart.client.UserClient;
import com.boutique.cart.dto.*;
import com.boutique.cart.exception.InvalidCartOperationException;
import com.boutique.cart.exception.ResourceNotFoundException;
import com.boutique.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public CartService(
            CartRepository cartRepository,
            UserClient userClient,
            ProductClient productClient,
            InventoryClient inventoryClient
    ) {
        this.cartRepository = cartRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        userClient.requireActiveUser(userId);
        productClient.requireActiveProduct(request.productId());

        InventoryClient.InventorySnapshot inventory =
                inventoryClient.getInventory(request.productId());

        if (request.quantity() > inventory.sellableQuantity()) {
            throw new InvalidCartOperationException(
                    "Requested quantity exceeds sellable inventory."
            );
        }

        int updatedQuantity = cartRepository.addQuantity(
                userId,
                request.productId(),
                request.quantity()
        );

        if (updatedQuantity < 0) {
            throw new InvalidCartOperationException(
                    "Cart item quantity cannot exceed 99."
            );
        }

        if (updatedQuantity > inventory.sellableQuantity()) {
            cartRepository.setQuantity(
                    userId,
                    request.productId(),
                    updatedQuantity - request.quantity()
            );
            throw new InvalidCartOperationException(
                    "Total cart quantity exceeds sellable inventory."
            );
        }

        return getCart(userId);
    }

    public CartResponse getCart(UUID userId) {
        userClient.requireActiveUser(userId);

        Map<UUID, Integer> storedItems = cartRepository.findItems(userId);
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;
        String currency = null;

        for (Map.Entry<UUID, Integer> entry : storedItems.entrySet()) {
            ProductClient.ProductSnapshot product =
                    productClient.requireActiveProduct(entry.getKey());
            InventoryClient.InventorySnapshot inventory =
                    inventoryClient.getInventory(entry.getKey());

            if (currency != null && !currency.equals(product.currency())) {
                throw new InvalidCartOperationException(
                        "A cart cannot contain multiple currencies."
                );
            }

            currency = product.currency();
            int quantity = entry.getValue();
            BigDecimal lineTotal = product.price()
                    .multiply(BigDecimal.valueOf(quantity));

            items.add(new CartItemResponse(
                    product.id(),
                    product.sku(),
                    product.name(),
                    product.imageUrl(),
                    product.price(),
                    product.currency(),
                    quantity,
                    inventory.sellableQuantity(),
                    quantity <= inventory.sellableQuantity(),
                    lineTotal
            ));

            totalItems += quantity;
            subtotal = subtotal.add(lineTotal);
        }

        return new CartResponse(
                userId,
                List.copyOf(items),
                totalItems,
                subtotal,
                currency == null ? "USD" : currency
        );
    }

    public CartResponse updateItem(
            UUID userId,
            UUID productId,
            UpdateCartItemRequest request
    ) {
        userClient.requireActiveUser(userId);
        productClient.requireActiveProduct(productId);

        Map<UUID, Integer> items = cartRepository.findItems(userId);
        if (!items.containsKey(productId)) {
            throw new ResourceNotFoundException(
                    "Product is not present in the cart: " + productId
            );
        }

        InventoryClient.InventorySnapshot inventory =
                inventoryClient.getInventory(productId);

        if (request.quantity() > inventory.sellableQuantity()) {
            throw new InvalidCartOperationException(
                    "Requested quantity exceeds sellable inventory."
            );
        }

        cartRepository.setQuantity(userId, productId, request.quantity());
        return getCart(userId);
    }

    public CartResponse removeItem(UUID userId, UUID productId) {
        userClient.requireActiveUser(userId);

        if (!cartRepository.removeItem(userId, productId)) {
            throw new ResourceNotFoundException(
                    "Product is not present in the cart: " + productId
            );
        }

        return getCart(userId);
    }

    public void clearCart(UUID userId) {
        userClient.requireActiveUser(userId);
        cartRepository.clear(userId);
    }
}
