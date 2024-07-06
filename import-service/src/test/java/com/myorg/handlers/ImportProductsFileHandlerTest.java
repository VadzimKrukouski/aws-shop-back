package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportProductsFileHandlerTest {

    @Mock
    private AmazonS3 mockS3Client;

    @Mock
    private Context mockContext;

    private ImportProductsFileHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        handler = new ImportProductsFileHandler();
        when(mockContext.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    @Test
    public void testHandleRequestEmptyFileName() {
        APIGatewayV2HTTPEvent mockEvent = new APIGatewayV2HTTPEvent();
        mockEvent.setQueryStringParameters(Map.of("name", ""));

        APIGatewayV2HTTPResponse response = handler.handleRequest(mockEvent, mockContext);

        assertEquals(400, response.getStatusCode());
    }
}