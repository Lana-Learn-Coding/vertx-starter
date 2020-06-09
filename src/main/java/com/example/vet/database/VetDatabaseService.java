package com.example.vet.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;

@ProxyGen
@VertxGen
public interface VetDatabaseService {
    @GenIgnore
    static VetDatabaseService create(MongoClient mongo, Handler<AsyncResult<VetDatabaseService>> readyHanler) {
        return new VetDatabaseServiceImpl(mongo, readyHanler);
    }

    @GenIgnore
    static com.example.vet.database.reactivex.VetDatabaseService createProxy(Vertx vertx, String address) {
        return new com.example.vet.database.reactivex.VetDatabaseService(new VetDatabaseServiceVertxEBProxy(vertx, address));
    }

    @Fluent
    VetDatabaseService fetchAllUser(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetDatabaseService fetchUser(JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetDatabaseService save(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetDatabaseService deleteUser(JsonObject query, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetDatabaseService isUserExist(JsonObject query, Handler<AsyncResult<Boolean>> resultHandler);
}
