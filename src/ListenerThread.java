import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ListenerThread extends Thread {

    public static final String REQUEST_GAMESTATE = "1";
    public static final String REQUEST_PING = "2";
    public static final String RESPONSE_PING = "3";

    private ServerSocket socket;
    private IGameState state;

    public ListenerThread(ServerSocket socket, IGameState state) {
        this.socket = socket;
        this.state = state;
    }

    public void run() {
        try {
            while (true) {
                // Create new thread for each connection
                Socket s = socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                Thread t = new ClientHandlerThread(s, state, in, out);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("ListenerThread:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class ClientHandlerThread extends Thread {

        private Socket socket;
        private IGameState state;
        private BufferedReader in;
        private ObjectOutputStream out;

        public ClientHandlerThread(Socket socket, IGameState state, BufferedReader in, ObjectOutputStream out) {
            this.socket = socket;
            this.state = state;
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                while (true) {
                    String input = in.readLine();
                    if (input != null) {
                        System.out.println(input);
                        if (input.equals(REQUEST_GAMESTATE)) {
                            out.writeObject(state);
                            break;
                        } else if (input.equals(REQUEST_PING)) {
                            out.writeObject(RESPONSE_PING);
                            break;
                        }
                    }
                }
                socket.close();
            } catch(IOException e) {
                System.err.println("ClientHandlerThread:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}