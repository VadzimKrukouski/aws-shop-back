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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.service.SqsService;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ImportFileParserHandler implements RequestHandler<S3Event, String> {

    private static final Map<Integer, String> HEADER_DESCRIPTION = Map.of(
            0, "id",
            1, "title",
            2, "description",
            3, "price",
            4, "count"
    );
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private final String uploadedFolder = System.getenv("UPLOADED_FOLDER");
    private final String parsedFolder = System.getenv("PARSED_FOLDER");
    private final String queueUrl = System.getenv("QUEUE_URL");
    private final SqsService sqsService;

    public ImportFileParserHandler() {
        this.sqsService = new SqsService();
    }

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
                boolean isHeaderCsv = true;
                while ((nextLine = csvReader.readNext()) != null) {
                    if (isHeaderCsv) {
                        logger.log("csv header: " + String.join(", ", nextLine), LogLevel.INFO);
                        isHeaderCsv = false;
                        continue;
                    }
                    logger.log("Record: " + String.join(", ", nextLine), LogLevel.INFO);
                    String json = convertRecordToJson(nextLine);
                    logger.log("JSON: " + json, LogLevel.INFO);
                    sqsService.sendMessageToQueue(json, queueUrl);
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

    private String convertRecordToJson(String[] record) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < record.length; i++) {
            if (i == 3) {
                map.put(HEADER_DESCRIPTION.get(i), Double.valueOf(record[i]));
            }
            if (i == 4) {
                map.put(HEADER_DESCRIPTION.get(i), Integer.valueOf(record[i]));
            }
            map.put(HEADER_DESCRIPTION.get(i), record[i]);
        }

        return objectMapper.writeValueAsString(map);
    }
}
