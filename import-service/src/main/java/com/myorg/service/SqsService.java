package com.myorg.service;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SqsService {

    private final AmazonSQS sqsClient;

    public SqsService() {
        this.sqsClient = AmazonSQSClientBuilder.defaultClient();
    }

    public void sendMessageToQueue(String message, String queueUrl) {
        SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(message);

        sqsClient.sendMessage(request);
    }
}
