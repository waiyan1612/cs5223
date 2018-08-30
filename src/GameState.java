import java.io.Serializable;
import java.util.*;

public class GameState implements Serializable  {
    public final int N;
    public final int K;
    private Set<Integer> treasurePositions = new HashSet<>();
    private Map<String, PlayerState> playerStates = new HashMap<>();

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        createTreasures();
    }

    public void initPlayerState(String playerName) {
        playerStates.put(playerName, new PlayerState(randValidPosition()));
    }

    public void updatePlayerState(String playerName, GameState.PlayerState state) {
        playerStates.put(playerName, state);
    }

    public Set<Integer> getTreasurePositions(){
        return treasurePositions;
    }

    public Map<String, PlayerState> getPlayerStates(){
        return playerStates;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("N: ").append(N).append("; K: ").append(K)
                .append("; Treasures: ").append(treasurePositions)
                .append("; PlayerStates: ").append(playerStates);
        return sb.toString();
    }

    public void createTreasures(){
        Random r = new Random();
        while(treasurePositions.size() < K) {
            treasurePositions.add(r.nextInt(N*N));
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
