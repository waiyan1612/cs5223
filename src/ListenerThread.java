import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
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
    public static final String CHECK_TYPE_PRIMARY = "6";
    public static final String IS_PRIMARY = "7";
    public static final String NOT_PRIMARY = "8";
    

    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int NONE = 0;

    private int port;
    private ServerSocket socket;
    private IGameState gameState;
    private ITrackerState trackerState;
    private ScheduledExecutorService executorService;
    private int type;

    public ITrackerState getITrackerState() {
        return trackerState;
    }

    public IGameState getIGameState(){
        return gameState;
    }

    public ListenerThread(int port, IGameState primaryGS, ITrackerState ts, int type) {
        this.port = port;
        this.gameState = primaryGS;
        this.trackerState = ts;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.type = type;

        if(type == PRIMARY) {
            setupPrimaryThread(primaryGS);
        }

        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupPrimaryThread(IGameState primaryGS){
        this.gameState = primaryGS;
        PrimaryThread primaryThread = new PrimaryThread(primaryGS, trackerState);
        type = PRIMARY;
        executorService.scheduleAtFixedRate(primaryThread, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public void setupPrimaryThread(IGameState primaryGS, ITrackerState trackerState) throws RemoteException{
    	type = PRIMARY;
        IGameState primaryStub = (IGameState) UnicastRemoteObject.exportObject(primaryGS.getReadOnlyCopy(), 0);
  
        this.gameState = primaryStub;
        this.gameState.setSecondaryGameState(primaryStub);
        this.trackerState = trackerState;
       
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
                setupSecondary();
            }

            while (true) {
                // Create new thread for each connection
                Socket s = socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                Thread t = new ClientHandlerThread(this, in, out);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("ListenerThread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setupSecondary() throws RemoteException {
        System.out.println("Received request to become secondary server.");
        GameState gs = (GameState) gameState.getReadOnlyCopy();
        Player secondary = null;
        for (Map.Entry<Player, GameState.PlayerState> entry : gs.getPlayerStates().entrySet()) {
            if (entry.getKey().port == port) {
                secondary = entry.getKey();
                break;
            }
        }
        gameState.setSecondary(secondary);
        IGameState secondaryStub = (IGameState) UnicastRemoteObject.exportObject(gameState.getReadOnlyCopy(), 0);
        gameState.setSecondaryGameState(secondaryStub);
        setupSecondaryThread(secondaryStub);
        System.out.println("Player " + secondary + " is now secondary server");
    }

    public static class ClientHandlerThread extends Thread {

        private ListenerThread parent;
        private BufferedReader in;
        private ObjectOutputStream out;
        private IGameState gameState;
        private int type;

        public ClientHandlerThread(ListenerThread parent, BufferedReader in, ObjectOutputStream out) {
            this.parent = parent;
            this.in = in;
            this.out = out;
            this.gameState = parent.gameState;
            this.type = parent.type;
        }

        public void run() {
            try {
                while (true) {
                    String input = in.readLine();
                    if (input != null) {
                        switch(input) {
                            case REQUEST_GAMESTATE:
                                out.writeObject(gameState);
                                break;
                            case REQUEST_PING:
                                out.writeObject(RESPONSE_PING);
                                break;
                            case ASSIGN_SECONDARY:
                                parent.setupSecondary();
                                out.writeObject(SECONDARY_ASSIGNED);
                                break;
                            case CHECK_TYPE_PRIMARY:
                            	out.writeObject(type == PRIMARY ? IS_PRIMARY : NOT_PRIMARY);
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