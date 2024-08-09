package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthServiceStack extends Stack {

    public AuthServiceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AuthServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Map<String, String> environmentMap = loadEnvironmentVariables();
        environmentMap.forEach((key, value) -> {
            System.out.println("Env Var: " + key + " = " + value);
        });

        Function basicAuthorizerLambda = new Function(this, "BasicAuthorizer", FunctionProps.builder()
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.handlers.BasicAuthorizerHandler::handleRequest")
                .code(Code.fromAsset("target/authorization-service-1.0.0-jar-with-dependencies.jar"))
                .environment(environmentMap)
                .build());
    }

    private Map<String, String> loadEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("./src/main/resources/.env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int index = line.indexOf('=');
                    if (index != -1) {
                        String key = line.substring(0, index).trim();
                        String value = line.substring(index + 1).trim();
                        envVars.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return envVars;
    }
}
