package com.boutique.cart.client;

import com.boutique.cart.exception.DownstreamServiceException;
import com.boutique.cart.exception.InvalidCartOperationException;
import com.boutique.cart.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(
            RestClient.Builder builder,
            @Value("${clients.product.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ProductSnapshot requireActiveProduct(UUID productId) {
        try {
            ProductSnapshot response = restClient.get()
                    .uri("/api/v1/products/{productId}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, result) -> {
                        throw new ResourceNotFoundException("Product not found: " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, result) -> {
                        throw new DownstreamServiceException("Product Service is unavailable.");
                    })
                    .body(ProductSnapshot.class);

            if (response == null) {
                throw new DownstreamServiceException("Product Service returned an empty response.");
            }

            if (!"ACTIVE".equals(response.status())) {
                throw new InvalidCartOperationException("Product is not ACTIVE: " + productId);
            }

            return response;
        } catch (ResourceNotFoundException | InvalidCartOperationException | DownstreamServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownstreamServiceException("Unable to validate product.", exception);
        }
    }

    public record ProductSnapshot(
            UUID id,
            String sku,
            String name,
            String description,
            String category,
            BigDecimal price,
            String currency,
            String imageUrl,
            String status
    ) {
    }
}
