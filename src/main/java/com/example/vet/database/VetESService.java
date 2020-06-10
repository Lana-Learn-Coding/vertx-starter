package com.example.vet.database;

import com.example.vet.database.impl.VetESServiceImpl;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.client.transport.TransportClient;

@ProxyGen
@VertxGen
public interface VetESService {
    @GenIgnore
    static VetESService create(TransportClient client, Handler<AsyncResult<VetESService>> readyHanler) {
        return new VetESServiceImpl(client, readyHanler);
    }

    @GenIgnore
    static com.example.vet.database.reactivex.VetESService createProxy(Vertx vertx, String address) {
        return new com.example.vet.database.reactivex.VetESService(new VetESServiceVertxEBProxy(vertx, address));
    }

    @Fluent
    VetESService findAllUser(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService findOneUser(JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetESService fetchAllUser(Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService fetchUser(String id, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetESService save(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetESService deleteUser(String id, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetESService isUserExist(String id, Handler<AsyncResult<Boolean>> resultHandler);
}
