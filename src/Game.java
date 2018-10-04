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
        ListenerThread listener;
        try {
            if (players.size() == 1) {
                System.out.println("Primary Server");
                GameState gameState = new GameState(N, K);
                gameState.setPrimary(player);

                // Creating GameState Stub and serving it via Listener Thread
                stub = (IGameState) UnicastRemoteObject.exportObject(gameState, 0);
                gs = (GameState) stub.initPlayerState(player);
                listener = new ListenerThread(player.port, stub, trackerStub, ListenerThread.PRIMARY);
                listener.start();
            } else if (players.size() == 2) {
                System.out.println("Backup Server");
                stub = getStub(trackerStub, player);
                gs = tryInitPlayerState(trackerStub, stub, player);
                listener = new ListenerThread(player.port, stub, trackerStub, ListenerThread.SECONDARY);
                listener.start();
            } else {
                stub = getStub(trackerStub, player);
                gs = tryInitPlayerState(trackerStub, stub, player);
                listener = new ListenerThread(player.port, stub, trackerStub, ListenerThread.NONE);
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
        acquireAndListen(stub, trackerStub, g, player, N, gs);

    }

    private static GameState tryInitPlayerState(ITrackerState trackerStub, IGameState stub, Player player){
        if(stub == null) {
            System.err.println("Unable to find a stub recursively.");
            return null;
        }
        try {
            return (GameState) stub.initPlayerState(player);
        } catch (RemoteException e) {
            stub = getStub(trackerStub, player);
            return tryInitPlayerState(trackerStub, stub, player);
        }
    }

    private static void acquireAndListen(IGameState stub, ITrackerState trackerStub, Game g, Player player, int N, GameState gs) {
        try {
            listenUserInput(stub, trackerStub, g, player, N);
        } catch (RemoteException | NullPointerException e) {
            System.err.println("Failed to fetch GameState: " + e.getMessage());
            System.err.println("Primary Server Failed"+ g.gs.getSecondary());
            
            int primaryServerPort = 0;
            primaryServerPort = g.gs.getSecondary().port;
            boolean notConnected = true;
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            while(notConnected && currentTime - startTime < 2000) {
                currentTime = System.currentTimeMillis();
                try {
                    System.out.println("Port "+ primaryServerPort + " acts as primary server now.");
                    stub = getStub(primaryServerPort);
                    gs = (GameState) stub.getReadOnlyCopy();
                    if(gs != null) {
                        acquireAndListen(stub, trackerStub, g, player, N, gs);
                        notConnected = false;
                    }
                } catch (IOException | NullPointerException ex) {

                }
            }
            System.out.print("Time done" + (currentTime - startTime));

            stub = getStub(trackerStub, player);
            System.out.println("Connecting to new server");
            acquireAndListen(stub, trackerStub, g, player, N, gs);
        }
    }

    private static void listenUserInput(IGameState stub, ITrackerState trackerStub, Game g, Player player, int N) throws RemoteException{
        Scanner input = new Scanner(System.in);
        g.updateGameState(stub);
        while (input.hasNext()) {
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
                    stub.removePlayer(g.player);
                    trackerStub.removePlayer(g.player);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid Input!");
                    System.out.println("========================  Instructions ======================== ");
                    System.out.println("                                                     4  \n0 to refresh, 9 to exit. Directional controls are: 1   3\n                                                     2  ");
            }
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
        IGameState stub = null;
        try {
            TrackerState tracker = (TrackerState) trackerStub.getReadOnlyCopy();
            Iterator<Player> iter = tracker.players.iterator();
            System.out.println(tracker.players);
            while(stub == null || !iter.hasNext()) {
                Player p = iter.next();
                if(!p.equals(currentPlayer)) {
                    try {
                        stub = getStub(p.port);
                    } catch (IOException e) {
                        System.err.println("Failed to get stub from " + p + ": " + e.getMessage());
                        //Doing this has some issues
                        //System.err.println("Removing " + p + " from list of players.");
                        //trackerStub.removePlayer(p);
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return stub;
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