package com.myorg.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import java.util.Map;

public class APIGatewayUtils {

    public static final Map<String, String> HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*");

    public static APIGatewayV2HTTPResponse createErrorResponse(String reason) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(500)
                .withHeaders(HEADERS)
                .withBody(reason)
                .build();
    }

    public static APIGatewayV2HTTPResponse createOkResponse(String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withHeaders(HEADERS)
                .withBody(body)
                .build();
    }

    public static APIGatewayV2HTTPResponse createNotFoundResponse() {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(400)
                .withHeaders(HEADERS)
                .withBody("Product not found")
                .build();
    }

    public static APIGatewayV2HTTPResponse createInvalidDataResponse() {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(400)
                .withHeaders(HEADERS)
                .withBody("Data invalid")
                .build();
    }
}
