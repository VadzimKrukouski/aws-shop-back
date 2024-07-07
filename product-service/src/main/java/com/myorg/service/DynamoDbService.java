package com.myorg.service;

import com.myorg.dto.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamoDbService {
    private final static String productsTableName = "Products";
    private final static String stocksTableName = "Stocks";

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String createItemProduct(Product product) {
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

        return id;
    }

    public void createItemStock(String productId, int count) {
        Map<String, AttributeValue> stockItem = new HashMap<>();
        stockItem.put("product_id", AttributeValue.builder().s(productId).build());
        stockItem.put("count", AttributeValue.builder().n(String.valueOf(count)).build());

        PutItemRequest stockPutRequest = PutItemRequest.builder()
                .tableName(stocksTableName)
                .item(stockItem)
                .build();

        dynamoDbClient.putItem(stockPutRequest);
    }
}
