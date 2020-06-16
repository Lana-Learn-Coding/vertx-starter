package com.example.vet.service;

import com.example.vet.service.impl.VetESServiceImpl;
import io.vertx.codegen.annotations.*;
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
    static com.example.vet.service.reactivex.VetESService createProxy(Vertx vertx, String address) {
        return new com.example.vet.service.reactivex.VetESService(new VetESServiceVertxEBProxy(vertx, address));
    }

    @Fluent
    VetESService findAllUser(String index, JsonObject query, int from, int size, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService fetchAllUser(String index, int from, int size, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService fetchUser(String index, String id, JsonObject fields, Handler<AsyncResult<@Nullable JsonObject>> resultHandler);

    @Fluent
    VetESService save(String index, JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetESService bulkCreate(String index, JsonArray users, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetESService deleteUser(String index, String id, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetESService isUserExist(String index, String id, Handler<AsyncResult<Boolean>> resultHandler);
}
