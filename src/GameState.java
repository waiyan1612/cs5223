import java.io.Serializable;
import java.util.*;

public class GameState implements IGameState, Serializable  {

    public final int N;
    public final int K;
    private Set<Integer> treasurePositions = new HashSet<>();
    private Map<String, PlayerState> playerStates = new HashMap<>();
    public int primary;
    public int secondary;

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        createTreasures();
    }

    public GameState(int N, int K, Set<Integer> treasurePositions, Map<String, PlayerState> playerStates,
                     int primary, int secondary) {
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

    public int getPrimary() {
        return primary;
    }

    public int getSecondary() {
        return secondary;
    }

    public IGameState getReadOnlyCopy() {
        return new GameState(N, K, treasurePositions, playerStates, primary, secondary);
    }

    public void setPrimary(int port) {
        primary = port;
    }

    public void setSecondary(int port) {
        secondary = port;
    }

    public void initPlayerState(String playerName) {
        playerStates.put(playerName, new PlayerState(randValidPosition()));
    }

    public Set<Integer> getTreasurePositions(){
        return treasurePositions;
    }

    public Map<String, PlayerState> getPlayerStates(){
        return playerStates;
    }

    public void createTreasures(){
        Random r = new Random();
        while(treasurePositions.size() < K) {
            treasurePositions.add(r.nextInt(N*N));
        }
    }

    public void move(Player player, int diff) {
        GameState.PlayerState ps = getPlayerStates().get(player.name);
        if ((diff == -1  && ps.position % N == 0) || // left
                (diff == N && ps.position >= N*(N-1)) || // bottom
                (diff == 1 && ps.position % N == N-1) || // right
                (diff == -N && ps.position < N)) {  // top
            return;
        }

        int newPosition = ps.position + diff;
        for (Map.Entry<String, GameState.PlayerState> entry : getPlayerStates().entrySet()) {
            if (entry.getValue().position == newPosition) {     //someone is already there
                return;
            }
        }

        ps.position = newPosition;
        if(getTreasurePositions().contains(ps.position)) {
            removeTreasures(ps.position);
            ps.score++;
        }
    }

    public void removeTreasures(int position){
        treasurePositions.remove(position);
    }

    private int randValidPosition() {
        Set<Integer> disallowedPositions = new HashSet<>(treasurePositions);
        for (Map.Entry<String, GameState.PlayerState> entry : playerStates.entrySet()) {
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
