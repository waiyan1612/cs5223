import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrackerState implements ITrackerState, Serializable {
    public int N;
    public int K;
    public List<Player> players;

    public TrackerState (int N, int K){
        this.N = N;
        this.K = K;
        this.players = new ArrayList<>();
    }

    public TrackerState(int N, int K, List<Player> players) {
        this.N = N;
        this.K = K;
        this.players = players;
    }

    /* Implementation of addPlayer */
    public ITrackerState addPlayer(Player p){
        players.add(p);
        return getInfo();
    }

    /* Implementation of removePlayer */
    public ITrackerState removePlayer(Player p) {
        players.remove(p);
        return getInfo();
    }

    /* Implementation of getInfo */
    public ITrackerState getInfo() {
        return new TrackerState(N, K, players);
    }

    @Override
    public String toString() {
        return "N:" + N + ", K:" + K + ", players: " + players;
    }

}
