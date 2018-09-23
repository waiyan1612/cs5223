import java.io.IOException;
import java.rmi.RemoteException;
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

        if(gs == null) {
            //TODO: THINGS TO HANDLE WHEN SECONDARY BECOMES PRIMARY
            listener.setupPrimaryThread(secondaryStub);
        }

        //TODO: Ping other nodes and remove them if they don't respond


    }
}