package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;

public class ImportFileParserHandler implements RequestHandler<S3Event, String> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private final String uploadedFolder = System.getenv("UPLOADED_FOLDER");
    private final String parsedFolder = System.getenv("PARSED_FOLDER");


    @Override
    public String handleRequest(S3Event s3event, Context context) {
        LambdaLogger logger = context.getLogger();
        s3event.getRecords().forEach(record -> {
            String bucketName = record.getS3().getBucket().getName();
            String objectKey = record.getS3().getObject().getKey();
            logger.log("Bucket name: " + bucketName + "; Key: " + objectKey, LogLevel.INFO);

            try (S3Object s3Object = s3Client.getObject(bucketName, objectKey);
                 InputStreamReader reader = new InputStreamReader(s3Object.getObjectContent());
                 CSVReader csvReader = new CSVReader(reader)) {
                logger.log("Start parsing");

                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    logger.log("Record: " + String.join(", ", nextLine), LogLevel.INFO);
                }
            } catch (Exception e) {
                logger.log("Error parsing: " + e.getMessage(), LogLevel.ERROR);
            }

            String newKey = objectKey.replace(uploadedFolder, parsedFolder);
            logger.log("Upload folder: " + objectKey, LogLevel.INFO);
            logger.log("Parsed folder: " + newKey, LogLevel.INFO);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, objectKey, bucketName, newKey);
            s3Client.copyObject(copyObjectRequest);

            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, objectKey);

            // Удалить файл из папки uploaded
            s3Client.deleteObject(deleteObjectRequest);
        });
        logger.log("Parsing completed", LogLevel.INFO);

        return "Processed " + s3event.getRecords().size() + " records.";
    }
}
