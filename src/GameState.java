import java.io.Serializable;
import java.util.*;

public class GameState implements IGameState, Serializable {
    public int N;
    public int K;
    public List<Integer> treasurePositions;
    public Map<String, PlayerState> playerStates;

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        this.treasurePositions = initTreasures();
        this.playerStates = new HashMap<>();
    }

    public GameState(int N, int K ,List<Integer> treasurePositions, Map<String, PlayerState> playerStates) {
        this.N = N;
        this.K = K;
        this.treasurePositions = treasurePositions;
        this.playerStates = playerStates;
    }

    public IGameState getInfo() {
        return new GameState(N, K, treasurePositions, playerStates);
    }

    public IGameState removeAndAddTreasure(int pos) {
        treasurePositions.remove(Integer.valueOf(pos));Random r = new Random();
        treasurePositions.add(r.nextInt(N*N));
        return getInfo();
    }

    public IGameState initPlayerState(String playerName) {
        playerStates.put(playerName, new PlayerState());
        return getInfo();
    }

    public IGameState updatePlayerState(String playerName, GameState.PlayerState state) {
        playerStates.put(playerName, state);
        return getInfo();
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

    public static class PlayerState implements Serializable {
        public int position;
        public int score;

        public PlayerState() {
            position = 0;
            score = 0;
        }

        @Override
        public String toString() {
            return new StringBuilder(7).append('<').append(position).append(',').append(score).append('>').toString();
        }
    }
}
