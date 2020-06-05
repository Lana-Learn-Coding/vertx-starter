package com.example.vet;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class VetServer extends AbstractVerticle {
    @Override
    public void start() {
        final Router router = Router.router(vertx);
        final MongoClient mongo = MongoClient.create(vertx, new JsonObject().put("db_name", "test"));
        // Enable the body parser to we can get the form data and json documents in out context.
        router.route().handler(BodyHandler.create());
        router.get("/").handler(context -> {
            context.response()
                    .setStatusCode(304)
                    .putHeader(HttpHeaders.LOCATION, "http://" + context.request().host() + "/users")
                    .end();
        });

        router.get("/users").handler(context -> {
            mongo.find("user", new JsonObject(), lookup -> {
                if (lookup.failed()) {
                    context.fail(lookup.cause());
                }
                JsonArray listUser = new JsonArray();
                for (JsonObject user : lookup.result()) {
                    listUser.add(user);
                }
                context.response()
                        .setStatusCode(200)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .end(listUser.encode());
            });
        });

        router.get("/users/:id").handler(context -> {
            mongo.findOne("user", new JsonObject().put("_id", context.request().getParam("id")), new JsonObject(), lookup -> {
                if (lookup.result() == null) {
                    context.response().setStatusCode(404).end();
                } else {
                    context.response()
                            .setStatusCode(200)
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .end(lookup.result().encode());
                }
            });
        });

        router.post("/users").handler(context -> {
            JsonObject user = context.getBodyAsJson();
            mongo.insert("user", user, lookup -> {
                if (lookup.failed()) {
                    context.fail(lookup.cause());
                } else {
                    context.response().setStatusCode(201).end();
                }
            });
        });

        router.put("/users/:id").handler(context -> {
            mongo.findOne("user", new JsonObject().put("_id", context.request().getParam("id")), new JsonObject(), lookup -> {
                if (lookup.result() == null) {
                    context.response().setStatusCode(404).end();
                } else {
                    JsonObject user = context.getBodyAsJson();
                    mongo.findOneAndReplace("user", new JsonObject().put("_id", context.request().getParam("id")), user, res -> {
                        if (res.failed()) {
                            context.fail(res.cause());
                        } else {
                            context.response()
                                    .setStatusCode(200)
                                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                                    .end(user.encode());
                        }
                    });
                }
            });
        });

        router.delete("/users/:id").handler(context -> {
            mongo.findOneAndDelete("user", new JsonObject().put("_id", context.request().getParam("id")), lookup -> {
                context.response().setStatusCode(204).end();
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