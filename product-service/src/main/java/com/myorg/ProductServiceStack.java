package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductServiceStack extends Stack {
    public ProductServiceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ProductServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function getProductsFunction = Function.Builder.create(this, "GetProductsFunction")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.handlers.AllProductsHandler::handleRequest")
                .code(Code.fromAsset("target/product-service-1.0.0-jar-with-dependencies.jar"))
                .timeout(Duration.seconds(30))
                .build();

        Function getProductByIdFunction = Function.Builder.create(this, "GetProductByIdFunction")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.handlers.ProductByIdHandler::handleRequest")
                .code(Code.fromAsset("target/product-service-1.0.0-jar-with-dependencies.jar"))
                .timeout(Duration.seconds(30))
                .build();

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "ProductApi")
                .handler(getProductsFunction)
                .deployOptions(StageOptions.builder().stageName("dev").build())
                .build();


        Resource resource = api.getRoot().addResource("products");

        resource.addMethod("GET", new LambdaIntegration(getProductsFunction), methodOptions());

        resource.addResource("{id}")
                .addMethod("GET", new LambdaIntegration(getProductByIdFunction), methodOptions());
    }

    private MethodOptions methodOptions() {
        // Настройка CORS
        Map<String, Boolean> responseParameters = new HashMap<>();
        responseParameters.put("method.response.header.Access-Control-Allow-Origin", true);

        MethodResponse methodResponse = MethodResponse.builder()
                .statusCode("200")
                .responseParameters(responseParameters)
                .build();

        return MethodOptions.builder()
                .methodResponses(List.of(methodResponse))
                .build();
    }
}
