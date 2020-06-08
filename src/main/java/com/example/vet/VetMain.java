package com.example.vet;

import com.example.vet.database.VetDatabase;
import com.example.vet.http.VetHttpServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;

public class VetMain extends AbstractVerticle {
    public static void main(String[] args) {
        Launcher.executeCommand("run", VetMain.class.getName());
    }

    @Override
    public void start(Promise<Void> promise) {
        Promise<String> vetDBDeployment = Promise.promise();
        vertx.deployVerticle(new VetDatabase(), vetDBDeployment);

        vetDBDeployment.future().compose(id -> {
            Promise<String> vetHttpDeployment = Promise.promise();
            // Don't use new (pass instance) if you want to specify the number of instances
            // Because Vertx can't magically turn one instance of a Java object into N instances
            vertx.deployVerticle(VetHttpServer.class, new DeploymentOptions().setInstances(2), vetHttpDeployment);
            return vetHttpDeployment.future();
        })
                .onComplete(ar -> promise.complete())
                .onFailure(ar -> promise.fail(ar.getCause()));
    }
}
