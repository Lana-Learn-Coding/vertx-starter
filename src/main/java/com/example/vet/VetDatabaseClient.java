package com.example.vet;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class VetDatabaseClient extends AbstractVerticle {
    private final String dbClientQueue = "dbclient.queue";

    private enum ErrorCodes {
        NO_ACTION_SPECIFIED,
        BAD_ACTION,
        BAD_QUERY
    }

    private MongoClient mongo;

    @Override
    public void start() {
        mongo = MongoClient.create(vertx, new JsonObject().put("db_name", "test"));
        vertx.eventBus().<JsonObject>consumer(dbClientQueue).handler(this::onMessage);
    }

    public void onMessage(Message<JsonObject> message) {
        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action specified");
            return;
        }
        String action = message.headers().get("action");
        switch (action) {
            case "get-user":
                fetchUser(message);
                break;
            case "get-all-user":
                fetchAllUser(message);
                break;
            case "create-user":
                createUser(message);
                break;
            case "update-user":
                updateUser(message);
                break;
            case "delete-user":
                deleteUser(message);
                break;
            default:
                message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Invalid action: " + action);
        }
    }

    public void fetchUser(Message<JsonObject> message) {
        mongo.findOne("user", message.body(), new JsonObject(), lookup -> {
            if (lookup.failed()) {
                message.fail(ErrorCodes.BAD_QUERY.ordinal(), lookup.cause().getMessage());
            } else {
                message.reply(lookup.result());
            }
        });
    }

    public void fetchAllUser(Message<JsonObject> message) {
        mongo.find("user", message.body(), lookup -> {
            if (lookup.failed()) {
                message.fail(ErrorCodes.BAD_QUERY.ordinal(), lookup.cause().getMessage());
            } else {
                JsonArray listUser = new JsonArray();
                for (JsonObject user : lookup.result()) {
                    listUser.add(user);
                }
                message.reply(listUser);
            }
        });
    }

    public void createUser(Message<JsonObject> message) {
        JsonObject user = message.body();
        mongo.insert("user", user, lookup -> {
            if (lookup.failed()) {
                message.fail(ErrorCodes.BAD_QUERY.ordinal(), lookup.cause().getMessage());
            } else {
                message.reply(lookup.result());
            }
        });
    }

    public void updateUser(Message<JsonObject> message) {
        JsonObject user = message.body();
        if (!user.containsKey("_id")) {
            message.fail(ErrorCodes.BAD_QUERY.ordinal(), "Missing _id field");
            return;
        }
        String id = user.getString("_id");
        mongo.findOne("user", new JsonObject().put("_id", id), new JsonObject(), lookup -> {
            if (lookup.failed()) {
                message.fail(ErrorCodes.BAD_QUERY.ordinal(), lookup.cause().getMessage());
            } else {
                if (lookup.result() == null) {
                    message.reply(null);
                } else {
                    mongo.findOneAndReplace("user", new JsonObject().put("_id", id), user, res -> {
                        if (res.failed()) {
                            message.fail(ErrorCodes.BAD_QUERY.ordinal(), res.cause().getMessage());
                        } else {
                            message.reply(user);
                        }
                    });
                }
            }
        });
    }

    public void deleteUser(Message<JsonObject> message) {
        JsonObject user = message.body();
        if (!user.containsKey("_id")) {
            message.fail(ErrorCodes.BAD_QUERY.ordinal(), "Missing _id field");
            return;
        }
        mongo.findOneAndDelete("user", user, lookup -> {
            if (lookup.failed()) {
                message.fail(ErrorCodes.BAD_QUERY.ordinal(), lookup.cause().getMessage());
            } else {
                message.reply(lookup.result());
            }
        });
    }
}
