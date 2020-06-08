package com.example.vet.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

public class VetWorker extends AbstractVerticle {
    private final String WORKER_QUEUE = "worker.queue";

    private enum ErrorCodes {
        NO_ACTION_SPECIFIED,
        BAD_ACTION,
    }

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(this.WORKER_QUEUE, this::onMessage);
    }

    private void onMessage(Message<String> message) {
        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action specified");
        } else {
            String action = message.headers().get("action");
            if (!action.equals("run-blocking-work")) {
                message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
            } else {
                try {
                    // Simulate blocking
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // Ignore
                } finally {
                    message.reply("Your heavy blocking work is done");
                }
            }
        }
    }
}
