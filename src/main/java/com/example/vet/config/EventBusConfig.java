package com.example.vet.config;

//TODO: find a better way to do this (extract constant)
public enum EventBusConfig {
    PASSWORD_ENCODER_QUEUE("encoder.worker.queue"),
    VET_DB_QUEUE("database.queue");

    public final String address;

    EventBusConfig(String address) {
        this.address = address;
    }
}
