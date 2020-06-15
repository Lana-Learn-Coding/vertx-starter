package com.example.vet.database;

import com.example.vet.config.EventBusConfig;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;

public class VetDatabase extends AbstractVerticle {
    TransportClient client = TransportClient.builder().build();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

        VetESService.create(client, ready -> {
            if (ready.failed()) {
                startPromise.fail(ready.cause());
                return;
            }
            ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
            binder.setAddress(EventBusConfig.VET_DB_QUEUE.address).register(VetESService.class, ready.result());
            startPromise.complete();
        });
    }

    @Override
    public void stop() {
        client.close();
    }
}
