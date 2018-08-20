import java.io.Serializable;

public class Player implements Serializable {
    public String name;
    public String ip;
    public int port;

    public Player(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString(){
        return name + "@" + ip + ":" + port;
    }
}