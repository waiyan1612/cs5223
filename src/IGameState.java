import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface IGameState extends Remote {
    int getN() throws RemoteException;
    int getPrimary() throws RemoteException;
    int getSecondary() throws RemoteException;
    IGameState getReadOnlyCopy() throws RemoteException;
    void setPrimary(int port) throws RemoteException;
    void setSecondary(int port) throws RemoteException;
    void initPlayerState(String playerName) throws RemoteException;
    void move(Player player, int diff) throws RemoteException;
    Set<Integer> getTreasurePositions() throws RemoteException;
    Map<String, GameState.PlayerState> getPlayerStates() throws RemoteException;
    void createTreasures() throws RemoteException;
    void removeTreasures(int position) throws RemoteException;
}