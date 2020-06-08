package com.example.vet.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceBinder;

public class VetDatabase extends AbstractVerticle {
    private final String VET_DB_QUEUE = "dbclient.queue";

    @Override
    public void start(Promise<Void> startPromise) {
        final MongoClient mongo = MongoClient.create(vertx, new JsonObject().put("db_name", "test"));
        VetDatabaseService.create(mongo, ready -> {
            if (ready.failed()) {
                startPromise.fail(ready.cause());
                return;
            }
            ServiceBinder binder = new ServiceBinder(vertx);
            binder.setAddress(VET_DB_QUEUE).register(VetDatabaseService.class, ready.result());
            startPromise.complete();
        });
    }
}
