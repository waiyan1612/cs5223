import java.io.Serializable;
import java.util.*;

public class GameState implements Serializable  {
    public final int N;
    public final int K;
    public List<Integer> treasurePositions;
    public Map<String, PlayerState> playerStates;

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        this.treasurePositions = initTreasures();
        this.playerStates = new HashMap<>();
    }

    public void removeAndAddTreasure(int pos) {
        treasurePositions.remove(Integer.valueOf(pos));Random r = new Random();
        treasurePositions.add(r.nextInt(N*N));
    }

    public void initPlayerState(String playerName) {
        playerStates.put(playerName, new PlayerState(randValidPosition()));
    }

    public void updatePlayerState(String playerName, GameState.PlayerState state) {
        playerStates.put(playerName, state);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("N: ").append(N).append("; K: ").append(K)
                .append("; Treasures: ").append(treasurePositions)
                .append("; PlayerStates: ").append(playerStates);
        return sb.toString();
    }

    private List<Integer> initTreasures(){
        Random r = new Random();
        List<Integer> positions = new ArrayList<>();
        for(int i=0; i<K; i++) {
            positions.add(r.nextInt(N*N));
        }
        return positions;
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
