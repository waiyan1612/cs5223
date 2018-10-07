import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITrackerState extends Remote {
    ITrackerState getReadOnlyCopy() throws RemoteException;
    void addPlayer(Player p) throws RemoteException;
    ITrackerState removePlayer(Player p) throws RemoteException;
}