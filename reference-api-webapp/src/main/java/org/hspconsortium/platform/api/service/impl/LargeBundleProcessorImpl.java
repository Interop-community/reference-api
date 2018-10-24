package org.hspconsortium.platform.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hspconsortium.platform.api.service.LargeBundleProcessor;
import org.springframework.boot.json.JsonJsonParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LargeBundleProcessorImpl implements LargeBundleProcessor {

    @Override
    public void processLargeBundle(String bundleString, String targetUrl, String notificationEmail) {
        LargeBundleProcessorImpl.processLargeBundleInternal(bundleString, targetUrl, notificationEmail);
    }

    // TODO: Make the process happen on a seperate thread.
//    private  void processOnNewThread(String bundleString, String targetUrl, String notificationEmail){
//        new Thread(() -> {
//            LargeBundleProcessorImpl.processLargeBundleInternal(bundleString, targetUrl, notificationEmail);
//            System.out.println("Thread is complete.");
//        }).start();
//    }

    private static void processLargeBundleInternal(String bundleString, String targetUrl, String notificationEmail){

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode bundleJsonNode;

        try {
            bundleJsonNode = objectMapper.readTree(bundleString);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // TODO: Create a bunch more smaller JsonNode objects that represent the bundles that need to be posted


        List<JsonNode> processedBundles = new ArrayList<>();

        for (JsonNode currentBundleJsonNode : processedBundles) {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> postedBundleResultString = restTemplate.postForEntity(targetUrl, headers, String.class);

            System.out.println(postedBundleResultString.getBody());
        }


        // TODO: Send an email

    }

}
