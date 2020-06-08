package com.example.vet;

import com.example.vet.database.VetDatabase;
import com.example.vet.http.VetHttpServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class VetMain extends AbstractVerticle {
    @Override
    public void start(Promise<Void> promise) {
        Promise<String> vetDBDeployment = Promise.promise();
        vertx.deployVerticle(new VetDatabase(), vetDBDeployment);
        vetDBDeployment.future().compose(id -> {
            Promise<String> vetHttpDeployment = Promise.promise();
            vertx.deployVerticle(new VetHttpServer(), vetHttpDeployment);
            return vetHttpDeployment.future();
        })
                .onComplete(ar -> promise.complete())
                .onFailure(throwable -> promise.fail(throwable.getCause()));
    }
}
