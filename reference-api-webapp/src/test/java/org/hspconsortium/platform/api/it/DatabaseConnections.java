package org.hspconsortium.platform.api.it;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class DatabaseConnections {

    private static final List<String> SANDBOXES = List.of("r4sb0914");
    private static final List<String> FHIR_RESOURCES = List.of("/Patient", "/Observation", "/Practitioner", "/Encounter", "/AllergyIntolerance", "/Procedure", "/Immunization", "/MedicationRequest", "/Condition");
    private static final int NUMBER_FHIR_API_CALLS = 20;
    private static final int SANDBOX_COUNT = SANDBOXES.size();
    private static final int FHIR_RESOURCE_COUNT = FHIR_RESOURCES.size();
    private static final int SLEEP_TIME = 1200;
    private static final String API_CALL_PREFIX = "http://localhost:8070/";
    private static final String API_CALL_SUFFIX = "/open";
    private static final String HTTP_OK = "200 OK";

    @Test
    @Ignore
    public void callDbIntensiveFhirQueries() throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_FHIR_API_CALLS);
        var apiCallResults = threadPool.invokeAll(apiCallsList());
        apiCallResults.forEach(result -> {
            try {
                assertEquals(HTTP_OK , result.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        threadPool.shutdown();
    }

    private List<Callable<String>> apiCallsList() {
        List<Callable<String>> apiCalls = new ArrayList<>(SANDBOX_COUNT * FHIR_RESOURCE_COUNT);
        IntStream.range(0, SANDBOX_COUNT)
                 .forEach(sandboxIndex -> IntStream.range(0, FHIR_RESOURCE_COUNT)
                                                   .forEach(fhirResourceIndex -> {
                                                       apiCalls.add(apiCallTask(sandboxIndex, fhirResourceIndex));
                                                   }));
        return apiCalls;
    }

    private Callable<String> apiCallTask(int sandboxIndex, int fhirResourceIndex) {
        return () -> {
            Thread.sleep(SLEEP_TIME);
            RestTemplate singleUseRestTemplate = new RestTemplate();
            ResponseEntity<String> apiCallResponse = singleUseRestTemplate.getForEntity(fhirCallUrl(sandboxIndex, fhirResourceIndex), String.class);
            return apiCallResponse.getStatusCode()
                                  .toString();
        };
    }

    private String fhirCallUrl(int sandboxIndex, int fhirResourceIndex) {
        return API_CALL_PREFIX + SANDBOXES.get(sandboxIndex) + API_CALL_SUFFIX + FHIR_RESOURCES.get(fhirResourceIndex);
    }

}
