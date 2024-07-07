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
import com.myorg.service.DynamoDbService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CreateProductHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final DynamoDbService dynamoDbService;

    public CreateProductHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        this.dynamoDbService = new DynamoDbService(dynamoDbClient);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        LambdaLogger logger = context.getLogger();

        try {
            Product product = mapper.readValue(event.getBody(), Product.class);
            logger.log("Create new product " + product.toString(), LogLevel.INFO);

            String productId = dynamoDbService.createItemProduct(product);
            dynamoDbService.createItemStock(productId, product.getCount());

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
