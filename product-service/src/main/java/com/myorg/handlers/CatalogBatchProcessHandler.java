package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.dto.Product;
import com.myorg.service.DynamoDbService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CatalogBatchProcessHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AmazonSNS snsClient;
    private final DynamoDbService dynamoDbService;
    private final String topicArn = System.getenv("TOPIC_ARN");

    public CatalogBatchProcessHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        this.dynamoDbService = new DynamoDbService(dynamoDbClient);
        this.snsClient = AmazonSNSClientBuilder.defaultClient();
    }

    public CatalogBatchProcessHandler(DynamoDbService dynamoDbService, AmazonSNS amazonSNS) {
        this.dynamoDbService = dynamoDbService;
        this.snsClient = amazonSNS;
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            try {
                logger.log("Id - " + msg.getMessageId(), LogLevel.INFO);
                logger.log("Body - " + msg.getBody(), LogLevel.INFO);

                Product product = objectMapper.readValue(msg.getBody(), Product.class);
                logger.log("Product from message: " + product.toString(), LogLevel.INFO);
                String productId = dynamoDbService.createItemProduct(product);
                dynamoDbService.createItemStock(productId, product.getCount());
                logger.log("Created product with id: " + productId, LogLevel.INFO);

                product.setId(productId);
                PublishRequest publishRequest = new PublishRequest()
                        .withTopicArn(topicArn)
                        .withMessage("Created product: " + product)
                        .addMessageAttributesEntry("title", new MessageAttributeValue()
                                .withDataType("String")
                                .withStringValue(product.getTitle()))
                        .addMessageAttributesEntry("price", new MessageAttributeValue()
                                .withDataType("Number")
                                .withStringValue(String.valueOf(product.getPrice())));
                snsClient.publish(publishRequest);
                logger.log("SNS publish successfully", LogLevel.INFO);
            } catch (Exception e) {
                logger.log("Error processing message: " + msg.getBody() + " Error: " + e.getMessage(), LogLevel.ERROR);
            }
        }
        return null;
    }
}
