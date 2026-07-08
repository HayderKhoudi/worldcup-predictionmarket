package de.tuberlin.aufgabe1.server;

import de.tuberlin.proto.SpectatorProto.*;
import de.tuberlin.proto.SpectatorServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// SpectatorServiceImpl implements the SpectatorService defined in spectator .proto
// it allows spectators to watch live odds and see the leaderboard
public class SpectatorServiceImpl extends SpectatorServiceGrpc.SpectatorServiceImplBase {

    private final ConcurrentHashMap<Integer, Market> markets;

    public SpectatorServiceImpl(ConcurrentHashMap<Integer, Market> markets) {
        this.markets = markets;
    }

    @Override
    public void watchOdds(WatchOddsRequest request, StreamObserver<OddsUpdate> responseObserver) {
        Market market = markets.get(request.getMarketId());

        if (market == null) {
            responseObserver.onCompleted();
            return;
        }

        // send only one odss update for the requested market
        OddsUpdate update = OddsUpdate.newBuilder()
                .setHomeTeam(market.getHomeTeam())
                .setAwayTeam(market.getAwayTeam())
                .setHomeOdds(market.getOdds("HOME"))
                .setDrawOdds(market.getOdds("DRAW"))
                .setAwayOdds(market.getOdds("AWAY"))
                .build();

        responseObserver.onNext(update);
        responseObserver.onCompleted();
    }
    // returns the leaderboard of top bettors
    @Override
    public void getLeaderboard(LeaderboardRequest request, StreamObserver<LeaderboardResponse> responseObserver) {
        ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();

        System.out.println("Total markets: " + markets.size());
        // go through every market in the server
        for (Market market : markets.values()) {
            System.out.println("Market " + market.getId() + " resolved: " + market.isResolved());
            System.out.println("Market " + market.getId() + " result: " + market.getResult());
            System.out.println("Market " + market.getId() + " bets: " + market.getBets().size());
            // skip the markets that have not been resolved yet
            if (!market.isResolved()) continue;
            String result = market.getResult();
            // go though every bet placed on this market
            for (String[] bet : market.getBets()) {
                System.out.println("Bet: " + bet[0] + " predicted " + bet[1]);
                if (bet[1].equals(result)) {
                    scores.merge(bet[0], 1, Integer::sum);
                }
            }
        }
        // build the leaderboard response
        LeaderboardResponse.Builder builder = LeaderboardResponse.newBuilder();
        // sort bettors from highest to lowest score and add each string in the leaderboard
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> builder.addEntries(e.getKey() + " → " + e.getValue() + " correct predictions"));
        // send the leaderboard to the spectator
        responseObserver.onNext(builder.build());

        responseObserver.onCompleted();
    }
}