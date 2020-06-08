package com.example.vet.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;


public class VetDatabaseServiceImpl implements VetDatabaseService {
    private final MongoClient mongo;
    private static final String USER_COLLECTION = "user";

    public VetDatabaseServiceImpl(MongoClient mongo, Handler<AsyncResult<VetDatabaseService>> readyHandler) {
        this.mongo = mongo;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public VetDatabaseService fetchAllUser(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
        mongo.find(USER_COLLECTION, query, lookup -> {
            if (lookup.failed()) {
                resultHandler.handle(Future.failedFuture(lookup.cause()));
            } else {
                JsonArray listUser = new JsonArray();
                lookup.result().forEach(listUser::add);
                resultHandler.handle(Future.succeededFuture(listUser));
            }
        });
        return this;
    }

    @Override
    public VetDatabaseService fetchUser(JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
        mongo.findOne(USER_COLLECTION, query, fields, resultHandler);
        return this;
    }

    @Override
    public VetDatabaseService save(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
        mongo.save(USER_COLLECTION, user, lookup -> {
            if (lookup.failed()) {
                resultHandler.handle(Future.failedFuture(lookup.cause()));
            } else {
                // fetch the saved user
                fetchUser(new JsonObject().put("_id", lookup.result()), new JsonObject(), resultHandler);
            }
        });
        return this;
    }

    @Override
    public VetDatabaseService deleteUser(JsonObject query, Handler<AsyncResult<Void>> resultHandler) {
        mongo.findOneAndDelete(USER_COLLECTION, query, lookup -> {
            if (lookup.failed()) {
                resultHandler.handle(Future.failedFuture(lookup.cause()));
            } else {
                resultHandler.handle(Future.succeededFuture());
            }
        });
        return this;
    }

    @Override
    public VetDatabaseService isUserExist(JsonObject query, Handler<AsyncResult<Boolean>> resultHandler) {
        mongo.findOne(USER_COLLECTION, query, new JsonObject().put("_id", 1), lookup -> {
            if (lookup.failed()) {
                resultHandler.handle(Future.failedFuture(lookup.cause()));
            } else {
                boolean isExisted = lookup.result() != null;
                resultHandler.handle(Future.succeededFuture(isExisted));
            }
        });
        return this;
    }
}
