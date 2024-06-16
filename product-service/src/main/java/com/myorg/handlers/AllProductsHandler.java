package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.Utils.APIGatewayUtils;
import com.myorg.Utils.ProductsMock;

public class AllProductsHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        LambdaLogger logger = context.getLogger();
        try {
            String bodyResponse = mapper.writeValueAsString(ProductsMock.products.values());
            return APIGatewayUtils.createOkResponse(bodyResponse);
        } catch (Exception e) {
            return APIGatewayUtils.createErrorResponse("Error when getting a list of all products");
        }
    }
}

