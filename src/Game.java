import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Game {

    public static final boolean DEBUG = true;

    public String playerName;
    public List<Integer> treasurePositions;
    public Map<String, PlayerState> playerStates;

    public static void main(String[] args) {

        String ip = null;
        int port = 0;
        String playerName;

        if(!DEBUG) {
            if (args.length < 3) {
                throw new IllegalArgumentException("You must specify trackerIP, trackerPort and playerName.");
            }
            ip = args[0];
            port = Integer.parseInt(args[1]);
            playerName = args[2];
        } else {
            playerName = args.length > 0 ? args[0] : createID();
        }

        Player player = new Player(playerName, ip, port);
        Registry registry;
        TrackerState tracker = null;
        try {
            registry = LocateRegistry.getRegistry(ip, port);
            ITrackerState state = (ITrackerState) registry.lookup("Tracker");

            // Adding new player
            System.out.println("Adding player: " + player.toString());
            tracker = (TrackerState) state.addPlayer(player);
            if(tracker == null) {
                System.err.println("Failed to retrieve information from tracker");
                System.exit(-1);
            }
            System.out.println("Tracker: " + tracker.toString());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // get N and K from tracker
        int N = tracker.N;
        int K = tracker.K;

        // get location of players and treasures from other players
        List<Player> players = tracker.players;

        Game g = new Game();
        g.playerName = playerName;

        if(players.size() == 1) {           // primary server
            System.out.println("Primary Server");
            g.treasurePositions = buryTreasures(N, N, K);
            g.playerStates = new HashMap<>();
        } else if(players.size() == 2) {    // backup server
            System.out.println("Backup Server");
            Player firstPlayer = players.get(0);
            // contact first player to get players and treasures
            g.treasurePositions = new ArrayList<>();
            g.playerStates = new HashMap<>();
        } else {
            System.out.println("Normal Client");
            Random r = new Random();
            int randomInt = r.nextInt(players.size()-1);
            Player randomPlayer = players.get(randomInt);
            // contact random player to get players and treasures
            g.treasurePositions = new ArrayList<>();
            g.playerStates = new HashMap<>();
        }
        g.playerStates.put(g.playerName, new PlayerState());
        new GUI(N, N, g.treasurePositions, g.playerStates, playerName);
    }

    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return new StringBuilder(2).append((char) (97 + randomInt / 26)).append((char) (97 + randomInt % 26)).toString();
    }

    private static List<Integer> buryTreasures(int count, int rows, int cols){
        Random r = new Random();
        List<Integer> positions = new ArrayList<>();
        for(int i=0; i<count; i++) {
            positions.add(r.nextInt(rows*cols));
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

        public PlayerState(int pos, int score) {
            this.position = pos;
            this.score = score;
        }
    }
}