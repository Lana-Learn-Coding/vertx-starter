package com.example.vet;

import io.vertx.core.Launcher;

public class VetApplication {
    public static void main(String[] args) {
        Launcher.executeCommand("run", VetServer.class.getName());
    }
}
