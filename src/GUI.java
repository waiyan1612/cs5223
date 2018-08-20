import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

public class GUI extends Frame {
    private JLabel[] mapGrids;
    private TextField commandTextField;
    private JLabel infoLabel;

    public GUI (GameState gameState, String playerName) {

        int rows = gameState.N;
        int cols = gameState.N;
        List<Integer> treasurePositions = gameState.treasurePositions;
        Map<String, GameState.PlayerState> playerStates = gameState.playerStates;

        // Info
        Panel legend = new Panel(new FlowLayout());
        StringBuilder sb = new StringBuilder("<html><body>Info of players<br>");
        for (Map.Entry<String, GameState.PlayerState> entry : playerStates.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().score).append("<br>");
        }
        sb.append("</body></html>");
        infoLabel = new JLabel(sb.toString());
        infoLabel.setBorder(BorderFactory.createLineBorder(Color.black));
        infoLabel.setSize(400, 768);
        legend.add(infoLabel);

        // Map
        Panel map = new Panel(new GridLayout(rows, cols));
        mapGrids = new JLabel[rows*cols];
        for(int i=0; i<rows; i++) {
            for(int j=0; j<cols; j++) {
                int pos = i*rows + j;
                Color borderColor = Color.black;
                StringBuilder cell = new StringBuilder("<html><body>").append(pos).append("<br>");
                if(treasurePositions.contains(pos)) {
                    cell.append("*<br>");
                }
                for (Map.Entry<String, GameState.PlayerState> entry : playerStates.entrySet()) {
                    if(entry.getValue().position == pos) {
                        cell.append(entry.getKey()).append("<br>");
                        if(entry.getKey().equals(playerName)) {
                            borderColor = Color.RED;
                        }
                    }
                }
                cell.append("</body></html>");
                mapGrids[pos] = new JLabel(cell.toString());
                mapGrids[pos].setBorder(BorderFactory.createLineBorder(borderColor));
                map.add(mapGrids[i*rows + j]);
            }
        }

        // Command
        Panel command = new Panel(new FlowLayout());
        commandTextField = new TextField("0", 10);
        command.add(commandTextField);

        setLayout(new BorderLayout());
        add(map, BorderLayout.CENTER);
        add(legend, BorderLayout.WEST);
        add(command, BorderLayout.SOUTH);

        setTitle(playerName);
        setSize(1024, 768);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Terminating ...");
                System.exit(0);
            }
        });
    }
}