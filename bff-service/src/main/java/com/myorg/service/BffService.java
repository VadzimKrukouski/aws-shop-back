package com.myorg.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BffService {


    private final RestTemplate restTemplate;

    public BffService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "productsCache", unless = "!#recipientURL.contains('product') " +
            "|| (#recipientURL.contains('product') && !#queryParams.isEmpty) " +
            "|| (#recipientURL.contains('product') && #body != null && !#body.empty)")
    public ResponseEntity<String> processData(String recipientURL, String methodOverride, Map<String, String> queryParams, String body) {
        HttpMethod method = getHttpMethod(methodOverride);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    buildUrlWithParams(recipientURL, queryParams),
                    method,
                    new HttpEntity<>(body, new HttpHeaders()),
                    String.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (
                HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Error processing request: " + e.getMessage());
        }
    }

    private HttpMethod getHttpMethod(String methodOverride) {
        if (methodOverride != null) {
            try {
                return HttpMethod.valueOf(methodOverride.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new UnsupportedOperationException("Unsupported HTTP method: " + methodOverride, e);
            }
        }
        return HttpMethod.GET;
    }

    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params.isEmpty()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        if (url.contains("product") && params.containsKey("id")) {
            return sb.append("/").append(params.get("id")).toString();
        }
        sb.append("?");
        params.forEach((key, value) -> sb.append(key).append("=").append(value).append("&"));
        return sb.deleteCharAt(sb.length() - 1).toString();
    }
}
