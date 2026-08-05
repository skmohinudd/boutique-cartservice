package com.boutique.cart.client;

import com.boutique.cart.exception.DownstreamServiceException;
import com.boutique.cart.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            RestClient.Builder builder,
            @Value("${clients.inventory.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public InventorySnapshot getInventory(UUID productId) {
        try {
            InventorySnapshot response = restClient.get()
                    .uri("/api/v1/inventory/{productId}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, result) -> {
                        throw new ResourceNotFoundException("Inventory not found for product: " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, result) -> {
                        throw new DownstreamServiceException("Inventory Service is unavailable.");
                    })
                    .body(InventorySnapshot.class);

            if (response == null) {
                throw new DownstreamServiceException("Inventory Service returned an empty response.");
            }

            return response;
        } catch (ResourceNotFoundException | DownstreamServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownstreamServiceException("Unable to retrieve inventory.", exception);
        }
    }

    public record InventorySnapshot(
            UUID productId,
            Integer availableQuantity,
            Integer reservedQuantity,
            Integer sellableQuantity,
            Long version
    ) {
    }
}
