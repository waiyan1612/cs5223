import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            Set<Player> playerSet = secondaryStub.getPlayerStates().keySet();
            List<Player> playerList = new ArrayList<>(playerSet);

            for (int i=0; i<playerSet.size(); i++){

                int primaryPort = gs.getPrimary().port;
                int secondaryPort = gs.getSecondary().port;
                Player player = playerList.get(i);
            	if (player.port != primaryPort && player.port != secondaryPort){
            		boolean success = Game.ping(player.port);
                    if (!success) {
                        System.out.println("Player at port " + player.port +" has crashed!");
                        System.out.println("Remove the player from game.");
                        this.listener.removePlayer(player);
                    }
            	}
            }
            
            
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