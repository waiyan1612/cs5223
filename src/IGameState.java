import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface IGameState extends Remote {
    void setSecondaryGameState(IGameState state) throws RemoteException;
    void updateAll(Set<Integer>  treasurePositions, Map<Player, GameState.PlayerState> playerStates,
                   Player primary, Player secondary) throws RemoteException;
    int getN() throws RemoteException;
    Player getPrimary() throws RemoteException;
    Player getSecondary() throws RemoteException;
    IGameState getReadOnlyCopy() throws RemoteException;
    void setPrimary(Player p) throws RemoteException;
    void setSecondary(Player p) throws RemoteException;
    IGameState initPlayerState(Player p) throws RemoteException;
    void move(Player player, int diff) throws RemoteException;
    void removePlayer(Player p) throws RemoteException;
    void updatePrimaryServer(Player p) throws RemoteException;
    void removeSecondaryServer(Player p) throws RemoteException;
    Set<Integer> getTreasurePositions() throws RemoteException;
    Map<Player, GameState.PlayerState> getPlayerStates() throws RemoteException;
    void createTreasures() throws RemoteException;
    void removeTreasures(int position) throws RemoteException;
}