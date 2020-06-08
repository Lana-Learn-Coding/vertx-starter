package com.example.vet.database;

import com.example.vet.QueueAddresses;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceBinder;

public class VetDatabase extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
        final MongoClient mongo = MongoClient.create(vertx, new JsonObject().put("db_name", "test"));
        VetDatabaseService.create(mongo, ready -> {
            if (ready.failed()) {
                startPromise.fail(ready.cause());
                return;
            }
            ServiceBinder binder = new ServiceBinder(vertx);
            binder.setAddress(QueueAddresses.VET_DB_QUEUE.address).register(VetDatabaseService.class, ready.result());
            startPromise.complete();
        });
    }
}
