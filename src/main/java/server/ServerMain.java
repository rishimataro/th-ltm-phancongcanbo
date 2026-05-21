package server;

import server.ui.ServerControlFrame;

import javax.swing.*;

public class ServerMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerControlFrame frame = new ServerControlFrame();
            frame.setVisible(true);
        });
    }
}
