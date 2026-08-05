package com.boutique.cart.controller;

import com.boutique.cart.dto.CartResponse;
import com.boutique.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest {

    @Test
    void addItemReturns201() throws Exception {
        CartService service = mock(CartService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CartController(service))
                .build();

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(service.addItem(eq(userId), any()))
                .thenReturn(new CartResponse(
                        userId,
                        List.of(),
                        0,
                        BigDecimal.ZERO,
                        "USD"
                ));

        mockMvc.perform(post("/api/v1/carts/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }
}
