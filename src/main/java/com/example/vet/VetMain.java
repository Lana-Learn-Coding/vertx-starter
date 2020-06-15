package com.example.vet;

import com.example.vet.database.VetDatabase;
import com.example.vet.worker.VetPasswordEncoder;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;

public class VetMain extends AbstractVerticle {
    public static void main(String[] args) {
        Launcher.executeCommand("run", VetMain.class.getName());
    }

    @Override
    public void start(Promise<Void> promise) {
        deployDatabase()
            .andThen(deployWorker())
            .andThen(deployHttpServer())
            .subscribe(promise::complete, promise::fail);
    }

    private Completable deployDatabase() {
        return Completable.fromSingle(vertx.rxDeployVerticle(new VetDatabase()));
    }

    private Completable deployHttpServer() {
        DeploymentOptions options = new DeploymentOptions().setInstances(1);
        return Completable.fromSingle(vertx.rxDeployVerticle(VetHttpServer.class.getName(), options));
    }

    private Completable deployWorker() {
        DeploymentOptions options = new DeploymentOptions().setWorker(true).setInstances(2);
        return Completable.fromSingle(vertx.rxDeployVerticle(VetPasswordEncoder.class.getName(), options));
    }
}
