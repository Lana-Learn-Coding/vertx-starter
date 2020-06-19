package com.example.vet;

import com.example.vet.model.User;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Calendar;
import java.util.Date;

@FixMethodOrder(MethodSorters.JVM)
@RunWith(VertxUnitRunner.class)
public class VetHttpServerTest {
    static HttpClient httpClient;
    static Vertx vertx;
    static final int port = 8800;
    static JsonObject testUser;

    @BeforeClass
    public static void beforeClass(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(VetMain.class.getName(), context.asyncAssertSuccess());
        httpClient = vertx.createHttpClient();
    }

    @AfterClass
    public static void afterClass(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void fetchAllUser_returnOK(TestContext context) {
        Async async = context.async();
        httpClient
            .get(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                async.complete();
            })
            .end();
    }

    @Test
    public void createUser_validData_return200(TestContext context) {
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
        httpClient
            .post(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                res.bodyHandler(body -> {
                    testUser = body.toJsonObject();
                });
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(JsonObject.mapFrom(user).toString())
            .end();
    }

    @Test
    public void createUser_invalidData_return400(TestContext context) {
        Async async = context.async();
        httpClient
            .post(port, "localhost", "/users")
            .handler(res -> {
                context.assertEquals(400, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(JsonObject.mapFrom(new User()).toString())
            .end();
    }

    @Test
    public void updateUser_invalidData_return400(TestContext context) {
        Async async = context.async();
        httpClient
            .put(port, "localhost", "/users/" + testUser.getString("_id"))
            .handler(res -> {
                context.assertEquals(400, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(JsonObject.mapFrom(new User()).toString())
            .end();
    }

    @Test
    public void updateUser_validData_return200(TestContext context) throws InterruptedException {
        Thread.sleep(1000); // wait for es to index
        Async async = context.async();
        JsonObject user = new JsonObject(testUser.getMap());
        user.put("name", "Test update");
        httpClient
            .put(port, "localhost", "/users/" + testUser.getString("_id"))
            .handler(res -> {
                context.assertEquals(200, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(user.toString())
            .end();
    }

    @Test
    public void deleteUser_shouldReturnOk(TestContext context) {
        Async async = context.async();
        context.assertNotNull(testUser.getString("_id"));
        httpClient
            .delete(port, "localhost", "/users/" + testUser.getString("_id"))
            .handler(delResponse -> {
                context.assertEquals(204, delResponse.statusCode());
                async.complete();
            })
            .end();
    }

    @Test
    public void updateUser_nonExist_return404(TestContext context) throws InterruptedException {
        Thread.sleep(1000); // wait for es to index
        Async async = context.async();
        JsonObject user = new JsonObject(testUser.getMap());
        user.put("name", "Test update non exist");
        httpClient
            .put(port, "localhost", "/users/" + testUser.getString("_id"))
            .handler(res -> {
                context.assertEquals(404, res.statusCode());
                async.complete();
            })
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .setChunked(true)
            .write(user.toString())
            .end();
    }
}
