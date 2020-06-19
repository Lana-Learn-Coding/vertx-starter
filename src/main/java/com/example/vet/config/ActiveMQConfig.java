package com.example.vet.config;

public enum ActiveMQConfig {
    BROKER_URL("tcp://localhost:61616"),
    USER_CREATION_QUEUE("activemq.queue.user_creation");
    public final String address;

    ActiveMQConfig(String address) {
        this.address = address;
    }
}

