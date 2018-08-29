import java.beans.PropertyChangeSupport;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.List;

public class Game {

    private GameState gameState;
    private Player player;
    private GUI gui;
    private PropertyChangeSupport observable;

    public Game (Player p, GameState gs) {
        player = p;
        gameState = gs;
        gui = new GUI(gameState, player.name);
        observable = new PropertyChangeSupport(this);
        observable.addPropertyChangeListener(gui);
    }

    public static void main(String[] args) {

        String ip = null;
        int port = 0;
        String playerName;

        if (args.length == 3) {
            ip = args[0];
            port = Integer.parseInt(args[1]);
            playerName = args[2];
        } else if (args.length == 0) {
            playerName = createID();
        } else {
            throw new IllegalArgumentException("You must specify trackerIP, trackerPort and playerName.");
        }

        Player player = new Player(playerName, ip, port);
        Registry registry = null;
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

        GameState gameState = null;
        if(players.size() == 1) {
            System.out.println("Primary Server");
            gameState = new GameState(N, K);
        } else if(players.size() == 2) {
            System.out.println("Backup Server");
            //TODO: GET GAME STATE FROM PRIMARY AND STORE IT
        } else {
            //TODO: GET GAME STATE FROM PRIMARY OR BACKUP SERVER
        }

        //TODO: REMOVE THIS AFTER P2P TRANSFER OF GAME STATE IS AVAILABLE
        if(gameState == null) {
            gameState = new GameState(N, K);
        }

        //FIXME: Need to consider the case when the same position is chosen by multiple nodes (stale game gameState from primary server)
        gameState.initPlayerState(player.name);

        System.out.println(gameState);
        System.out.println("========================  Instructions ======================== ");
        System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");

        // The assignment promises 2 seconds gap between successive crashes.
        // StressTest gives 3 seconds just to be nice.
        long start = System.currentTimeMillis();
        // Start Game
        Game g = new Game(player, gameState);
        System.out.println("Time taken should be less than 3 seconds: " + (System.currentTimeMillis() - start));

        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            String in = input.nextLine();
            switch(in) {
                case "0":
                    g.updateGameState();
                    break;
                case "1":
                    g.move(-1);
                    g.updateGameState();
                    break;
                case "2":
                    g.move(N);
                    g.updateGameState();
                    break;
                case "3":
                    g.move(1);
                    g.updateGameState();
                    break;
                case "4":
                    g.move(-N);
                    g.updateGameState();
                    break;
                case "9":
                    g.gameState.playerStates.remove(g.player.name);
                    g.resolveGameState(g.gameState);
                    try {
                        ITrackerState state = (ITrackerState) registry.lookup("Tracker");
                        state.removePlayer(g.player);
                    } catch (RemoteException | NotBoundException e) {
                        System.err.println("Failed to unregister player from Tracker.");
                    }
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid Input!");
                    System.out.println("========================  Instructions ======================== ");
                    System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");
            }
        }
    }

    //TODO: Add border constraints and update score if target has treasure (what if treasure is already claimed by someone else??)
    private void move(int diff) {
        GameState.PlayerState ps = gameState.playerStates.get(player.name);
        ps.position += diff;

        boolean hasTreasure = false;
        if(hasTreasure)
            ps.score++;
    }

    /**
     * This should send the gameState to the server and receive the updated gameState back.
     */
    private void updateGameState(){
        //TODO: CHANGE THE FUNCTION TO HTTP/SOCKET TO SEND/RECEIVE GAME STATE FROM PRIMARY SERVER
        GameState resolvedGameState = resolveGameState(gameState);
        // this will not fire when gameState has not changed.
        observable.firePropertyChange("gameState", null, resolvedGameState);
        gameState = resolvedGameState;
    }

    /**
     * Only for Primary/Backup Server, receive a GameState and return a GameState back to the caller
     * @return resolvedGameState
     */
    private GameState resolveGameState(GameState gs) {
        //TODO: DO MUTEX AND RESOLVE
        gameState = gs;
        return gameState;
    }

    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return new StringBuilder(2).append((char) (97 + randomInt / 26)).append((char) (97 + randomInt % 26)).toString();
    }
}