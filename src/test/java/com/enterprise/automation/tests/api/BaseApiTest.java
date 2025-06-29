package com.enterprise.automation.tests.api;

import com.enterprise.automation.config.FrameworkConfig;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.specification.RequestSpecification;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public abstract class BaseApiTest {

    @Autowired
    protected FrameworkConfig.Api apiConfig;

    protected RequestSpecification spec;

    @BeforeEach
    public void initSpec() {
        // Configure timeout for the HTTP client
        int timeoutMillis = apiConfig.getTimeout() * 1000;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMillis)
                .setSocketTimeout(timeoutMillis)
                .setConnectionRequestTimeout(timeoutMillis)
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();

        RestAssured.config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .httpClientFactory(() -> httpClient)
                );

        // Build reusable request spec
        spec = RestAssured.given()
                .baseUri(apiConfig.getBaseUrl())
                .headers(apiConfig.getHeaders())
                .relaxedHTTPSValidation()
                .log().all(); // Optional: to log request details
    }
}
