package com.example.vet;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class VetHttpServer extends AbstractVerticle {
    private final String dbClientQueue = "dbclient.queue";

    @Override
    public void start() {
        final Router router = Router.router(vertx);
        // Enable the body parser to we can get the form data and json documents in out context.
        router.route().handler(BodyHandler.create());

        router.get("/users").handler(context -> {
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-all-user");
            vertx.eventBus().<JsonArray>request(dbClientQueue, new JsonObject(), options, reply -> {
                if (reply.failed()) {
                    context.fail(reply.cause());
                } else {
                    context.response()
                            .setStatusCode(200)
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .end(reply.result().body().encode());
                }
            });
        });

        router.get("/users/:id").handler(context -> {
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-user");
            vertx.eventBus().<JsonObject>request(dbClientQueue, new JsonObject(), options, reply -> {
                if (reply.failed()) {
                    context.fail(reply.cause());
                } else {
                    JsonObject user = reply.result().body();
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
        });

        router.post("/users").handler(context -> {
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "create-user");
            JsonObject user = context.getBodyAsJson();
            vertx.eventBus().request(dbClientQueue, user, options, reply -> {
                if (reply.failed()) {
                    context.fail(reply.cause());
                } else {
                    context.response().setStatusCode(201).end();
                }
            });
        });

        router.put("/users/:id").handler(context -> {
            JsonObject user = context.getBodyAsJson();
            user.put("_id", context.request().getParam("id"));
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "update-user");
            vertx.eventBus().<JsonObject>request(dbClientQueue, user, options, reply -> {
                if (reply.failed()) {
                    context.fail(reply.cause());
                } else {
                    JsonObject changedUser = reply.result().body();
                    if (changedUser == null) {
                        context.response().setStatusCode(404).end();
                    } else {
                        context.response()
                                .setStatusCode(200)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                .end(changedUser.encode());
                    }
                }
            });
        });

        router.delete("/users/:id").handler(context -> {
            DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-user");
            vertx.eventBus().request(dbClientQueue, new JsonObject().put("_id", context.request().getParam("id")), options, reply -> {
                if (reply.failed()) {
                    context.fail(reply.cause());
                } else {
                    context.response().setStatusCode(204).end();
                }
            });
        });


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
}