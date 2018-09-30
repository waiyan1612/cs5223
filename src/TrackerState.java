import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrackerState implements ITrackerState, Serializable {
    public int N;
    public int K;
    public List<Player> players;
    private int secondaryServerPort;
    private int lastPortUsed;

    public TrackerState (int N, int K){
        this.N = N;
        this.K = K;
        this.players = new ArrayList<>();
        this.lastPortUsed = 9200;
    }

    public TrackerState(int N, int K, List<Player> players, int lastPortUsed) {
        this.N = N;
        this.K = K;
        this.players = players;
        this.lastPortUsed = lastPortUsed;
    }

    public synchronized void addPlayer(Player p){
        p.setPort(lastPortUsed);
        players.add(p);
        lastPortUsed ++;
    }

    public synchronized void removePlayer(Player p) {
        players.remove(p);
    }
    
    public synchronized void setPrimaryServerPort(int port) {
    	secondaryServerPort = port;
    }
    

    public ITrackerState getReadOnlyCopy() {
        return new TrackerState(N, K, players, lastPortUsed);
    }
    
    public int getPrimaryServerPort() {
    	return secondaryServerPort;
    }

    @Override
    public String toString() {
        return "N:" + N + ", K:" + K + ", players: " + players;
    }

}
