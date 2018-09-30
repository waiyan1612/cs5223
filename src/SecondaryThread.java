import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

public class SecondaryThread extends Thread {

    private ListenerThread listener;
    private IGameState secondaryStub;

    public SecondaryThread(ListenerThread listener, IGameState secondaryStub) {
        this.listener = listener;
        this.secondaryStub = secondaryStub;
    }

    public void run() {
        GameState gs = null;
        try {
            gs = (GameState) listener.getIGameState().getReadOnlyCopy();
        } catch (RemoteException e) {
            System.out.println("Primary Server has went down.");
        }

        try {
            if(gs == null) {
                gs = (GameState) secondaryStub.getReadOnlyCopy();
                Player primary = gs.getPrimary();
                Player secondary = gs.getSecondary();
                
                ITrackerState trackerStub = listener.getITrackerState();
                trackerStub.removePlayer(primary);
                trackerStub.setPrimaryServerPort(secondary.port);
                secondaryStub.removePrimaryServer(primary);
                secondaryStub.setPrimary(secondary);
                listener.setupPrimaryThread(secondaryStub, trackerStub);
                System.out.println("Set " + secondary.port + " as primary server");
            }

            
        } catch (IOException e) {
            System.err.println("SecondaryThread: " + e.getMessage());
            e.printStackTrace();
        }

        //TODO: Ping other nodes and remove them if they don't respond


    }
}