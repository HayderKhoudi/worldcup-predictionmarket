package de.tuberlin.aufgabe1.server;

import java.util.ArrayList;
import java.util.List;
// Market represent a single market for one match
public class Market {
    private final int id ;
    private final String homeTeam;
    private final String awayTeam;
    private final List<String[]> bets; //list of all bets placed on this market
    private boolean resolved;
    private String result;
    // constructor
     public Market( int id, String homeTeam, String awayTeam){
         this.id=id;
         this.homeTeam=homeTeam;
         this.awayTeam=awayTeam;
         this.bets= new ArrayList<>();
         this.resolved=false;
         this.result= null;

     }
    public int getId() {
         return id;
     }
    public String getHomeTeam() {
         return homeTeam;
     }

    public String getAwayTeam() {
         return awayTeam;
     }
    public boolean isResolved() {
         return resolved;
     }
    public String getResult() {
         return result;
     }
    // synchronized ensures only one thread can add a bet at a time
    public synchronized void addBet(String Name, String prediction) {
        bets.add(new String[]{Name, prediction});
    }
    // synchronized: only one client can modify the bets list at a time
    public synchronized void resolve(String result) {
        this.resolved=true;
        this.result=result;
    }

    public synchronized List<String[]> getBets() {
        return new ArrayList<>(bets);
    }
// to calculate the odds based on how many people bet on each outcome.
// the more people bet on HOME, the lower the Home odds get
    public synchronized double getOdds(String outcome) {
        int total= bets.size();
        if (total== 0) return 2.0;
        long count= bets.stream()
                .filter(b -> b[1].equals(outcome))
                .count();
        if (count== 0) return 5.0;
        return Math.round((double) total / count * 10.0) / 10.0;
    }
}


