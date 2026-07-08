package de.tuberlin.aufgabe1.client;
import de.tuberlin.proto.BettingProto.*;
import de.tuberlin.proto.BettingServiceGrpc;
import io.grpc.ManagedChannel; // the line between the client and the server
import io.grpc.ManagedChannelBuilder; // this builder creates the channel
import java.util.Scanner;

public class BettorClient {

    private final BettingServiceGrpc.BettingServiceBlockingStub stub; // our remote control to call betting methods on the server.
    private final String Name;

    public BettorClient(String host, int port, String bettorName) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        stub = BettingServiceGrpc.newBlockingStub(channel);
        this.Name = bettorName;
    }
    // builds a bet request with the market ID, bettor name and prediction
    // then send it to the server which adds the bet to the market
    public void placeBet(int marketId, String prediction) {
        PlaceBetRequest request = PlaceBetRequest.newBuilder()
                .setMarketId(marketId)
                .setBettorName(Name)
                .setPrediction(prediction)
                .build();

        PlaceBetResponse response = stub.placeBet(request);
        System.out.println(response.getMessage());
    }
    // sends a market ID to the server and gets back the current odds for all three outcomes.
    public void getOdds(int marketId) {
        GetOddsRequest request = GetOddsRequest.newBuilder()
                .setMarketId(marketId)
                .build();

        GetOddsResponse response = stub.getOdds(request);
        System.out.println("HOME: " + response.getHomeOdds()
                + " | DRAW: " + response.getDrawOdds()
                + " | AWAY: " + response.getAwayOdds());
    }
    // asks the server for all bets places by this bettor across all markets.
    public void getMyBets() {
        GetMyBetsRequest request = GetMyBetsRequest.newBuilder()
                .setBettorName(Name)
                .build();

        GetMyBetsResponse response = stub.getMyBets(request);
        if (response.getBetsList().isEmpty()) {
            System.out.println("No bets placed yet");
        } else {
            response.getBetsList().forEach(System.out::println);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: "); // asks the bettor to enter their name
        String name = scanner.nextLine();

        BettorClient client = new BettorClient("localhost", 1234, name);
        // keeps showing the menu until the bettor chooses to exit.
        while (true) {
            System.out.println("\n1. Place Bet");
            System.out.println("2. Get Odds");
            System.out.println("3. My Bets");
            System.out.println("4. Exit");
            System.out.print("Choose: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 1) {
                    // ask for market ID and prediction then place the bet
                    System.out.print("Market ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Prediction (HOME/DRAW/AWAY): ");
                    String prediction = scanner.nextLine().trim();
                    client.placeBet(id, prediction);

                } else if (choice == 2) {
                    // display current odds for a market
                    System.out.print("Market ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    client.getOdds(id);

                } else if (choice == 3) {
                    // it shows all bets this bettor has placed
                    client.getMyBets();

                } else if (choice == 4) {
                    break;

                } else {
                    System.out.println("Invalid choice! Please enter 1, 2, 3 or 4");
                }

            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number");
            }
        }
    }
}