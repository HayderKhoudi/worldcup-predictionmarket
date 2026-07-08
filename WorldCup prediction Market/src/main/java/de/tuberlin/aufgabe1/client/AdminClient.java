package de.tuberlin.aufgabe1.client;
import de.tuberlin.proto.AdminProto.*;
import de.tuberlin.proto.AdminServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Scanner;

public class AdminClient {

    private final AdminServiceGrpc.AdminServiceBlockingStub stub; // is the remote control to call the methods on the server,
    // BlockingStub so owe wait for the server response before continuing

    public AdminClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    // to build a request with the two team names and send it to the server
    public void createMarket(String homeTeam, String awayTeam) {
        CreateMarketRequest request = CreateMarketRequest.newBuilder()
                .setHomeTeam(homeTeam)
                .setAwayTeam(awayTeam)
                .build();

        CreateMarketResponse response = stub.createMarket(request);
        System.out.println(response.getMessage() + " (ID: " + response.getMarketId() + ")");
    }

    // send the market ID and the final result to the server, and the server market as resolved and calculates leader board scores.
    public void resolveMarket(int marketId, String result) {
        ResolveMarketRequest request = ResolveMarketRequest.newBuilder()
                .setMarketId(marketId)
                .setResult(result)
                .build();

        ResolveMarketResponse response = stub.resolveMarket(request);
        System.out.println(response.getMessage());
    }

    // send the marked ID to the server to delet it. And the server removes it from the market map and confirms the deletion.
    public void deleteMarket(int marketId) {
        DeleteMarketRequest request = DeleteMarketRequest.newBuilder()
                .setMarketId(marketId)
                .build();

        DeleteMarketResponse response = stub.deleteMarket(request);
        System.out.println(response.getMessage());
    }

    // this menu keeps showing until the admin chooses to exit
    public static void main(String[] args) {
        AdminClient client = new AdminClient("localhost", 1234);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Create Market");
            System.out.println("2. Resolve Market");
            System.out.println("3. Delete Market");
            System.out.println("4. Exit");
            System.out.print("Choose: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 1) {
                    System.out.print("Home team: ");
                    String home = scanner.nextLine().trim();
                    System.out.print("Away team: ");
                    String away = scanner.nextLine().trim();
                    client.createMarket(home, away);

                } else if (choice == 2) {
                    System.out.print("Market ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Result (HOME/DRAW/AWAY): ");
                    String result = scanner.nextLine().trim();
                    client.resolveMarket(id, result);

                } else if (choice == 3) {
                    System.out.print("Market ID: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    client.deleteMarket(id);

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