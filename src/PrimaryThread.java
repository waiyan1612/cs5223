import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

public class PrimaryThread extends Thread {

    private IGameState primaryStub;
    private ITrackerState trackerStub;

    public PrimaryThread(IGameState primaryStub,  ITrackerState trackerStub) {
        this.primaryStub = primaryStub;
        this.trackerStub = trackerStub;
        // TODO: Currently the setting of next secondary server when primary server down not working.

    }

    public void run() {
        try {
            // this is fine since this is not a remote call
            GameState gs = (GameState) primaryStub.getReadOnlyCopy();
            Player primary = gs.getPrimary();
            Player secondary = gs.getSecondary();
            if(secondary != null) {
                boolean success = Game.ping(secondary.port);
                if (!success) {
                    System.out.println("Secondary server has crashed!");
                    // remove secondary from tracker
                    trackerStub.removePlayer(secondary);
                    // remove secondary from game
                    primaryStub.removeSecondaryServer(secondary);
                    // get latest list of players from tracker
                    sendRequestToNonPrimary(primary);
                }
            } else {
                sendRequestToNonPrimary(primary);
            }
        } catch (IOException e) {
            System.err.println("PrimaryThread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRequestToNonPrimary(Player primary) throws RemoteException {
        TrackerState tracker = (TrackerState) trackerStub.getReadOnlyCopy();
        List<Player> activePlayers = tracker.players;
        Iterator<Player> iter = activePlayers.iterator();
        boolean assigned = false;
        while (iter.hasNext() && !assigned) {
            Player p = iter.next();
            if(!p.equals(primary)) {
                assigned = Game.assignSecondary(primary.port, p.port);
            }
        }
    }
}