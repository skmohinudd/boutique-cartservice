package com.boutique.cart.service;

import com.boutique.cart.client.InventoryClient;
import com.boutique.cart.client.ProductClient;
import com.boutique.cart.client.UserClient;
import com.boutique.cart.dto.AddCartItemRequest;
import com.boutique.cart.dto.CartResponse;
import com.boutique.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Test
    void addsValidatedItemAndReturnsCart() {
        CartRepository repository = mock(CartRepository.class);
        UserClient userClient = mock(UserClient.class);
        ProductClient productClient = mock(ProductClient.class);
        InventoryClient inventoryClient = mock(InventoryClient.class);

        CartService service = new CartService(
                repository,
                userClient,
                productClient,
                inventoryClient
        );

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductClient.ProductSnapshot product =
                new ProductClient.ProductSnapshot(
                        productId,
                        "SKU-1",
                        "Sunglasses",
                        "Description",
                        "Accessories",
                        new BigDecimal("19.99"),
                        "USD",
                        "/static/sunglasses.jpg",
                        "ACTIVE"
                );

        InventoryClient.InventorySnapshot inventory =
                new InventoryClient.InventorySnapshot(
                        productId,
                        10,
                        0,
                        10,
                        0L
                );

        when(productClient.requireActiveProduct(productId)).thenReturn(product);
        when(inventoryClient.getInventory(productId)).thenReturn(inventory);
        when(repository.addQuantity(userId, productId, 2)).thenReturn(2);

        Map<UUID, Integer> stored = new LinkedHashMap<>();
        stored.put(productId, 2);
        when(repository.findItems(userId)).thenReturn(stored);

        CartResponse response = service.addItem(
                userId,
                new AddCartItemRequest(productId, 2)
        );

        assertEquals(2, response.totalItems());
        assertEquals(new BigDecimal("39.98"), response.subtotal());
        verify(userClient, atLeastOnce()).requireActiveUser(userId);
        verify(repository).addQuantity(userId, productId, 2);
    }
}
