import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;

public class GameState implements IGameState, Serializable  {

    public final int N;
    public final int K;
    private Set<Integer> treasurePositions = new HashSet<>();
    private Map<Player, PlayerState> playerStates = new HashMap<>();
    private Player primary;
    private Player secondary;
    private IGameState secondaryStub;

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        createTreasures();
    }

    public GameState(int N, int K, Set<Integer> treasurePositions, Map<Player, PlayerState> playerStates,
                     Player primary, Player secondary) {
        this.N = N;
        this.K = K;
        this.treasurePositions = treasurePositions;
        this.playerStates = playerStates;
        this.primary = primary;
        this.secondary = secondary;
    }

    public int getN() {
        return N;
    }

    public Player getPrimary() {
        return primary;
    }

    public Player getSecondary() {
        return secondary;
    }
   
    
    public IGameState getSecondaryStub() {
        return secondaryStub;
    }

    public Set<Integer> getTreasurePositions(){
        return treasurePositions;
    }

    public Map<Player, PlayerState> getPlayerStates(){
        return playerStates;
    }

    public IGameState getReadOnlyCopy() {
        return new GameState(N, K, treasurePositions, playerStates, primary, secondary);
    }

    public synchronized void setPrimary(Player port) {
        primary = port;
        updateBackupCopy();
    }

    public synchronized void setSecondary(Player port) {
        secondary = port;
    }

    public synchronized void setSecondaryGameState(IGameState stub){
        secondaryStub = stub;
    }

    public synchronized void createTreasures(){
        Random r = new Random();
        while(treasurePositions.size() < K) {
            treasurePositions.add(r.nextInt(N*N));
        }
        updateBackupCopy();
    }

    public void removeTreasures(int position){
        treasurePositions.remove(position);
    }

    public synchronized IGameState initPlayerState(Player player) {
        playerStates.put(player, new PlayerState(randValidPosition()));
        updateBackupCopy();
        return getReadOnlyCopy();
    }

    public synchronized void move(Player player, int diff) {
        GameState.PlayerState ps = getPlayerStates().get(player);
        if ((diff == -1  && ps.position % N == 0) || // left
                (diff == N && ps.position >= N*(N-1)) || // bottom
                (diff == 1 && ps.position % N == N-1) || // right
                (diff == -N && ps.position < N)) {  // top
            return;
        }

        int newPosition = ps.position + diff;
        for (Map.Entry<Player, GameState.PlayerState> entry: getPlayerStates().entrySet()) {
            if (entry.getValue().position == newPosition) {     //someone is already there
                return;
            }
        }

        ps.position = newPosition;
        if(getTreasurePositions().contains(ps.position)) {
            removeTreasures(ps.position);
            ps.score++;
            createTreasures();
        } else {
            updateBackupCopy();
        }
    }

    public void removePlayer(Player p){
        playerStates.remove(p);
        updateBackupCopy();
    }
    
    public void removePrimaryServer(Player p) {
        playerStates.remove(p);
        secondary = null;
    }

    public void removeSecondaryServer(Player p) {
        playerStates.remove(p);
        secondary = null;
        // no need to updateBackupCopy
    }

    public synchronized void updateBackupCopy() {
        if(secondaryStub != null) {
            try {
                secondaryStub.updateAll(treasurePositions, playerStates, primary, secondary);
            } catch (RemoteException e) {
                System.err.println("Failed to update back up copy: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void updateAll(Set<Integer> treasurePositions, Map<Player, PlayerState> playerStates,
                          Player primary, Player secondary) {
        this.treasurePositions = treasurePositions;
        this.playerStates = playerStates;
        this.primary = primary;
        this.secondary = secondary;
    }

    private int randValidPosition() {
        Set<Integer> disallowedPositions = new HashSet<>(treasurePositions);
        for (Map.Entry<Player, GameState.PlayerState> entry : playerStates.entrySet()) {
            disallowedPositions.add(entry.getValue().position);
        }
        Random r = new Random();
        int bound = N * N;
        int randomInt = r.nextInt(bound);
        while(disallowedPositions.contains(randomInt)) {
            randomInt = r.nextInt(bound);
        }
        return randomInt;
    }


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("N: ").append(N).append("; K: ").append(K)
                .append("; Treasures: ").append(treasurePositions)
                .append("; PlayerStates: ").append(playerStates);
        return sb.toString();
    }

    public static class PlayerState implements Serializable {
        public int position;
        public int score;

        public PlayerState(int pos) {
            position = pos;
            score = 0;
        }

        @Override
        public String toString() {
            return new StringBuilder(7).append('<').append(position).append(',').append(score).append('>').toString();
        }
    }
}
