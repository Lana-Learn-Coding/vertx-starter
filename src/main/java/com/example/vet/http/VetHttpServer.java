package com.example.vet.http;

import com.example.vet.QueueAddresses;
import com.example.vet.database.VetDatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class VetHttpServer extends AbstractVerticle {
    private VetDatabaseService dbService;

    @Override
    public void start() {
        dbService = VetDatabaseService.createProxy(vertx, QueueAddresses.VET_DB_QUEUE.address);
        final Router router = Router.router(vertx);
        // Enable the body parser to we can get the form data and json documents in out context.
        router.route().handler(BodyHandler.create());

        router.get("/users").handler(this::fetchAllUser);
        router.post("/users").handler(this::createUser);
        router.get("/users/:id").handler(this::fetchUser);
        router.put("/users/:id").handler(this::updateUser);
        router.delete("/users/:id").handler(this::deleteUser);

        // Print error stack trace to console
        router.errorHandler(500, rc -> {
            System.err.println("Handling failure");
            Throwable failure = rc.failure();
            if (failure != null) {
                failure.printStackTrace();
            }
        });

        vertx.createHttpServer().requestHandler(router).listen(8000);
    }

    private void fetchAllUser(RoutingContext context) {
        dbService.fetchAllUser(new JsonObject(), result -> {
            if (result.failed()) {
                context.fail(result.cause());
                return;
            }

            context.response()
                    .setStatusCode(200)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(result.result().encode());
        });
    }

    private void fetchUser(RoutingContext context) {
        dbService.fetchUser(new JsonObject().put("_id", context.request().getParam("id")), new JsonObject(), result -> {
            if (result.failed()) {
                context.fail(result.cause());
                return;
            }

            JsonObject user = result.result();
            if (user == null) {
                context.response().setStatusCode(404).end();
                return;
            }

            context.response()
                    .setStatusCode(200)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(user.encode());
        });
    }

    private void createUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        // ignore the _id field
        user.remove("_id");
        hashPasswordThenSaveUserThenResponse(context, user);
    }

    private void updateUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        final String id = context.request().getParam("id");
        dbService.isUserExist(new JsonObject().put("_id", id), lookup -> {
            if (lookup.failed()) {
                context.fail(lookup.cause());
                return;
            }

            boolean userExisted = lookup.result();
            if (userExisted) {
                hashPasswordThenSaveUserThenResponse(context, user);
            } else {
                context.response().setStatusCode(404);
            }
        });
    }

    private void hashPasswordThenSaveUserThenResponse(RoutingContext context, JsonObject user) {
        if (!user.containsKey("password")) {
            saveUserThenResponse(context, user);
            return;
        }
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "encode");
        vertx.eventBus().request(QueueAddresses.PASSWORD_ENCODER_QUEUE.address, user.getString("password"), options, reply -> {
            if (reply.failed()) {
                context.fail(reply.cause());
                return;
            }
            user.put("password", reply.result().body());
            saveUserThenResponse(context, user);
        });
    }

    private void saveUserThenResponse(RoutingContext context, JsonObject user) {
        dbService.save(user, result -> {
            if (result.failed()) {
                context.fail(result.cause());
                return;
            }
            context.response()
                    .setStatusCode(200)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(result.result().encode());
        });
    }

    private void deleteUser(RoutingContext context) {
        dbService.deleteUser(new JsonObject().put("_id", context.request().getParam("id")), result -> {
            if (result.failed()) {
                context.fail(result.cause());
                return;
            }
            context.response().setStatusCode(204).end();
        });
    }
}