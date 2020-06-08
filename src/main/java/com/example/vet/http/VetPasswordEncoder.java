package com.example.vet.http;

import com.example.vet.QueueAddresses;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

public class VetPasswordEncoder extends AbstractVerticle {
    private enum ErrorCodes {
        NO_ACTION_SPECIFIED,
        BAD_ACTION,
    }

    @Override
    public void start() {
        vertx.eventBus().consumer(QueueAddresses.PASSWORD_ENCODER_QUEUE.address, this::onMessage);
    }

    private void onMessage(Message<String> message) {
        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action specified");
            return;
        }

        String action = message.headers().get("action");
        if (action.equals("encode")) {
            encode(message);
        } else {
            message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
        }
    }

    private void encode(Message<String> message) {
        try {
            // Encoding...
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        } finally {
            message.reply("encoded#" + message.body());
        }
    }
}
