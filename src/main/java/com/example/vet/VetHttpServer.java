package com.example.vet;

import com.example.vet.config.EventBusConfig;
import com.example.vet.database.VetQueryParser;
import com.example.vet.service.VetESService;
import com.example.vet.validation.UserValidationHandler;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class VetHttpServer extends AbstractVerticle {
    private final String INDEX = "test";
    private final String TYPE = "user";
    private com.example.vet.service.reactivex.VetESService dbService;

    @Override
    public void start() {
        dbService = VetESService.createProxy(vertx.getDelegate(), EventBusConfig.VET_DB_QUEUE.address);
        final Router router = Router.router(vertx);
        final Handler<RoutingContext> userValidation = new UserValidationHandler();
        // Enable the body parser to we can get the form data and json documents in out context.
        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true));

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

    private void fetchAllUser(RoutingContext context) {
        HttpServerRequest request = context.request();
        JsonObject search = this.setIndexAndType(new JsonObject());
        search.put("query", request.getParam("query"));
        search.put("size", request.getParam("size"));
        search.put("from", request.getParam("from"));
        search.put("query", VetQueryParser.parseQuery(search.getString("query")));

        dbService
            .rxFindAllUser(search)
            .subscribe(
                listUser -> this.responseOk(context, listUser.encode()),
                context::fail
            );
    }

    private void fetchUser(RoutingContext context) {
        JsonObject identify = this.setIndexAndType(new JsonObject())
            .put("id", context.request().getParam("id"));
        dbService
            .rxFetchUser(identify)
            .subscribe(
                user -> this.responseOk(context, user.encode()), context::fail,
                () -> this.responseNotFound(context)
            );
    }

    private void createUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        // ignore the _id field
        user.remove("_id");
        hashPasswordThenSaveUser(user).subscribe(
            createdUser -> this.responseOk(context, createdUser.encode()),
            context::fail
        );
    }

    private void updateUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        final String id = context.request().getParam("id");
        final JsonObject identify = this.setIndexAndType(new JsonObject()).put("id", id);
        user.put("_id", id);

        dbService
            .rxIsUserExist(identify)
            .flatMapMaybe(userExisted -> {
                if (userExisted) {
                    return Maybe.empty();
                }
                return this.hashPasswordThenSaveUser(user).toMaybe();
            })
            .subscribe(
                updatedUser -> this.responseOk(context, user.encode()),
                context::fail,
                () -> this.responseNotFound(context)
            );
    }

    private Single<JsonObject> hashPasswordThenSaveUser(JsonObject user) {
        final String PASSWORD_FIELD = "password";
        JsonObject modification = this.setIndexAndType(new JsonObject()).put("modification", user);
        if (!user.containsKey(PASSWORD_FIELD)) {
            return dbService.rxSave(INDEX, modification);
        }
        DeliveryOptions options = new DeliveryOptions().addHeader("action", "encode");
        return vertx.eventBus()
            .rxRequest(EventBusConfig.PASSWORD_ENCODER_QUEUE.address, user.getString(PASSWORD_FIELD), options)
            .flatMap(hashed -> {
                user.put(PASSWORD_FIELD, hashed.body());
                return dbService.rxSave(INDEX, modification);
            });
    }

    private void deleteUser(RoutingContext context) {
        JsonObject identify = this.setIndexAndType(new JsonObject())
            .put("id", context.request().getParam("id"));
        dbService
            .rxDeleteUser(identify)
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

    private JsonObject setIndexAndType(JsonObject object) {
        return object
            .put("type", TYPE)
            .put("index", INDEX);
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
