package com.example.vet.http;

import com.example.vet.database.VetDatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class VetHttpServer extends AbstractVerticle {
    private final String VET_DB_QUEUE = "dbclient.queue";
    private VetDatabaseService dbService;

    @Override
    public void start() {
        dbService = VetDatabaseService.createProxy(vertx, VET_DB_QUEUE);
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
            } else {
                context.response()
                        .setStatusCode(200)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .end(result.result().encode());
            }
        });
    }

    private void fetchUser(RoutingContext context) {
        dbService.fetchUser(new JsonObject().put("_id", context.request().getParam("id")), new JsonObject(), result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                JsonObject user = result.result();
                if (user == null) {
                    context.response().setStatusCode(404).end();
                } else {
                    context.response()
                            .setStatusCode(200)
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .end(user.encode());
                }
            }
        });
    }

    private void createUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        if (user.containsKey("_id")) {
            user.remove("_id");
        }
        saveUser(context, user);
    }

    private void updateUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        final String id = context.request().getParam("id");
        dbService.isUserExist(new JsonObject().put("_id", id), lookup -> {
            if (lookup.failed()) {
                context.fail(lookup.cause());
            } else {
                boolean userExisted = lookup.result();
                if (userExisted) {
                    saveUser(context, user);
                } else {
                    context.response().setStatusCode(404);
                }
            }
        });
    }

    private void saveUser(RoutingContext context, JsonObject user) {
        dbService.save(user, result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                context.response()
                        .setStatusCode(200)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .end(result.result().encode());
            }
        });
    }

    private void deleteUser(RoutingContext context) {
        dbService.deleteUser(new JsonObject().put("_id", context.request().getParam("id")), result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else {
                context.response().setStatusCode(204).end();
            }
        });
    }
}