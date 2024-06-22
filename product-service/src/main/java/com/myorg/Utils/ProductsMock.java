package com.myorg.Utils;

import com.myorg.dto.Product;

import java.util.Map;

public interface ProductsMock {
    Product product1 = new Product("1", "Product 1", 1.10);
    Product product2 = new Product("2", "Product 2", 2.50);
    Product product3 = new Product("3", "Product 3", 3.98);

    Map<String, Product> products = Map.of("1", product1, "2", product2, "3", product3);

}
