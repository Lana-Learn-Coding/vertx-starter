package com.example.vet;

import com.example.vet.config.ActiveMQConfig;
import com.example.vet.config.EventBusConfig;
import com.example.vet.database.VetQueryParser;
import com.example.vet.service.VetESService;
import com.example.vet.validation.UserValidationHandler;
import io.reactivex.Maybe;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class VetHttpServer extends AbstractVerticle {
    private final String INDEX = "test";
    private final String TYPE = "user";
    private com.example.vet.service.reactivex.VetESService dbService;
    private Session session;
    private Connection connection;
    private MessageProducer messageProducer;

    @Override
    public void start() throws Exception {
        dbService = VetESService.createProxy(vertx.getDelegate(), EventBusConfig.VET_DB_QUEUE.address);
        ConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMQConfig.BROKER_URL.address);
        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(ActiveMQConfig.USER_CREATION_QUEUE.address);
        messageProducer = session.createProducer(destination);
        connection.start();

        final Router router = Router.router(vertx);
        final Handler<RoutingContext> userValidation = new UserValidationHandler();
        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true));
        router.errorHandler(500, this::onError);

        router.get("/users").handler(this::fetchAllUser);
        router.get("/users/:id").handler(this::fetchUser);
        router.delete("/users/:id").handler(this::deleteUser);
        router.post("/users/upload").handler(this::uploadUsers);

        router.post("/users").handler(userValidation).handler(this::createUser);
        router.put("/users/:id").handler(userValidation).handler(this::updateUser);

        vertx.createHttpServer().requestHandler(router).listen(8800);
    }

    @Override
    public void stop() throws Exception {
        session.close();
        connection.close();
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
                user -> this.responseOk(context, user.encode()),
                context::fail,
                () -> this.responseNotFound(context)
            );
    }

    private void createUser(RoutingContext context) {
        final JsonObject user = context.getBodyAsJson();
        // ignore the _id field
        user.remove("_id");
        hashPasswordThenSaveUser(user)
            .subscribe(
                (saved) -> responseOk(context, saved.toString()),
                context::fail,
                () -> responseOk(context, "We are on it")
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
                if (!userExisted) {
                    return Maybe.empty();
                }
                return this.hashPasswordThenSaveUser(user);
            })
            .subscribe(
                updatedUser -> this.responseOk(context, user.encode()),
                context::fail,
                () -> this.responseNotFound(context)
            );
    }

    private Maybe<JsonObject> hashPasswordThenSaveUser(JsonObject user) {
        JsonObject modification = this.setIndexAndType(new JsonObject()).put("modification", user);
        if (!user.containsKey("password")) {
            return dbService.rxSave(modification).toMaybe();
        }
        // Enqueue as our hash algorithm take a lot of time
        return Maybe.fromAction(() -> {
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText(modification.toString());
            messageProducer.send(textMessage);
        });
    }

    private void deleteUser(RoutingContext context) {
        JsonObject identify = this.setIndexAndType(new JsonObject())
            .put("id", context.request().getParam("id"));
        dbService
            .rxDeleteUser(identify)
            .subscribe(() -> this.responseNoContent(context), context::fail);
    }

    private void uploadUsers(RoutingContext context) {
        context.fileUploads().forEach(fileUpload -> {
            vertx.fileSystem()
                .rxReadFile(fileUpload.uploadedFileName())
                .flatMapCompletable(buffer -> {
                    JsonObject modification = this.setIndexAndType(new JsonObject())
                        .put("modification", new JsonArray(buffer.getDelegate()));
                    return dbService.rxBulkCreate(modification);
                })
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

    private void responseNoContent(RoutingContext context) {
        context.response().setStatusCode(204).end();
    }
}
