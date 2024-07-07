package com.myorg;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.amazon.awscdk.services.sqs.IQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueAttributes;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportServiceStack extends Stack {

    public ImportServiceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ImportServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket bucket = Bucket.Builder.create(this, "ImportBucket")
                .bucketName("shop-import-svc-file-bucket")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .cors(List.of(CorsRule.builder()
                        .allowedMethods(List.of(HttpMethods.GET, HttpMethods.PUT))
                        .allowedOrigins(List.of("*"))
                        .allowedHeaders(List.of("*"))
                        .build()))
                .removalPolicy(RemovalPolicy.DESTROY)
                .versioned(true)
                .autoDeleteObjects(true)
                .build();

        Function importProductsFileLambda = Function.Builder.create(this, "ImportProductsFileLambda")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.handlers.ImportProductsFileHandler::handleRequest")
                .code(Code.fromAsset("target/import-service-1.0.0-jar-with-dependencies.jar"))
                .timeout(Duration.seconds(30))
                .build();
        importProductsFileLambda.addEnvironment("BUCKET_NAME", bucket.getBucketName());
        importProductsFileLambda.addEnvironment("UPLOADED_FOLDER", "uploaded");

        bucket.grantReadWrite(importProductsFileLambda);

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "ImportApi")
                .handler(importProductsFileLambda)
                .deployOptions(StageOptions.builder().stageName("dev").build())
                .build();

        api.getRoot().addResource("import").addMethod("GET", new LambdaIntegration(importProductsFileLambda), methodOptions());

        importProductsFileLambda.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("s3:PutObject"))
                .resources(List.of(bucket.getBucketArn() + "/uploaded/*"))
                .build());

        Function importFileParserLambda = Function.Builder.create(this, "ImportFileParserLambda")
                .runtime(Runtime.JAVA_17)
                .handler("com.myorg.handlers.ImportFileParserHandler::handleRequest")
                .code(Code.fromAsset("target/import-service-1.0.0-jar-with-dependencies.jar"))
                .timeout(Duration.seconds(30))
                .build();

        AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();
        String queueName = "catalogItemsQueue";
        String queueUrl = amazonSQS.getQueueUrl(queueName).getQueueUrl();
        GetQueueAttributesRequest request = new GetQueueAttributesRequest(queueUrl, List.of(QueueAttributeName.QueueArn.name()));
        GetQueueAttributesResult response = amazonSQS.getQueueAttributes(request);
        String queueArn = response.getAttributes().get(QueueAttributeName.QueueArn.name());
        IQueue queue = Queue.fromQueueAttributes(this, "GetQueue", QueueAttributes.builder()
                .queueArn(queueArn)
                .queueName(queueName)
                .build());

        importFileParserLambda.addEnvironment("UPLOADED_FOLDER", "uploaded");
        importFileParserLambda.addEnvironment("PARSED_FOLDER", "parsed");
        importFileParserLambda.addEnvironment("QUEUE_URL", queue.getQueueUrl());
        queue.grantSendMessages(importFileParserLambda);

        bucket.grantReadWrite(importFileParserLambda);

        bucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(importFileParserLambda),
                NotificationKeyFilter.builder().prefix("uploaded/").build());

        importFileParserLambda.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("s3:GetObject"))
                .resources(List.of(bucket.getBucketArn() + "/uploaded/*"))
                .build());
    }

    private MethodOptions methodOptions() {
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
