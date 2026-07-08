package de.tuberlin.aufgabe1.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PredictionServer {
    private final int port = 1234; // key
    private Server server;
    private final ConcurrentHashMap<Integer, Market> markets = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new AdminServiceImpl(markets, nextId))
                .addService(new BettingServiceImpl(markets))
                .addService(new SpectatorServiceImpl(markets))
                .build()
                .start();

        System.out.println("Server started on port " + port);
    }
    public void stop() {
        if (server != null) {
            server.shutdown();
            System.out.println("Server stopped");
        }
    }
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        PredictionServer server = new PredictionServer();
        server.start();
        server.blockUntilShutdown();
    }
}