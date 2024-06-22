package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.Utils.APIGatewayUtils;
import com.myorg.Utils.DaoUtils;
import com.myorg.dto.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;

public class ProductByIdHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final static String productsTableName = "Products";
    private final static String stocksTableName = "Stocks";

    private final DynamoDbClient dynamoDbClient;

    public ProductByIdHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        LambdaLogger logger = context.getLogger();

        try {
            String id = event.getPathParameters().get("id");
            logger.log("Get product by id = " + id, LogLevel.INFO);

            GetItemRequest getProductsRequest = GetItemRequest.builder()
                    .tableName(productsTableName)
                    .key(Map.of("id", AttributeValue.builder().s(id).build()))
                    .build();

            GetItemResponse getProductsResponse = dynamoDbClient.getItem(getProductsRequest);

            if (getProductsResponse.hasItem()) {
                Map<String, AttributeValue> itemProduct = getProductsResponse.item();
                Product product = DaoUtils.convertItemToProduct(itemProduct);
                logger.log("Response products table success", LogLevel.INFO);

                GetItemRequest getStocksRequest = GetItemRequest.builder()
                        .tableName(stocksTableName)
                        .key(Map.of("product_id", AttributeValue.builder().s(id).build()))
                        .build();

                GetItemResponse getStocksResponse = dynamoDbClient.getItem(getStocksRequest);
                if (getStocksResponse.hasItem()) {
                    logger.log("Response stocks table success", LogLevel.INFO);
                    Map<String, AttributeValue> itemStock = getStocksResponse.item();
                    int count = Integer.parseInt(itemStock.get("count").n());
                    product.setCount(count);
                } else {
                    return APIGatewayUtils.createNotFoundResponse();
                }

                String bodyResponse = mapper.writeValueAsString(product);
                logger.log("Product response: " + bodyResponse, LogLevel.INFO);
                return APIGatewayUtils.createOkResponse(bodyResponse);
            } else {
                return APIGatewayUtils.createNotFoundResponse();
            }
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return APIGatewayUtils.createErrorResponse("Error when getting a product by id");
        }
    }
}
