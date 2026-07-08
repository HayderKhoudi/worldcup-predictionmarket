package de.tuberlin.aufgabe1.server;

import de.tuberlin.proto.BettingProto.*;
import de.tuberlin.proto.BettingServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BettingServiceImpl extends BettingServiceGrpc.BettingServiceImplBase {

    private final ConcurrentHashMap<Integer, Market> markets;

    public BettingServiceImpl(ConcurrentHashMap<Integer, Market> markets) {
        this.markets = markets;
    }

    @Override
    public void placeBet(PlaceBetRequest request, StreamObserver<PlaceBetResponse> responseObserver) {
        Market market = markets.get(request.getMarketId());
        PlaceBetResponse response;

        if (market == null) {
            response = PlaceBetResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Market not found")
                    .build();
        } else if (market.isResolved()) {
            response = PlaceBetResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Market is already resolved")
                    .build();
        } else {
            market.addBet(request.getBettorName(), request.getPrediction());
            response = PlaceBetResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Bet placed: " + request.getBettorName() + " bets " + request.getPrediction())
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getOdds(GetOddsRequest request, StreamObserver<GetOddsResponse> responseObserver) {
        Market market = markets.get(request.getMarketId());

        if (market == null) {
            responseObserver.onNext(GetOddsResponse.newBuilder().build());
        } else {
            GetOddsResponse response = GetOddsResponse.newBuilder()
                    .setHomeOdds(market.getOdds("HOME"))
                    .setDrawOdds(market.getOdds("DRAW"))
                    .setAwayOdds(market.getOdds("AWAY"))
                    .build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getMyBets(GetMyBetsRequest request, StreamObserver<GetMyBetsResponse> responseObserver) {
        String bettorName = request.getBettorName();
        GetMyBetsResponse.Builder builder = GetMyBetsResponse.newBuilder();

        for (Market market : markets.values()) {
            List<String[]> bets = market.getBets();
            for (String[] bet : bets) {
                if (bet[0].equals(bettorName)) {
                    builder.addBets("Market " + market.getId() + ": "
                            + market.getHomeTeam() + " vs " + market.getAwayTeam()
                            + " → you bet " + bet[1]);
                }
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}