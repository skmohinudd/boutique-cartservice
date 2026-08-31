package com.boutique.cart.client;

import com.boutique.cart.exception.DownstreamServiceException;
import com.boutique.cart.exception.InvalidCartOperationException;
import com.boutique.cart.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("/api/v1/users/{userId}", userId);

            String authorization = inboundAuthorization();
            if (authorization != null) {
                request.header(HttpHeaders.AUTHORIZATION, authorization);
            }

            UserResponse response = request
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (httpRequest, result) -> {
                        throw new ResourceNotFoundException("User not found: " + userId);
                    })
                    .onStatus(status -> status.value() == 401 || status.value() == 403,
                            (httpRequest, result) -> {
                                throw new DownstreamServiceException(
                                        "User authentication could not be validated by User Service."
                                );
                            })
                    .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, result) -> {
                        throw new DownstreamServiceException("User Service is unavailable.");
                    })
                    .body(UserResponse.class);

            if (response == null) {
                throw new DownstreamServiceException(
                        "User Service returned an empty response."
                );
            }

            if (!"ACTIVE".equals(response.status())) {
                throw new InvalidCartOperationException(
                        "Only ACTIVE users can use a cart."
                );
            }
        } catch (ResourceNotFoundException
                 | InvalidCartOperationException
                 | DownstreamServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DownstreamServiceException(
                    "Unable to validate user.",
                    exception
            );
        }
    }

    private String inboundAuthorization() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        String authorization = attributes.getRequest()
                .getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization
                : null;
    }

    private record UserResponse(UUID id, String status) {
    }
}
