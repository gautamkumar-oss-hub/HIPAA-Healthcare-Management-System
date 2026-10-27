package com.healthcare.patient;

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
public class AppointmentAuthorizationClient {
    private final RestClient restClient;

    public AppointmentAuthorizationClient(RestClient.Builder builder,
                                          @Value("${healthcare.gateway-url}") String gatewayUrl) {
        this.restClient = builder.baseUrl(gatewayUrl).build();
    }

    public boolean isAssigned(UUID patientId, String accessToken) {
        try {
            Boolean assigned = restClient.get()
                    .uri("/api/appointments/assigned/{patientId}", patientId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(assigned);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound exception) {
            return false;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Could not verify patient assignment", exception);
        }
    }
}
