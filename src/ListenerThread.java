import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ListenerThread extends Thread {

    public static final long PING_INTERVAL = 500;

    public static final String REQUEST_GAMESTATE = "1";
    public static final String REQUEST_PING = "2";
    public static final String RESPONSE_PING = "3";
    public static final String ASSIGN_SECONDARY = "4";
    public static final String SECONDARY_ASSIGNED = "5";
    

    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int NONE = 0;

    private Player player;
    private ServerSocket socket;
    private static IGameState gameState;
    private static ITrackerState trackerState;
    private ScheduledExecutorService executorService;
    private int type;

    public ITrackerState getITrackerState() {
        return trackerState;
    }

    public IGameState getIGameState(){
        return gameState;
    }

    public void setIGameState(IGameState gs){
        gameState = gs;
    }
    
    public void removePlayer(Player player) throws RemoteException{
    	gameState.removePlayer(player);
    	trackerState.removePlayer(player);
    }

    public ListenerThread(Player player, IGameState primaryGS, ITrackerState ts, int type) {
        this.player = player;
        gameState = primaryGS;
        trackerState = ts;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.type = type;

        if(type == PRIMARY) {
            setupPrimaryThread(primaryGS);
        }

        try {
            socket = new ServerSocket(player.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupPrimaryThread(IGameState primaryGS){
        gameState = primaryGS;
        PrimaryThread primaryThread = new PrimaryThread(primaryGS, trackerState);
        type = PRIMARY;
        executorService.scheduleAtFixedRate(primaryThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public void setupPrimaryThread(IGameState secondaryStub, ITrackerState ts) throws RemoteException{
    	type = PRIMARY;
        GameState gs = (GameState) secondaryStub.getReadOnlyCopy();
        IGameState primaryStub = (IGameState) UnicastRemoteObject.exportObject(gs, 0);

        gameState = primaryStub;
        gameState.setSecondaryGameState(primaryStub);
        trackerState = ts;
       
        PrimaryThread primaryThread = new PrimaryThread(primaryStub, trackerState);
        executorService.shutdown();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(primaryThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void setupSecondaryThread(IGameState secondaryGS){
    	type = SECONDARY;
        SecondaryThread secondaryThread = new SecondaryThread(this, secondaryGS);
        executorService.scheduleAtFixedRate(secondaryThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void run() {
        try {
            if(type == SECONDARY) {
                setupSecondary(-1);
            }

            while (true) {
                // Create new thread for each connection
                Socket s = socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                Thread t = new ClientHandlerThread(this, in, out);
                t.start();
                try {
                    sleep(100);
                } catch(Exception e) {

                }
            }
        } catch (IOException e) {
            System.err.println("ListenerThread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setupSecondary(int from) throws RemoteException {
        System.out.println("Received request to become secondary server from " + from);
        if (from != -1) {
            try {
                gameState = Game.getStub(from);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GameState gs = (GameState) gameState.setSecondary(player);
        IGameState secondaryStub = (IGameState) UnicastRemoteObject.exportObject(gs, 0);
        System.out.println("Made it in time ..");
        try {
            gameState.setSecondaryGameState(secondaryStub);
            setupSecondaryThread(secondaryStub);
        } catch (RemoteException e) {
            System.err.println("I need to become primary RIGHT NOW");
            Player primary = gs.getPrimary();
            Player secondary = gs.getSecondary();
            trackerState.removePlayer(primary);
            secondaryStub.updatePrimaryServer(secondary);
            setupPrimaryThread(secondaryStub, trackerState);
        }


        System.out.println("Player " + player + " is now secondary server");
    }

    public static class ClientHandlerThread extends Thread {

        private ListenerThread parent;
        private BufferedReader in;
        private ObjectOutputStream out;

        public ClientHandlerThread(ListenerThread parent, BufferedReader in, ObjectOutputStream out) {
            this.parent = parent;
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                while (true) {
                    try {
                        sleep(100);
                    } catch(Exception e) {

                    }
                    String input = in.readLine();
                    if (input != null) {
                        switch(input) {
                            case REQUEST_GAMESTATE:
                                out.writeObject(gameState);
                                break;
                            case REQUEST_PING:
                                out.writeObject(RESPONSE_PING);
                                break;
                        }
                        if(input.startsWith(ASSIGN_SECONDARY)) {
                            int port = Integer.valueOf(input.split("_")[1]);
                            parent.setupSecondary(port);
                            out.writeObject(SECONDARY_ASSIGNED);
                            break;
                        }
                    }
                }
            } catch(SocketException e) {
                System.err.println("ClientHandlerThread SocketException: " + e.getMessage());
            } catch(IOException e) {
                System.err.println("ClientHandlerThread: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}