package com.myorg.controller;


import com.myorg.service.BffService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class BffController {

    private final Dotenv dotenv;
    private final BffService service;

    @Autowired
    public BffController(Dotenv dotenv, BffService service) {
        this.dotenv = dotenv;
        this.service = service;
    }

    @RequestMapping("/{recipientServiceName}")
    public ResponseEntity<String> proxyRequest(
            @PathVariable String recipientServiceName,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader(value = "x-http-method-override", required = false) String methodOverride,
            @RequestBody(required = false) String body
    ) {
        String recipientURL = dotenv.get(recipientServiceName.toUpperCase());

        if (recipientURL == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Cannot process request");
        }

        return service.processData(recipientURL, methodOverride, queryParams, body);
    }
}
