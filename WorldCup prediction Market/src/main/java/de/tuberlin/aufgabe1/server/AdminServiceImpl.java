package de.tuberlin.aufgabe1.server;

import de.tuberlin.proto.AdminProto.*;
import de.tuberlin.proto.AdminServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ConcurrentHashMap<Integer, Market> markets;
    private final AtomicInteger nextId;

    public AdminServiceImpl(ConcurrentHashMap<Integer, Market> markets, AtomicInteger nextId) {
        this.markets = markets;
        this.nextId = nextId;
    }

    @Override
    public void createMarket(CreateMarketRequest request, StreamObserver<CreateMarketResponse> responseObserver) {
        int id= nextId.getAndIncrement();
        Market market=new Market(id,request.getHomeTeam(),request.getAwayTeam());
        markets.put(id, market);

        CreateMarketResponse response= CreateMarketResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Market created: " + request.getHomeTeam() + " vs " + request.getAwayTeam())
                .setMarketId(id)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void resolveMarket(ResolveMarketRequest request, StreamObserver<ResolveMarketResponse> responseObserver) {
        Market market = markets.get(request.getMarketId());
        ResolveMarketResponse response;

        if (market == null) {
            response = ResolveMarketResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Market not found")
                    .build();
        } else {
            market.resolve(request.getResult());
            response = ResolveMarketResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Market resolved with result: " + request.getResult())
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteMarket(DeleteMarketRequest request, StreamObserver<DeleteMarketResponse> responseObserver) {
        Market removed = markets.remove(request.getMarketId());
        DeleteMarketResponse response;

        if (removed == null) {
            response = DeleteMarketResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Market not found")
                    .build();
        } else {
            response = DeleteMarketResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Market deleted successfully")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}