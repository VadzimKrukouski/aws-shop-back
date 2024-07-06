package com.myorg.handlers;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.myorg.utils.APIGatewayUtils;

import java.net.URL;
import java.util.Date;

public class ImportProductsFileHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private final String bucketName = System.getenv("BUCKET_NAME");
    private final String uploadedFolder = System.getenv("UPLOADED_FOLDER");

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            String fileName = event.getQueryStringParameters().get("name");
            logger.log("File name: " + fileName, LogLevel.INFO);

            if (fileName == null || fileName.isEmpty()) {
                return APIGatewayUtils.createInvalidDataResponse();
            }

            String objectKey = uploadedFolder +"/" + fileName;
            Date expiration = new Date();
            long expirationTime = expiration.getTime();
            expirationTime += 1000*60*60*12;
            expiration.setTime(expirationTime);

            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiration);

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            logger.log("url - " + url.toString(), LogLevel.INFO);

            return APIGatewayUtils.createOkResponse(url.toString());
        } catch (Exception e) {
            logger.log(e.getMessage(), LogLevel.ERROR);
            return APIGatewayUtils.createErrorResponse(e.getMessage());
        }
    }
}
