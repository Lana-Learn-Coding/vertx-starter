package com.example.vet;

//TODO: find a better way to do this (extract constant)
public enum QueueAddresses {
    PASSWORD_ENCODER_QUEUE("encoder.worker.queue"),
    VET_DB_QUEUE("database.queue");

    public final String address;

    QueueAddresses(String address) {
        this.address = address;
    }
}
