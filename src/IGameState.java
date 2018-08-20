import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGameState extends Remote {
    IGameState getInfo() throws RemoteException;
    IGameState removeAndAddTreasure(int pos) throws RemoteException;
    IGameState updatePlayerState(String playerName, GameState.PlayerState p) throws RemoteException;
    IGameState initPlayerState(String playerName) throws RemoteException;
}