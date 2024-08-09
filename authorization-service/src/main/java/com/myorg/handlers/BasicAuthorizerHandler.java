package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicAuthorizerHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse> {

    private final Map<String, String> users = new HashMap<>();

    @Override
    public IamPolicyResponse handleRequest(APIGatewayCustomAuthorizerEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        logger.log("ENV: " + users, LogLevel.INFO);
        logger.log("Event: " + event.toString(), LogLevel.INFO);
        String authorizationToken = event.getAuthorizationToken();
        if (authorizationToken == null || !authorizationToken.startsWith("Basic ")) {
            return generateResponsePolicy("user", IamPolicyResponse.DENY, event.getMethodArn());
        }

        String base64Credentials = authorizationToken.substring("Basic ".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return generateResponsePolicy("user", IamPolicyResponse.DENY, event.getMethodArn());
        }

        String username = values[0];
        String password = values[1];
        String expectedPassword = System.getenv(username);

        if (!StringUtils.equals(expectedPassword, password)) {
            return generateResponsePolicy(username, IamPolicyResponse.DENY, event.getMethodArn());
        }

        return generateResponsePolicy(username, IamPolicyResponse.ALLOW, event.getMethodArn());
    }

    private IamPolicyResponse generateResponsePolicy(String principalId, String effect, String resource) {
        IamPolicyResponse.Statement statement = new IamPolicyResponse.Statement();
        statement.setEffect(effect);
        statement.setAction(IamPolicyResponse.EXECUTE_API_INVOKE);
        statement.setResource(List.of(resource));

        return IamPolicyResponse.builder()
                .withPrincipalId(principalId)
                .withPolicyDocument(IamPolicyResponse.PolicyDocument.builder()
                        .withVersion(IamPolicyResponse.VERSION_2012_10_17)
                        .withStatement(List.of(statement))
                        .build())
                .build();
    }
}
