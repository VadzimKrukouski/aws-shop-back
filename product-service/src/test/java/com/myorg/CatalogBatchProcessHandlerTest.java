package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.dto.Product;
import com.myorg.handlers.CatalogBatchProcessHandler;
import com.myorg.service.DynamoDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CatalogBatchProcessHandlerTest {

    @Mock
    private AmazonSNS snsClient;

    @Mock
    private DynamoDbService dynamoDbService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private CatalogBatchProcessHandler handler;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getLogger()).thenReturn(logger);
        System.setProperty("TOPIC_ARN", "arn:aws:sns:us-east-1:123456789012:createProductTopic");
        handler = new CatalogBatchProcessHandler(dynamoDbService, snsClient);
    }

    @Test
    public void testHandleRequest() throws Exception {
        Product testProduct = new Product("123", "Test Product", "Test Description", 10.0, 100);
        String testProductJson = objectMapper.writeValueAsString(testProduct);

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody(testProductJson);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Collections.singletonList(message));

        given(dynamoDbService.createItemProduct(any(Product.class))).willReturn("123");

        ArgumentCaptor<PublishRequest> publishRequestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        when(snsClient.publish(publishRequestCaptor.capture())).thenReturn(new PublishResult());

        handler.handleRequest(sqsEvent, context);

        verify(dynamoDbService, times(1)).createItemProduct(any());

        verify(snsClient, times(1)).publish(any(PublishRequest.class));

        verify(logger, times(1)).log("Id - " + message.getMessageId(), LogLevel.INFO);
        verify(logger, times(1)).log("Body - " + testProductJson, LogLevel.INFO);
        verify(logger, times(1)).log("Product from message: " + testProduct.toString(), LogLevel.INFO);
        verify(logger, times(1)).log("Created product with id: 123", LogLevel.INFO);
        verify(logger, times(1)).log("SNS publish successfully", LogLevel.INFO);
    }
}
