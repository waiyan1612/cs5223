import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.List;

public class Game {

    public static final boolean DEBUG = true;

    public static void main(String[] args) {

        String ip = null;
        int port = 0;
        String playerName;

        if(!DEBUG) {
            if (args.length < 3) {
                throw new IllegalArgumentException("You must specify tracker ip, tracker port number and playerName.");
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
            state.addPlayer(player);
            System.out.println("Adding player: " + player.toString());

            tracker = (TrackerState) state.getInfo();
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

        GUI gui = new GUI(N, N, K, players, playerName);
    }


    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return new StringBuilder(2).append((char) (97 + randomInt / 26)).append((char) (97 + randomInt % 26)).toString();
    }
}