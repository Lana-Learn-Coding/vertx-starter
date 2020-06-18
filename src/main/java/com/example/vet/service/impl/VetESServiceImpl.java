package com.example.vet.service.impl;

import com.example.vet.service.VetESService;
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

import java.util.Optional;

public class VetESServiceImpl implements VetESService {
    private final TransportClient client;

    public VetESServiceImpl(TransportClient client, Handler<AsyncResult<VetESService>> readyHandler) {
        this.client = client;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public VetESService findAllUser(JsonObject search, Handler<AsyncResult<JsonArray>> resultHandler) {
        JsonObject query = Optional.ofNullable(search.getJsonObject("query")).orElseGet(JsonObject::new);
        int from = Optional.ofNullable(search.getInteger("from")).orElse(0);
        int size = Optional.ofNullable(search.getInteger("size")).orElse(20);
        String index = search.getString("index");
        return findAllUser(index, query, from, size, resultHandler);
    }

    @Override
    public VetESService fetchAllUser(JsonObject option, Handler<AsyncResult<JsonArray>> resultHandler) {
        option.remove("query");
        return this.findAllUser(option, resultHandler);
    }

    private VetESService findAllUser(String index, JsonObject query, int from, int size, Handler<AsyncResult<JsonArray>> resultHandler) {
        String queryString;
        if (query == null || query.size() == 0) {
            queryString = QueryBuilders.matchAllQuery().toString();
        } else {
            queryString = new JsonObject().put("query", query).toString();
        }

        client
            .prepareSearch(index)
            .setQuery(queryString)
            .setFrom(from)
            .setSize(size)
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
    public VetESService fetchUser(JsonObject identify, Handler<AsyncResult<JsonObject>> resultHandler) {
        return this.fetchUser(identify.getString("index"), identify.getString("type"), identify.getString("id"), resultHandler);
    }

    private VetESService fetchUser(String index, String type, String id, Handler<AsyncResult<JsonObject>> resultHandler) {
        client
            .prepareGet()
            .setIndex(index)
            .setType(type)
            .setId(id)
            .execute(new ActionListener<GetResponse>() {
                @Override
                public void onResponse(GetResponse getFields) {
                    if (getFields.isExists()) {
                        JsonObject user = new JsonObject(getFields.getSourceAsMap());
                        user.put("_id", id);
                        resultHandler.handle(Future.succeededFuture(user));
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
    public VetESService bulkCreate(JsonObject modification, Handler<AsyncResult<Void>> resultHandler) {
        BulkRequestBuilder requestBuilder = client.prepareBulk();
        JsonArray modifications = modification.getJsonArray("modification");
        modifications.forEach(item -> {
            IndexRequestBuilder indexRequestBuilder = client
                .prepareIndex()
                .setIndex(modification.getString("index"))
                .setType(modification.getString("type"))
                .setSource(item.toString());
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

    @Override
    public VetESService save(JsonObject modification, Handler<AsyncResult<JsonObject>> resultHandler) {
        final String index = modification.getString("index");
        final String type = modification.getString("type");
        Handler<AsyncResult<String>> fetchIdHandler = savedIdResult -> {
            if (savedIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                return;
            }
            this.fetchUser(index, type, savedIdResult.result(), savedUserResult -> {
                if (savedUserResult.failed()) {
                    resultHandler.handle(Future.failedFuture(savedIdResult.cause()));
                    return;
                }
                resultHandler.handle(Future.succeededFuture(savedUserResult.result()));
            });
        };

        JsonObject user = modification.getJsonObject("modification");
        if (user.containsKey("_id")) {
            return update(index, type, user.getString("_id"), user, fetchIdHandler);
        }
        return create(index, type, user, fetchIdHandler);
    }

    private VetESService create(String index, String type, JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        client
            .prepareIndex()
            .setIndex(index)
            .setType(type)
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

    private VetESService update(String index, String type, String id, JsonObject user, Handler<AsyncResult<String>> resultHandler) {
        user.remove("_id");
        client
            .prepareUpdate()
            .setIndex(index)
            .setId(id)
            .setType(type)
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
    public VetESService deleteUser(JsonObject identify, Handler<AsyncResult<Void>> resultHandler) {
        return this.deleteUser(identify.getString("index"), identify.getString("type"), identify.getString("id"), resultHandler);
    }

    private VetESService deleteUser(String index, String type, String id, Handler<AsyncResult<Void>> resultHandler) {
        client
            .prepareDelete()
            .setIndex(index)
            .setId(id)
            .setType(type)
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
    public VetESService isUserExist(JsonObject identify, Handler<AsyncResult<Boolean>> resultHandler) {
        return this.isUserExist(identify.getString("index"), identify.getString("type"), identify.getString("id"), resultHandler);
    }

    private VetESService isUserExist(String index, String type, String id, Handler<AsyncResult<Boolean>> resultHandler) {
        client
            .prepareExists(index)
            .setTypes(type)
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
