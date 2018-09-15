import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.List;

public class Game {

    private Player player;
    private PropertyChangeSupport observable;

    public Game(Player p, GameState gs) {
        player = p;
        GUI gui = new GUI(gs, player.name);
        observable = new PropertyChangeSupport(this);
        observable.addPropertyChangeListener(gui);
    }

    public static void main(String[] args) {

        String trackerIp = null;
        int trackerPort = 0;
        String playerName;

        if (args.length == 3) {
            trackerIp = args[0];
            trackerPort = Integer.parseInt(args[1]);
            playerName = args[2];
        } else if (args.length == 0) {
            playerName = createID();
        } else {
            throw new IllegalArgumentException("You must specify trackerIP, trackerPort and playerName.");
        }

        Player player = new Player(playerName);
        Registry registry;
        ITrackerState trackerStub = null;
        TrackerState tracker = null;
        try {
            registry = LocateRegistry.getRegistry(trackerIp, trackerPort);
            trackerStub = (ITrackerState) registry.lookup("Tracker");
            trackerStub.addPlayer(player);
            tracker = (TrackerState) trackerStub.getReadOnlyCopy();
            if (tracker == null) {
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
        for (Player p : players) {
            if (p.name.equals(player.name)) {
                player.setPort(p.port);
                break;
            }
        }
        //TODO: Recreate game state if primary goes down
        //TODO: Ping every 0.5s to detect crashed nodes
        //                if (!ping(players.get(0).port)) {
        //                    System.out.println(players.get(0) + " has crashed!");
        //                    trackerStub.removePlayer(players.get(0));
        //                }
        IGameState stub = null;
        try {
            if (players.size() == 1) {
                System.out.println("Primary Server");
                GameState gameState = new GameState(N, K);
                gameState.setPrimary(player.port);

                // Creating GameState Stub and serving it via Listener Thread
                ServerSocket serverSocket = new ServerSocket(player.port);
                stub = (IGameState) UnicastRemoteObject.exportObject(gameState, 0);
                ListenerThread listener = new ListenerThread(serverSocket, stub);
                listener.start();

            } else if (players.size() == 2) {
                System.out.println("Backup Server");
                stub = getStub(players.get(0).port);
                stub.setSecondary(player.port);

                // Creating GameState Stub and serving it via Listener Thread
                ServerSocket serverSocket = new ServerSocket(player.port);
                ListenerThread listener = new ListenerThread(serverSocket, stub);
                listener.start();
            } else {
                stub = getStub(players.get(0).port);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        GameState gs = null;
        try {
            stub.initPlayerState(player.name);
            gs = (GameState) stub.getReadOnlyCopy();
            System.out.println();
            System.out.println("========================  Instructions ======================== ");
            System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");
        } catch (RemoteException e) {
            System.err.println("Failed to init player state");
            System.exit(-1);
        }

        // The assignment promises 2 seconds gap between successive crashes.
        // StressTest gives 3 seconds just to be nice.
        long start = System.currentTimeMillis();
        Game g = new Game(player, gs);
        System.out.println("Time taken should be less than 3 seconds: " + (System.currentTimeMillis() - start));

        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            try {
                String in = input.nextLine();
                switch (in) {
                    case "0":
                        g.updateGameState(stub);
                        break;
                    case "1":
                        stub.move(player, -1);
                        g.updateGameState(stub);
                        break;
                    case "2":
                        stub.move(player, N);
                        g.updateGameState(stub);
                        break;
                    case "3":
                        stub.move(player, 1);
                        g.updateGameState(stub);
                        break;
                    case "4":
                        stub.move(player, -N);
                        g.updateGameState(stub);
                        break;
                    case "9":
                        stub.getPlayerStates().remove(g.player.name);
                        trackerStub.removePlayer(g.player);
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid Input!");
                        System.out.println("========================  Instructions ======================== ");
                        System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");
                }
            } catch (RemoteException e) {
                System.err.println("Failed to fetch GameState: " + e.getMessage());
            }
        }
    }

    private void updateGameState(IGameState stub) throws RemoteException {
        GameState gs = (GameState) stub.getReadOnlyCopy();
        observable.firePropertyChange("gameState", null, gs);
    }

    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return "" + (char) (97 + randomInt / 26) + ((char) (97 + randomInt % 26));
    }

    private static IGameState getStub(int port) throws IOException, ClassNotFoundException {
        return getStub(null, port);
    }

    private static IGameState getStub(String ip, int port) throws IOException, ClassNotFoundException {
        IGameState stub;
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(ListenerThread.REQUEST_GAMESTATE);
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                stub = (IGameState) ois.readObject();
            }
        }
        return stub;
    }

    private static boolean ping(int port) {
        return ping(null, port);
    }

    private static boolean ping(String ip, int port) {
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(ListenerThread.REQUEST_PING);
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                String resp = (String) ois.readObject();
                return resp.equals(ListenerThread.RESPONSE_PING);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

}