import java.beans.PropertyChangeSupport;
import java.io.*;
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
    private GameState gs;
    private static String trackerIp = null;
    private static int trackerPort = 0;

    public Game(Player p, GameState gs) {
        player = p;
        GUI gui = new GUI(gs, player.name);
        observable = new PropertyChangeSupport(this);
        observable.addPropertyChangeListener(gui);
        this.gs = gs;
    }

    public static void main(String[] args) {

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
        IGameState stub = null;
        GameState gs = null;
        ListenerThread listener = null;
        try {
            if (players.size() == 1) {
                System.out.println("Primary Server");
                GameState gameState = new GameState(N, K);
                gameState.setPrimary(player);

                // Creating GameState Stub and serving it via Listener Thread
                stub = (IGameState) UnicastRemoteObject.exportObject(gameState, 0);
                gs = (GameState) stub.initPlayerState(player);
                listener = new ListenerThread(player, stub, trackerStub, ListenerThread.PRIMARY);
                listener.start();
            } else if (players.size() == 2) {
                System.out.println("Backup Server");
                stub = getStub(trackerStub, player);
                gs = tryInitPlayerState(trackerStub, stub, player, 10);
                listener = new ListenerThread(player, stub, trackerStub, ListenerThread.SECONDARY);
                listener.start();
            } else {
                stub = getStub(trackerStub, player);
                gs = tryInitPlayerState(trackerStub, stub, player, 10);
                listener = new ListenerThread(player, stub, trackerStub, ListenerThread.NONE);
                listener.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println();
        System.out.println("========================  Instructions ======================== ");
        System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");

        // The assignment promises 2 seconds gap between successive crashes.
        // StressTest gives 3 seconds just to be nice.
        long start = System.currentTimeMillis();
        Game g = new Game(player, gs);
        System.out.println("Time taken should be less than 3 seconds: " + (System.currentTimeMillis() - start));
        acquireAndListen(listener, g, null);
    }

    private static GameState tryInitPlayerState(ITrackerState trackerStub, IGameState stub, Player player, int level){
        if(level == 0) {
            System.err.println("Unable to find a stub recursively.");
            return null;
        }
        try {
            return (GameState) stub.initPlayerState(player);
        } catch (RemoteException | NullPointerException e) {
            stub = getStub(trackerStub, player);
            return tryInitPlayerState(trackerStub, stub, player, level-1);
        }
    }

    private static void acquireAndListen(ListenerThread listener, Game g, String prevUserInput) {
        IGameState stub = listener.getIGameState();
        ITrackerState trackerStub = listener.getITrackerState();
        try {
            listenUserInput(stub, trackerStub, g, prevUserInput);
        } catch (GameException e) {
            System.err.println("Failed to fetch GameState: " + e.getMessage());
            String prevInput = e.userInput;
            try {
                Player secondary = g.gs.getSecondary();
                if(secondary != null) {
                    stub = getStub(secondary.port);
                    g.updateGameState(stub);
                    System.out.println("Refreshed acquiredAndListen from secondary");
                    acquireAndListen(listener, g, prevInput);
                } else {
                    throw new IOException("Secondary is null in local GameState");
                }
            } catch (IOException ee) {
                try {
                    stub = listener.getIGameState();
                    g.updateGameState(stub);
                    System.out.println("Refreshed acquiredAndListen from self");
                    acquireAndListen(listener, g, prevInput);
                } catch (RemoteException e1) {
                    try {
                        stub = getStub(listener.getITrackerState(), g.player);
                        GameState gs = (GameState) stub.getReadOnlyCopy();
                        listener.setIGameState(gs);
                        g.updateGameState(stub);
                        System.out.println("Refreshed acquiredAndListen");
                        acquireAndListen(listener, g, prevInput);
                    } catch (RemoteException | NullPointerException e2) {
                        System.out.println("Failed to refresh acquiredAndListen. Trying again: " + e.getMessage());
                        acquireAndListen(listener, g, prevInput);
                    }
                }
            }
        }
    }

    private static void listenUserInput(IGameState stub, ITrackerState trackerStub, Game g, String prevInput) throws GameException {
        Scanner input = new Scanner(System.in);
        while (prevInput != null || input.hasNext()) {
            String in;
            if(prevInput != null) {
                in = prevInput;
                prevInput = null;
            } else {
                in = input.nextLine();
            }
            try {
                switch (in) {
                    case "0":
                        g.updateGameState(stub);
                        break;
                    case "1":
                        stub.move(g.player, -1);
                        g.updateGameState(stub);
                        break;
                    case "2":
                        stub.move(g.player, g.gs.N);
                        g.updateGameState(stub);
                        break;
                    case "3":
                        stub.move(g.player, 1);
                        g.updateGameState(stub);
                        break;
                    case "4":
                        stub.move(g.player, -g.gs.N);
                        g.updateGameState(stub);
                        break;
                    case "9":
                        stub.removePlayer(g.player);
                        trackerStub.removePlayer(g.player);
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid Input!");
                        System.out.println("========================  Instructions ======================== ");
                        System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");
                }
            } catch(RemoteException | NullPointerException e) {
                throw new GameException(in, e);
            }
        }
    }

    public static class GameException extends Exception {
        public String userInput;
        public GameException(String input, Exception e) {
            super(e.getMessage(), e);
            this.userInput = input;
        }
    }

    private void updateGameState(IGameState stub) throws RemoteException {
        GameState gs = (GameState) stub.getReadOnlyCopy();
        if (gs != null) this.gs = gs;
        observable.firePropertyChange("gameState", null, gs);
    }

    private static String createID() {
        Random r = new Random();
        int randomInt = r.nextInt(26) + 1;
        return "" + (char) (97 + randomInt / 26) + ((char) (97 + randomInt % 26));
    }

    private static IGameState getStub(ITrackerState trackerStub, Player currentPlayer) {
        IGameState stub;
            int round = 4;
            while (round > 0) {
                try {
                    TrackerState tracker = (TrackerState) trackerStub.getReadOnlyCopy();
                    Collections.shuffle(tracker.players);
                    Iterator<Player> iter = tracker.players.iterator();
                    boolean delete = false;
                    while (iter.hasNext()) {
                        Player p = iter.next();
                        if (delete) {
                            iter.remove();
                        }
                        if (!p.equals(currentPlayer)) {
                            try {
                                System.err.println("Trying to get stub from " + p);
                                stub = getStub(p.port);
                                if (stub != null) {
                                    return stub;
                                } else {
                                    System.err.println("Why am i getting  null");
                                }
                            } catch (IOException e) {
                                System.err.println("Failed to get stub from " + p + ": " + e.getMessage());
                                if (round == 2) {
                                    System.err.println("Removing " + p + " from list of players.");
                                    trackerStub.removePlayer(p);
                                    delete = true;
                                } else {
                                    delete = false;
                                }
                            }
                        }
                    }
                    round--;

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        return null;
    }

    public static IGameState getStub(int port) throws IOException {
        String ip = null;
        IGameState stub = null;
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(ListenerThread.REQUEST_GAMESTATE);
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                stub = (IGameState) ois.readObject();
            } catch (ClassNotFoundException e) {
                System.err.println("This should never happen!");
            }
        }
        return stub;
    }


    public static boolean ping(int port) {
        String ip = null;
        try (Socket socket = new Socket(ip, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(ListenerThread.REQUEST_PING);
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                String resp = (String) ois.readObject();
                return resp.equals(ListenerThread.RESPONSE_PING);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Exception while pinging "+ port + ": " + e.getMessage());
        }
        return false;
    }

    public static boolean assignSecondary(int from, int to) {
        String ip = null;
        try (Socket socket = new Socket(ip, to)) {
            System.out.println("Sending ASSIGN_SECONDARY msg to "+ to);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(ListenerThread.ASSIGN_SECONDARY + "_" + from);
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                String resp = (String) ois.readObject();
                return resp.equals(ListenerThread.SECONDARY_ASSIGNED);
            }
        } catch (IOException  | ClassNotFoundException e) {
            System.err.println("Exception while sending ASSIGN_SECONDARY msg to "+ to + ": " + e.getMessage());
        }
        return false;
    }
}