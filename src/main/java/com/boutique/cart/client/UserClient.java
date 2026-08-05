package com.boutique.cart.client;

import com.boutique.cart.exception.DownstreamServiceException;
import com.boutique.cart.exception.InvalidCartOperationException;
import com.boutique.cart.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            RestClient.Builder builder,
            @Value("${clients.user.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void requireActiveUser(UUID userId) {
        try {
            UserResponse response = restClient.get()
                    .uri("/api/v1/users/{userId}", userId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, result) -> {
                        throw new ResourceNotFoundException("User not found: " + userId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, result) -> {
                        throw new DownstreamServiceException("User Service is unavailable.");
                    })
                    .body(UserResponse.class);

            if (response == null) {
                throw new DownstreamServiceException("User Service returned an empty response.");
            }

            if (!"ACTIVE".equals(response.status())) {
                throw new InvalidCartOperationException("Only ACTIVE users can use a cart.");
            }
        } catch (ResourceNotFoundException | InvalidCartOperationException | DownstreamServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownstreamServiceException("Unable to validate user.", exception);
        }
    }

    private record UserResponse(UUID id, String status) {
    }
}
