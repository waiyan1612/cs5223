import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecondaryThread extends Thread {

    private static ListenerThread listener;
    private static IGameState secondaryStub;

    public SecondaryThread(ListenerThread listenerThread, IGameState gs) {
        listener = listenerThread;
        secondaryStub = gs;
    }

    public void run() {
        GameState gs = null;
        System.out.println("PING");
        try {
            gs = (GameState) listener.getIGameState().getReadOnlyCopy();
        } catch (RemoteException e) {
            System.out.println("Primary Server has went down.");
            try {
                gs = (GameState) secondaryStub.getReadOnlyCopy();
                Player primary = gs.getPrimary();
                Player secondary = gs.getSecondary();
                ITrackerState trackerStub = listener.getITrackerState();
                trackerStub.removePlayer(primary);
                secondaryStub.updatePrimaryServer(secondary);
                listener.setupPrimaryThread(secondaryStub, trackerStub);
                System.out.println("Set " + secondary + " as primary server");
            } catch (IOException e2) {
                System.err.println("SecondaryThread: " + e2.getMessage());
                e.printStackTrace();
            }
        }

        //TODO: Ping other nodes and remove them if they don't respond
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final GameState gs2 = gs;
        executorService.submit(() -> {
            Set<Player> playerSet = gs2.getPlayerStates().keySet();
            List<Player> playerList = new ArrayList<>(playerSet);
            for (int i=0; i<playerSet.size(); i++){
                int primaryPort = gs2.getPrimary().port;
                int secondaryPort = gs2.getSecondary().port;
                Player player = playerList.get(i);
                if (player.port != primaryPort && player.port != secondaryPort){
                    System.out.println("PINGING " + player);
                    boolean success = Game.ping(player.port);
                    if (!success) {
                        System.out.println("PING failed. " + player +" has crashed! Removing the player from the game");
                        try {
                            listener.removePlayer(player);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}