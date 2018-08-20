import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ITrackerState extends Remote {
    ITrackerState getInfo() throws RemoteException;
    ITrackerState addPlayer(Player p) throws RemoteException;
    ITrackerState removePlayer(Player p) throws RemoteException;
}