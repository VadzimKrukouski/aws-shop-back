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
import com.myorg.dto.Stock;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AllProductsHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final static String productsTableName = "Products";
    private final static String stocksTableName = "Stocks";

    private final DynamoDbClient dynamoDbClient;

    public AllProductsHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
    }


    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        LambdaLogger logger = context.getLogger();
        logger.log("Start get all products", LogLevel.INFO);
        try {
            ScanRequest productsScan = ScanRequest.builder()
                    .tableName(productsTableName)
                    .build();
            ScanResponse productsResponse = dynamoDbClient.scan(productsScan);
            logger.log("Scan products table success", LogLevel.INFO);
            List<Product> products = productsResponse.items().stream()
                    .map(DaoUtils::convertItemToProduct)
                    .collect(Collectors.toList());

            ScanRequest stocksScan = ScanRequest.builder()
                    .tableName(stocksTableName)
                    .build();
            ScanResponse stocksResponse = dynamoDbClient.scan(stocksScan);
            logger.log("Scan stocks table success", LogLevel.INFO);
            List<Stock> stocks = stocksResponse.items().stream()
                    .map(DaoUtils::convertItemToStock)
                    .collect(Collectors.toList());
            stocks.forEach(s -> {
                        Optional<Product> optionalProduct = products.stream()
                                .filter(p -> s.getProductId().equals(p.getId()))
                                .findFirst();
                        if (optionalProduct.isPresent()) {
                            Product product = optionalProduct.get();
                            product.setCount(s.getCount());
                        }
                    }
            );

            String bodyResponse = mapper.writeValueAsString(products);
            logger.log("Products: " + bodyResponse, LogLevel.INFO);
            return APIGatewayUtils.createOkResponse(bodyResponse);
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return APIGatewayUtils.createErrorResponse("Error when getting a list of all products");
        }
    }
}

