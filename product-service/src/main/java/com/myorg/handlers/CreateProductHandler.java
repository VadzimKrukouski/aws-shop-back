package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.Utils.APIGatewayUtils;
import com.myorg.dto.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateProductHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final static String productsTableName = "Products";
    private final static String stocksTableName = "Stocks";

    private final DynamoDbClient dynamoDbClient;

    public CreateProductHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        LambdaLogger logger = context.getLogger();

        try {
            Product product = mapper.readValue(event.getBody(), Product.class);
            logger.log("Create new product " + product.toString(), LogLevel.INFO);

            String id = UUID.randomUUID().toString();
            product.setId(id);

            // Добавление в таблицу Products
            Map<String, AttributeValue> productItem = new HashMap<>();
            productItem.put("id", AttributeValue.builder().s(product.getId()).build());
            productItem.put("title", AttributeValue.builder().s(product.getTitle()).build());
            productItem.put("description", AttributeValue.builder().s(product.getDescription()).build());
            productItem.put("price", AttributeValue.builder().n(Double.toString(product.getPrice())).build());

            PutItemRequest productPutRequest = PutItemRequest.builder()
                    .tableName(productsTableName)
                    .item(productItem)
                    .build();

            dynamoDbClient.putItem(productPutRequest);

            // Добавление в таблицу Stocks
            Map<String, AttributeValue> stockItem = new HashMap<>();
            stockItem.put("product_id", AttributeValue.builder().s(id).build());
            stockItem.put("count", AttributeValue.builder().n(String.valueOf(product.getCount())).build());

            PutItemRequest stockPutRequest = PutItemRequest.builder()
                    .tableName(stocksTableName)
                    .item(stockItem)
                    .build();

            dynamoDbClient.putItem(stockPutRequest);
            logger.log("Created", LogLevel.INFO);
            return APIGatewayUtils.createOkResponse("Created!");
        } catch (JsonProcessingException e) {
            logger.log("Error: " + e.getMessage(), LogLevel.ERROR);
            return APIGatewayUtils.createInvalidDataResponse();
        } catch (Exception e) {
            logger.log("Error: " + e.getMessage(), LogLevel.ERROR);
            return APIGatewayUtils.createErrorResponse("Internal Server Error");
        }
    }
}
