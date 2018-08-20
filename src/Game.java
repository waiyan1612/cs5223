import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Game {

    public static final boolean DEBUG = true;

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

        IGameState state = null;
        if(players.size() == 1) {
            System.out.println("Primary Server");
            try {
                GameState obj = new GameState(N, K);
                state = (IGameState) UnicastRemoteObject.exportObject(obj, 0);
                registry.bind("GameStatePrimary", state);
                System.err.println("GameStatePrimary ready at " + ip + ":" + port);
            } catch(AlreadyBoundException ae) {
                try {
                    System.err.println("GameStatePrimary is already registered. Rebinding ...");
                    registry.unbind("GameStatePrimary");
                    registry.bind("GameStatePrimary", state);
                    System.err.println("GameStatePrimary ready at " + ip + ":" + port);
                } catch(Exception ee){
                    System.err.println("GameStatePrimary exception: " + ee.getMessage());
                    ee.printStackTrace();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if(players.size() == 2) {
            System.out.println("Backup Server");
            try {
                state = (IGameState) registry.lookup("GameStatePrimary");
                registry.bind("GameStateSecondary", state);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            } catch (AlreadyBoundException e) {
                try {
                    System.err.println("GameStateSecondary is already registered. Rebinding ...");
                    registry.unbind("GameStateSecondary");
                    registry.bind("GameStateSecondary", state);
                    System.err.println("GameStateSecondary ready at " + ip + ":" + port);
                } catch(Exception ee){
                    System.err.println("GameStateSecondary exception: " + ee.getMessage());
                    ee.printStackTrace();
                }
            }
        }

        GameState gs = null;
        try {
            if(state == null) {
                try {
                    state = (IGameState) registry.lookup("GameStatePrimary");
                    gs = (GameState) state.initPlayerState(playerName);
                } catch (RemoteException | NotBoundException e1) {
                    System.err.println("Unable to fetch GameStatePrimary. Trying GameStateSecondary ...");
                    try {
                        state = (IGameState) registry.lookup("GameStateSecondary");
                        gs = (GameState) state.initPlayerState(playerName);
                    } catch (RemoteException | NotBoundException e2) {
                        System.err.println("Unable to fetch GameStatePrimary: " + e1.getMessage());
                        System.err.println("Unable to fetch GameStateSecondary: " + e2.getMessage());
                        System.exit(-1);
                    }
                }
            }
            gs = (GameState) state.initPlayerState(playerName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        System.out.println(gs);
        new GUI(gs, playerName);
    }

    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return new StringBuilder(2).append((char) (97 + randomInt / 26)).append((char) (97 + randomInt % 26)).toString();
    }
}