import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITrackerState extends Remote {
    ITrackerState getInfo() throws RemoteException;
    void addPlayer(Player p) throws RemoteException;
    void removePlayer(Player p) throws RemoteException;
}