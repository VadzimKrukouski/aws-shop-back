package com.myorg.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ImportFileParserHandlerTest {

    private ImportFileParserHandler handler;
    private AmazonS3 s3Client;
    private Context context;

    @BeforeEach
    public void setUp() {
        s3Client = mock(AmazonS3.class);
        handler = new ImportFileParserHandler();
        context = mock(Context.class);
    }

    @Test
    public void testHandleRequest() {
        S3Event s3Event = mock(S3Event.class);
        S3Object s3Object = mock(S3Object.class);
        InputStream inputStream = new ByteArrayInputStream("id,title,description,price\n1,Product A,Desc,10.99\n".getBytes());
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        when(s3Client.getObject(anyString(), anyString())).thenReturn(s3Object);

        String result = handler.handleRequest(s3Event, context);

        verify(s3Client, times(1)).getObject(anyString(), anyString());
        verify(s3Client, times(1)).copyObject(any(CopyObjectRequest.class));
        verify(s3Client, times(1)).deleteObject(anyString(), anyString());

        assertEquals("Processed 1 records.", result);
    }
}