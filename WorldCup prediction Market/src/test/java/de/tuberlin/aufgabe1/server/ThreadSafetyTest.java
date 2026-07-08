package de.tuberlin.aufgabe1.server;

import de.tuberlin.aufgabe1.server.AdminServiceImpl;
import de.tuberlin.aufgabe1.server.BettingServiceImpl;
import de.tuberlin.aufgabe1.server.Market;
import de.tuberlin.proto.AdminProto.*;
import de.tuberlin.proto.AdminServiceGrpc;
import de.tuberlin.proto.BettingProto.*;
import de.tuberlin.proto.BettingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafetyTest {

    // the server and its shared data
    private Server server;
    private ConcurrentHashMap<Integer, Market> markets;
    private AtomicInteger nextId;

    // starts the server before each test
    @BeforeEach
    public void startServer() throws IOException {
        markets = new ConcurrentHashMap<>();
        nextId = new AtomicInteger(1);

        server = ServerBuilder.forPort(1235)
                .addService(new AdminServiceImpl(markets, nextId))
                .addService(new BettingServiceImpl(markets))
                .build()
                .start();
    }

    // stops the server after each test
    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testMultipleClientsParallel() throws InterruptedException {
        // first create a market using admin client
        ManagedChannel adminChannel = ManagedChannelBuilder
                .forAddress("localhost", 1235)
                .usePlaintext()
                .build();
        AdminServiceGrpc.AdminServiceBlockingStub adminStub =
                AdminServiceGrpc.newBlockingStub(adminChannel);

        CreateMarketRequest createRequest = CreateMarketRequest.newBuilder()
                .setHomeTeam("Germany")
                .setAwayTeam("France")
                .build();
        CreateMarketResponse createResponse = adminStub.createMarket(createRequest);
        assertTrue(createResponse.getSuccess());
        int marketId = createResponse.getMarketId();

        // now simulate 10 bettors placing bets at the same time
        int numBettors = 10;
        CountDownLatch latch = new CountDownLatch(numBettors);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numBettors; i++) {
            final String bettorName = "Bettor" + i;
            final String prediction = i % 2 == 0 ? "HOME" : "AWAY";

            Thread thread = new Thread(() -> {
                ManagedChannel bettorChannel = ManagedChannelBuilder
                        .forAddress("localhost", 1235)
                        .usePlaintext()
                        .build();
                BettingServiceGrpc.BettingServiceBlockingStub bettorStub =
                        BettingServiceGrpc.newBlockingStub(bettorChannel);

                PlaceBetRequest betRequest = PlaceBetRequest.newBuilder()
                        .setMarketId(marketId)
                        .setBettorName(bettorName)
                        .setPrediction(prediction)
                        .build();

                PlaceBetResponse betResponse = bettorStub.placeBet(betRequest);
                assertTrue(betResponse.getSuccess());
                latch.countDown();
            });

            threads.add(thread);
        }

        // start all threads at the same time
        threads.forEach(Thread::start);

        // wait for all bettors to finish
        latch.await();

        // verify all 10 bets were recorded correctly
        Market market = markets.get(marketId);
        assertNotNull(market);
        assertEquals(10, market.getBets().size());

        System.out.println("Thread safety test passed! All 10 bets recorded correctly.");
    }
}