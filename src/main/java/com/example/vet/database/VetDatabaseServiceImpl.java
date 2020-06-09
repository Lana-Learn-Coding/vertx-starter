package com.example.vet.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;


public class VetDatabaseServiceImpl implements VetDatabaseService {
    private final MongoClient mongo;
    private static final String USER_COLLECTION = "user";

    public VetDatabaseServiceImpl(MongoClient mongo, Handler<AsyncResult<VetDatabaseService>> readyHandler) {
        this.mongo = mongo;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public VetDatabaseService fetchAllUser(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
        mongo
            .rxFind(USER_COLLECTION, query)
            .map(listUser -> {
                JsonArray jsonArrayUser = new JsonArray();
                listUser.forEach(jsonArrayUser::add);
                return jsonArrayUser;
            })
            .subscribe(
                listUser -> resultHandler.handle(Future.succeededFuture(listUser)),
                error -> resultHandler.handle(Future.failedFuture(error))
            );
        return this;
    }

    @Override
    public VetDatabaseService fetchUser(JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
        mongo
            .rxFindOne(USER_COLLECTION, query, fields)
            .subscribe(
                user -> resultHandler.handle(Future.succeededFuture(user)),
                error -> resultHandler.handle(Future.failedFuture(error)),
                () -> resultHandler.handle(Future.succeededFuture(null))
            );
        return this;
    }

    @Override
    public VetDatabaseService save(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
        mongo
            .rxSave(USER_COLLECTION, user)
            .flatMap(id -> mongo.rxFindOne(USER_COLLECTION, new JsonObject().put("_id", id), new JsonObject()))
            .subscribe(
                saved -> resultHandler.handle(Future.succeededFuture(saved)),
                error -> resultHandler.handle(Future.failedFuture(error))
            );
        ;
        return this;
    }

    @Override
    public VetDatabaseService deleteUser(JsonObject query, Handler<AsyncResult<Void>> resultHandler) {
        mongo
            .rxFindOneAndDelete(USER_COLLECTION, query)
            .subscribe(
                deleted -> resultHandler.handle(Future.succeededFuture()),
                error -> resultHandler.handle(Future.failedFuture(error)),
                () -> resultHandler.handle(Future.succeededFuture())
            );
        return this;
    }

    @Override
    public VetDatabaseService isUserExist(JsonObject query, Handler<AsyncResult<Boolean>> resultHandler) {
        mongo
            .rxFindOne(USER_COLLECTION, query, new JsonObject().put("_id", 1))
            .subscribe(
                userExisted -> resultHandler.handle(Future.succeededFuture(true)),
                error -> resultHandler.handle(Future.failedFuture(error)),
                () -> resultHandler.handle(Future.succeededFuture(false))
            );
        return this;
    }
}
