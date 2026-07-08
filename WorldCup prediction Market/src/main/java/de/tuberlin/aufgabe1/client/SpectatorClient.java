package de.tuberlin.aufgabe1.client;

import de.tuberlin.proto.SpectatorProto.*;
import de.tuberlin.proto.SpectatorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class SpectatorClient {

    // stub to call methods on the server, async because we receive a stream of updates
    private final SpectatorServiceGrpc.SpectatorServiceStub stub;

    // constructor opens a connection to the server and creates the stub
    public SpectatorClient(String host, int port) {
        // open a phone line to the server on the given host and port
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        // create an async stub using that connection
        stub = SpectatorServiceGrpc.newStub(channel);
    }

    // watch live odds updates for a specific market
    public void watchOdds(int marketId) throws InterruptedException {
        // CountDownLatch waits until all 5 updates are received before continuing
        CountDownLatch latch = new CountDownLatch(1);

        WatchOddsRequest request = WatchOddsRequest.newBuilder()
                .setMarketId(marketId)
                .build();

        // StreamObserver handles the stream of responses from the server
        stub.watchOdds(request, new StreamObserver<OddsUpdate>() {

            // called every time the server sends a new odds update
            @Override
            public void onNext(OddsUpdate update) {
                System.out.println("\n" + update.getHomeTeam() + " vs " + update.getAwayTeam());
                System.out.println("HOME: " + update.getHomeOdds()
                        + " | DRAW: " + update.getDrawOdds()
                        + " | AWAY: " + update.getAwayOdds());
            }

            // called if something goes wrong during streaming
            @Override
            public void onError(Throwable t) {
                System.out.println("Error watching odds: " + t.getMessage());
                latch.countDown();
            }

            // called when the server is done sending all updates
            @Override
            public void onCompleted() {
                System.out.println("Stream completed");
                latch.countDown();
            }
        });

        // wait here until the stream is fully completed
        latch.await();
    }

    // get the current leaderboard from the server
    public void getLeaderboard() {
        // empty request, no input needed
        LeaderboardRequest request = LeaderboardRequest.newBuilder().build();

        // use a blocking stub just for this one call
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 1234)
                .usePlaintext()
                .build();
        SpectatorServiceGrpc.SpectatorServiceBlockingStub blockingStub =
                SpectatorServiceGrpc.newBlockingStub(channel);

        LeaderboardResponse response = blockingStub.getLeaderboard(request);

        // print each leaderboard entry
        if (response.getEntriesList().isEmpty()) {
            System.out.println("No resolved markets yet");
        } else {
            response.getEntriesList().forEach(System.out::println);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // create client connected to localhost on port 9090
        SpectatorClient client = new SpectatorClient("localhost", 1234);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Watch Live Odds");
            System.out.println("2. Get Leaderboard");
            System.out.println("3. Exit");
            System.out.print("Choose: ");
            int choice = scanner.nextInt();

            if (choice == 1) {
                // ask which market to watch
                System.out.print("Market ID: ");
                int id = scanner.nextInt();
                client.watchOdds(id);

            } else if (choice == 2) {
                // fetch and display the leaderboard
                client.getLeaderboard();

            } else if (choice == 3) {
                break;
            }
        }
    }
}