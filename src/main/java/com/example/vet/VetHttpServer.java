package com.example.vet;

import com.example.vet.config.EventBusConfig;
import com.example.vet.service.VetESService;
import com.example.vet.database.VetQueryParser;
import com.example.vet.validation.UserValidationHandler;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import java.util.Optional;

public class VetHttpServer extends AbstractVerticle {
    private final String INDEX = "test";
    private com.example.vet.service.reactivex.VetESService dbService;

    @Override
    public void start() {
        dbService = VetESService.createProxy(vertx.getDelegate(), EventBusConfig.VET_DB_QUEUE.address);
        final Router router = Router.router(vertx);
        final Handler<RoutingContext> userValidation = new UserValidationHandler();
        // Enable the body parser to we can get the form data and json documents in out context.
        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true));

        router.get("/users/search").handler(this::searchUser);
        router.get("/users").handler(this::fetchAllUser);
        router.get("/users/:id").handler(this::fetchUser);
        router.delete("/users/:id").handler(this::deleteUser);
        router.post("/users/upload").handler(this::uploadUsers);

        // User body data
        router.post("/users").handler(userValidation).handler(this::createUser);
        router.put("/users/:id").handler(userValidation).handler(this::updateUser);

        router.errorHandler(500, this::onError);

        vertx.createHttpServer().requestHandler(router).listen(8800);
    }

    private void onError(RoutingContext context) {
        Throwable failure = context.failure();
        if (failure != null) {
            failure.printStackTrace();
        }
    }

    private void searchUser(RoutingContext context) {
        int from = Optional.ofNullable(context.request().getParam("from"))
            .map(Integer::parseInt)
            .orElse(0);
        int size = Optional.ofNullable(context.request().getParam("size"))
            .map(Integer::parseInt)
            .orElse(10);

        dbService
            .rxFindAllUser(INDEX, context.getBodyAsJson(), from, size)
            .subscribe(
                listUser -> responseOk(context, listUser.encode()),
                error -> context.response().setStatusCode(400).end()
            );
    }

    private void fetchAllUser(RoutingContext context) {
        int from = Optional.ofNullable(context.request().getParam("from"))
            .map(Integer::parseInt)
            .orElse(0);
        int size = Optional.ofNullable(context.request().getParam("size"))
            .map(Integer::parseInt)
            .orElse(10);

        VetQueryParser.parseQuery(context.request().getParam("query"));
        Optional.ofNullable(context.request().getParam("query"))
            .map((query) -> {
                System.out.println(VetQueryParser.parseQuery(query));
                return dbService.rxFindAllUser(INDEX, VetQueryParser.parseQuery(query), from, size);
            })
            .orElseGet(() -> dbService.rxFetchAllUser(INDEX, from, size))
            .subscribe(
                listUser -> responseOk(context, listUser.encode()),
                context::fail
            );
    }

    private void fetchUser(RoutingContext context) {
        dbService
            .rxFetchUser(INDEX, context.request().getParam("id"), new JsonObject())
            .subscribe(
                user -> responseOk(context, user.encode()),
                context::fail,
                () -> responseNotFound(context)
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
        user.put("_id", id);
        dbService
            .rxIsUserExist(INDEX, id)
            .flatMapMaybe(userExisted -> {
                if (userExisted) {
                    return Maybe.empty();
                }
                return hashPasswordThenSaveUser(user).toMaybe();
            })
            .subscribe(
                updatedUser -> responseOk(context, user.encode()),
                context::fail,
                () -> responseNotFound(context)
            );
    }

    private Single<JsonObject> hashPasswordThenSaveUser(JsonObject user) {
        final String PASSWORD_FIELD = "password";
        if (!user.containsKey(PASSWORD_FIELD)) {
            return dbService.rxSave(INDEX, user);
        }
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "encode");
        return vertx.eventBus()
            .rxRequest(EventBusConfig.PASSWORD_ENCODER_QUEUE.address, user.getString(PASSWORD_FIELD), options)
            .flatMap(hashed -> {
                user.put(PASSWORD_FIELD, hashed.body());
                return dbService.rxSave(INDEX, user);
            });
    }

    private void deleteUser(RoutingContext context) {
        dbService
            .rxDeleteUser(INDEX, context.request().getParam("id"))
            .subscribe(
                () -> context.response().setStatusCode(204).end(),
                context::fail
            );
    }

    private void uploadUsers(RoutingContext context) {
        context.fileUploads().forEach(fileUpload -> {
            vertx.fileSystem()
                .rxReadFile(fileUpload.uploadedFileName())
                .flatMapCompletable(buffer -> dbService.rxBulkCreate(INDEX, new JsonArray(buffer.getDelegate())))
                .subscribe(
                    () -> context.response().setStatusCode(201).end(),
                    (error) -> context.response().setStatusCode(400).end()
                );
        });
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