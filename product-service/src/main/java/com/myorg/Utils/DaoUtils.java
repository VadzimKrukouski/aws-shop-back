package com.myorg.Utils;

import com.myorg.dto.Product;
import com.myorg.dto.Stock;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class DaoUtils {

    public static Product convertItemToProduct(Map<String, AttributeValue> item) {
        String id = item.get("id").s();
        String title = item.get("title").s();
        String description = item.get("description").s();
        int price = Integer.parseInt(item.get("price").n());

        return new Product(id, title, description, price);
    }

    public static Stock convertItemToStock(Map<String, AttributeValue> item) {
        String productId = item.get("product_id").s();
        int count = Integer.parseInt(item.get("count").n());

        return new Stock(productId, count);
    }
}
