package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.Utils.ProductsMock;
import com.myorg.dto.Product;
import com.myorg.handlers.AllProductsHandler;
import com.myorg.handlers.ProductByIdHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

public class ProductServiceTest {

    @Mock
    Context context;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getProductsTest() throws JsonProcessingException {
        AllProductsHandler handler = new AllProductsHandler();
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        ObjectMapper mapper = new ObjectMapper();
        event.setBody("Test");

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);
        Assertions.assertEquals(200, response.getStatusCode());
        List<Product> products = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        Assertions.assertEquals(ProductsMock.products.values().size(), products.size());
    }

    @Test
    public void getProductsByIdTest() throws JsonProcessingException {
        ProductByIdHandler handler = new ProductByIdHandler();
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        ObjectMapper mapper = new ObjectMapper();
        event.setPathParameters(Map.of("id", "1"));

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);
        Assertions.assertEquals(200, response.getStatusCode());
        Product product = mapper.readValue(response.getBody(), new TypeReference<>() {
        });
        Assertions.assertEquals("1", product.getId());
        Assertions.assertEquals("Product 1", product.getDescription());
    }
}
