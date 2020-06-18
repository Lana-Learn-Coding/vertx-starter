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
    VetESService findAllUser(JsonObject search, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService fetchAllUser(JsonObject search, Handler<AsyncResult<JsonArray>> resultHandler);

    @Fluent
    VetESService fetchUser(JsonObject identify, Handler<AsyncResult<@Nullable JsonObject>> resultHandler);

    @Fluent
    VetESService save(String index, JsonObject modification, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    VetESService bulkCreate(String index, JsonArray modifications, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetESService deleteUser(JsonObject identify, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    VetESService isUserExist(JsonObject identify, Handler<AsyncResult<Boolean>> resultHandler);
}
