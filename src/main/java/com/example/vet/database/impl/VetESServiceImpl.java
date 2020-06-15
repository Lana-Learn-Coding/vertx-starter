package com.example.vet.database.impl;

import com.example.vet.database.VetESService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;

public class VetESServiceImpl implements VetESService {
    private final TransportClient client;

    public VetESServiceImpl(TransportClient client, Handler<AsyncResult<VetESService>> readyHandler) {
        this.client = client;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public VetESService findAllUser(String index, JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
        client
                .prepareSearch(index)
                .setQuery(query.toString())
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
    public VetESService fetchAllUser(String index, Handler<AsyncResult<JsonArray>> resultHandler) {
        client.prepareSearch(index)
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
    public VetESService fetchUser(String index, String id, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
        client
                .prepareGet()
                .setIndex(index)
                .setId(id)
                .execute(new ActionListener<GetResponse>() {
                    @Override
                    public void onResponse(GetResponse getFields) {
                        if (getFields.isExists()) {
                            resultHandler.handle(Future.succeededFuture(new JsonObject(getFields.getSourceAsMap())));
                            return;
                        }
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
    public VetESService save(String index, JsonObject user, Handler<AsyncResult<JsonObject>> resultHandler) {
        Handler<AsyncResult<String>> fetchIdHandler = savedIdResult -> {
            if (savedIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                return;
            }
            fetchUser(index, savedIdResult.result(), new JsonObject(), savedUserResult -> {
                if (savedUserResult.failed()) {
                    resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                    return;
                }
                resultHandler.handle(Future.succeededFuture(savedUserResult.result()));
            });
        };

        if (user.containsKey("_id")) {
            return update(index, user.getString("_id"), user, fetchIdHandler);
        }
        return create(index, user, fetchIdHandler);
    }

    @Override
    public VetESService bulkCreate(String index, JsonArray users, Handler<AsyncResult<Void>> resultHandler) {
        BulkRequestBuilder requestBuilder = client.prepareBulk();
        users.forEach(user -> {
            IndexRequestBuilder indexRequestBuilder = client
                .prepareIndex()
                .setIndex(index)
                .setSource(user.toString());
            requestBuilder.add(indexRequestBuilder);
        });
        requestBuilder.execute(new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                resultHandler.handle(Future.succeededFuture());
            }

            @Override
            public void onFailure(Throwable e) {
                resultHandler.handle(Future.failedFuture(e));
            }
        });
        return this;
    }

    private VetESService create(String index, JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        client
                .prepareIndex()
                .setIndex(index)
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

    private VetESService update(String index, String id, JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        user.remove("_id");
        client
                .prepareUpdate()
                .setIndex(index)
                .setId(id)
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
    public VetESService deleteUser(String index, String id, Handler<AsyncResult<Void>> resultHandler) {
        client
                .prepareDelete()
                .setIndex(index)
                .setId(id)
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
    public VetESService isUserExist(String index, String id, Handler<AsyncResult<Boolean>> resultHandler) {
        client
                .prepareExists(index)
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
