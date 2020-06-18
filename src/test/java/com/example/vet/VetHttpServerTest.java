package com.example.vet;

import com.example.vet.model.User;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;

@RunWith(VertxUnitRunner.class)
public class VetHttpServerTest {
    Vertx vertx;
    int port = 8800;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(VetMain.class.getName(), context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void fetchAllUser_returnOK(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient()
            .get(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                async.complete();
            })
            .end();
    }

    @Test
    public void createUser_invalidData_return400(TestContext context) {
        Async async = context.async();
        User user = new User();
        vertx.createHttpClient()
            .post(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(400, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(JsonObject.mapFrom(user).toString())
            .end();
    }

    @Test
    public void createUser_validData_returnOk(TestContext context) {
        Async async = context.async();
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
        vertx.createHttpClient()
            .post(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(JsonObject.mapFrom(user).toString())
            .end();
    }

    @Test
    public void deleteUser_shouldReturnOk(TestContext context) {
        Async async = context.async();
        HttpClient client = vertx.createHttpClient();
        client
            .get(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                res.bodyHandler(buffer -> {
                    JsonArray bodyArray = new JsonArray(buffer);
                    context.assertNotEquals(0, bodyArray.size());

                    JsonObject item = bodyArray.getJsonObject(0);
                    context.assertNotNull(item.getString("_id"));
                    client
                        .delete(port, "localhost", "/users/" + item.getString("_id"))
                        .handler(delResponse -> {
                            context.assertEquals(204, delResponse.statusCode());
                            async.complete();
                        })
                        .end();
                });
            })
            .end();
    }
}
