package com.example.vet.pact;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.example.vet.model.User;
import io.vertx.core.json.JsonObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactConsumerContactTest {
    static User validUser;
    static User invalidUser;

    @BeforeClass
    public static void prepareData() {
        User user = new User();
        user.setName("Test Name");
        user.setActivated(true);
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.YEAR, 2);
        Date expiredDate = calendar.getTime();
        user.setActivatedDate(new Date());
        user.setExpirationDate(expiredDate);
        user.setEmail("test@test.test");

        validUser = user;
        invalidUser = new User();
    }

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("My Provider", "localhost", 8080, this);

    @Pact(provider = "My Provider", consumer = "Some Consumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        return builder
            .given("test GET")
            .uponReceiving("GET REQUEST")
            .path("/users")
            .method("GET")
            .willRespondWith()
            .status(200)
            .given("test POST invalid")
            .uponReceiving("POST REQUEST")
            .method("POST")
            .headers(headers)
            .body(JsonObject.mapFrom(invalidUser).toString())
            .path("/users")
            .willRespondWith()
            .status(400)
            .given("test POST")
            .uponReceiving("POST REQUEST")
            .method("POST")
            .headers(headers)
            .body(JsonObject.mapFrom(validUser).toString())
            .path("/users")
            .willRespondWith()
            .status(200)
            .toPact();
    }

    @Test
    @PactVerification("My Provider")
    public void createPact_passAll() {
        getAllUser_returnOk();
        postUser_invalidUser_return400();
        postUser_validUser_returnOk();
    }

    private void getAllUser_returnOk() {
        HttpResponse<String> response = Unirest.get("http://localhost:8080/users").asString();
        assertEquals(200, response.getStatus());
    }

    private void postUser_invalidUser_return400() {
        HttpResponse<String> response = Unirest
            .post("http://localhost:8080/users")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(JsonObject.mapFrom(invalidUser).toString())
            .asString();
        assertEquals(400, response.getStatus());
    }

    private void postUser_validUser_returnOk() {
        HttpResponse<String> response = Unirest
            .post("http://localhost:8080/users")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(JsonObject.mapFrom(validUser).toString())
            .asString();
        assertEquals(200, response.getStatus());
    }
}
