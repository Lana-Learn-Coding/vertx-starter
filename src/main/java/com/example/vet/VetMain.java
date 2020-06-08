package com.example.vet;

import com.example.vet.database.VetDatabase;
import com.example.vet.http.VetHttpServer;
import com.example.vet.http.VetPasswordEncoder;
import io.vertx.core.*;

public class VetMain extends AbstractVerticle {
    public static void main(String[] args) {
        Launcher.executeCommand("run", VetMain.class.getName());
    }

    @Override
    public void start(Promise<Void> promise) {
        deployDatabase().compose(dbId ->
                deployWorker().compose(severId -> deployHttpServer())
        )
                .onComplete(ar -> promise.complete())
                .onFailure(ar -> promise.fail(ar.getCause()));
    }

    private Future<String> deployDatabase() {
        Promise<String> deployment = Promise.promise();
        vertx.deployVerticle(new VetDatabase(), deployment);
        return deployment.future();
    }

    private Future<String> deployHttpServer() {
        Promise<String> deployment = Promise.promise();
        DeploymentOptions options = new DeploymentOptions().setInstances(1);
        vertx.deployVerticle(VetHttpServer.class, options, deployment);
        return deployment.future();
    }

    private Future<String> deployWorker() {
        Promise<String> deployment = Promise.promise();
        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true)
                .setInstances(2);
        vertx.deployVerticle(VetPasswordEncoder.class, options, deployment);
        return deployment.future();
    }
}
