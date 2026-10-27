package com.healthcare.appointment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class UserDirectoryClient {
    private final RestClient restClient;

    public UserDirectoryClient(RestClient.Builder builder,
                               @Value("${healthcare.gateway-url}") String gatewayUrl) {
        this.restClient = builder.baseUrl(gatewayUrl).build();
    }

    public boolean isDoctor(UUID userId, String accessToken) {
        try {
            UserSummary user = restClient.get()
                    .uri("/api/auth/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(UserSummary.class);
            return user != null && "DOCTOR".equals(user.role());
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Could not verify doctor account", exception);
        }
    }

    private record UserSummary(UUID id, String email, String role, boolean mfaEnabled) {}
}
