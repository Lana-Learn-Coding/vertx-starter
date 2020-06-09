package com.example.vet.http;

import com.example.vet.QueueAddresses;
import com.example.vet.database.VetDatabaseService;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class VetHttpServer extends AbstractVerticle {
    private com.example.vet.database.reactivex.VetDatabaseService dbService;

    @Override
    public void start() {
        dbService = VetDatabaseService.createProxy(vertx.getDelegate(), QueueAddresses.VET_DB_QUEUE.address);
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
        dbService
            .rxFetchAllUser(new JsonObject())
            .subscribe(
                listUser -> responseOk(context, listUser.encode()),
                context::fail
            );
    }

    private void fetchUser(RoutingContext context) {
        dbService
            .rxFetchUser(new JsonObject().put("_id", context.request().getParam("id")), new JsonObject())
            .subscribe(
                user -> {
                    if (user == null) {
                        responseNotFound(context);
                        return;
                    }
                    responseOk(context, user.encode());
                },
                context::fail
            );
    }

    private void createUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        // ignore the _id field
        user.remove("_id");
        hashPasswordThenSaveUser(user).subscribe(
            createdUser -> responseOk(context, createdUser.encode()),
            context::fail
        );
    }

    private void updateUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        final String id = context.request().getParam("id");
        dbService
            .rxIsUserExist(new JsonObject().put("_id", id))
            .flatMapMaybe(userExisted -> userExisted ? dbService.rxSave(user).toMaybe() : Maybe.empty())
            .subscribe(
                updatedUser -> responseOk(context, user.encode()),
                context::fail,
                () -> responseNotFound(context)
            );
    }

    private Single<JsonObject> hashPasswordThenSaveUser(JsonObject user) {
        final String PASSWORD_FIELD = "password";
        if (!user.containsKey(PASSWORD_FIELD)) {
            return dbService.rxSave(user);
        }
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "encode");
        return vertx.eventBus()
            .rxRequest(QueueAddresses.PASSWORD_ENCODER_QUEUE.address, user.getString(PASSWORD_FIELD), options)
            .flatMap(hashed -> {
                user.put(PASSWORD_FIELD, hashed);
                return dbService.rxSave(user);
            });
    }

    private void deleteUser(RoutingContext context) {
        dbService
            .rxDeleteUser(new JsonObject().put("_id", context.request().getParam("id")))
            .subscribe(
                () -> context.response().setStatusCode(204).end(),
                context::fail
            );
    }

    private void responseOk(RoutingContext context, String data) {
        context.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(data);
    }

    private void responseNotFound(RoutingContext context) {
        context.response().setStatusCode(404).end();
    }
}