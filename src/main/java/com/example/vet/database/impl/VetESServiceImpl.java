package com.example.vet.database.impl;

import com.example.vet.database.VetESService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;

public class VetESServiceImpl implements VetESService {
    private static final String INDEX = "test";
    private static final String TYPE = "user";
    private final TransportClient client;

    public VetESServiceImpl(TransportClient client, Handler<AsyncResult<VetESService>> readyHandler) {
        this.client = client;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public VetESService findAllUser(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
        return this;
    }

    @Override
    public VetESService findOneUser(JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
        return this;
    }

    @Override
    public VetESService fetchAllUser(Handler<AsyncResult<JsonArray>> resultHandler) {
        client.prepareSearch(INDEX)
            .setTypes(TYPE)
            .setQuery(QueryBuilders.matchAllQuery())
            .execute(new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse searchResponse) {
                    resultHandler.handle(Future.succeededFuture(VetESServiceMapper.mapToJsonArray(searchResponse)));
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }

    @Override
    public VetESService fetchUser(String id, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
        client
            .prepareGet(INDEX, TYPE, id)
            .execute(new ActionListener<GetResponse>() {
                @Override
                public void onResponse(GetResponse getFields) {
                    resultHandler.handle(Future.succeededFuture(new JsonObject(getFields.getSourceAsMap())));
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }

    @Override
    public VetESService save(JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
        Handler<AsyncResult<String>> fetchIdHandler = savedIdResult -> {
            if (savedIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                return;
            }
            fetchUser(savedIdResult.result(), new JsonObject(), savedUserResult -> {
                if (savedUserResult.failed()) {
                    resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                    return;
                }
                resultHandler.handle(Future.succeededFuture(savedUserResult.result()));
            });
        };

        if (user.containsKey("_id")) {
            return update(user.getString("_id"), user, fetchIdHandler);
        }
        return create(user, fetchIdHandler);
    }

    private VetESService create(JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        client
            .prepareIndex(INDEX, TYPE)
            .setSource(user.getMap())
            .execute(new ActionListener<IndexResponse>() {
                @Override
                public void onResponse(IndexResponse indexResponse) {
                    resultHandler.handle(Future.succeededFuture(indexResponse.getId()));
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }

    private VetESService update(String id, JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        user.remove("_id");
        client
            .prepareUpdate(INDEX, TYPE, id)
            .setDoc(user.getMap())
            .execute(new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse updateResponse) {
                    resultHandler.handle(Future.succeededFuture(updateResponse.getId()));
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }

    @Override
    public VetESService deleteUser(String id, Handler<AsyncResult<Void>> resultHandler) {
        client
            .prepareDelete(INDEX, TYPE, id)
            .execute(new ActionListener<DeleteResponse>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {
                    resultHandler.handle(Future.succeededFuture());
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }

    @Override
    public VetESService isUserExist(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        client
            .prepareExists(INDEX)
            .setTypes(TYPE)
            .setQuery(QueryBuilders.matchQuery("_id", id))
            .execute(new ActionListener<ExistsResponse>() {
                @Override
                public void onResponse(ExistsResponse existsResponse) {
                    resultHandler.handle(Future.succeededFuture(existsResponse.exists()));
                }

                @Override
                public void onFailure(Throwable e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            });
        return this;
    }
}
