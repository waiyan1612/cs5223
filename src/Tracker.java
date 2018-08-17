import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Tracker {

    public static ITrackerState state;

    public static void main(String[] args){

        int port = 1099;
        int N = 10;
        int K = 10;

        if(args.length != 0) {
            if (args.length < 3) {
                throw new IllegalArgumentException("You must specify port number, N and K.");
            }
            port = Integer.parseInt(args[0]);
            N = Integer.parseInt(args[1]);
            K = Integer.parseInt(args[2]);
        }

        Registry registry = null;
        String ip = "";

        try {
            ip = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            TrackerState obj = new TrackerState(N, K);
            state = (ITrackerState) UnicastRemoteObject.exportObject(obj, 0);
            registry = LocateRegistry.getRegistry(port);
            registry.bind("Tracker", state);
            System.err.println("Tracker ready at " + ip + ":" + port);
        } catch(AlreadyBoundException ae) {
            try {
                System.err.println("Tracker is already registered. Rebinding ...");
                registry.unbind("Tracker");
                registry.bind("Tracker", state);
                System.err.println("Tracker ready at " + ip + ":" + port);
            } catch(Exception ee){
                System.err.println("Tracker exception: " + ee.toString());
                ee.printStackTrace();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
