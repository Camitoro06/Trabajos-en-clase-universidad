package com.icesi.buscaminas.client;

import com.icesi.buscaminas.protocol.Protocol;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BuscaminasSwingClient extends JFrame {
    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final JLabel statusLabel = new JLabel("Conectando...");
    private final JPanel boardPanel = new JPanel();
    private JButton[][] cells = new JButton[0][0];
    private int rows;
    private int cols;

    public BuscaminasSwingClient(String host, int port) {
        super("Buscaminas TCP");
        this.host = host;
        this.port = port;

        buildWindow();
        connect();
    }

    private void buildWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        top.add(statusLabel, BorderLayout.CENTER);

        JButton newGameButton = new JButton("Nueva partida");
        newGameButton.addActionListener(e -> sendCommandAsync("NUEVO"));
        top.add(newGameButton, BorderLayout.EAST);

        JLabel help = new JLabel("Click izquierdo: abrir | Click derecho: bandera");
        help.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        add(top, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(help, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(520, 580));
        setLocationRelativeTo(null);
    }

    private void connect() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                socket = new Socket(host, port);
                socket.setSoTimeout(15_000);
                socket.setTcpNoDelay(true);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                return readResponse();
            }

            @Override
            protected void done() {
                try {
                    applyResponse(get());
                } catch (Exception e) {
                    showConnectionError(e);
                }
            }
        }.execute();
    }

    private void sendCommandAsync(String command) {
        if (out == null || in == null) {
            return;
        }

        setBoardEnabled(false);
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                synchronized (BuscaminasSwingClient.this) {
                    out.println(command);
                    return readResponse();
                }
            }

            @Override
            protected void done() {
                try {
                    applyResponse(get());
                } catch (Exception e) {
                    showConnectionError(e);
                } finally {
                    setBoardEnabled(true);
                }
            }
        }.execute();
    }

    private List<String> readResponse() throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if (Protocol.END.equals(line)) {
                return lines;
            }
            lines.add(line);
        }
        throw new IOException("El servidor cerro la conexion.");
    }

    private void applyResponse(List<String> lines) {
        String status = "PLAYING";
        int newRows = rows;
        int newCols = cols;
        int boardMarker = -1;
        String message = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("STATUS ")) {
                status = line.substring("STATUS ".length()).trim();
            } else if (line.startsWith("SIZE ")) {
                String[] parts = line.split("\\s+");
                newRows = Integer.parseInt(parts[1]);
                newCols = Integer.parseInt(parts[2]);
            } else if (line.equals("BOARD")) {
                boardMarker = i;
            } else if ((line.startsWith("OK ") || line.startsWith("ERROR ") || line.startsWith("BYE ")) && message == null) {
                message = line;
            }
        }

        if (newRows > 0 && newCols > 0 && (newRows != rows || newCols != cols)) {
            rebuildBoard(newRows, newCols);
        }

        if (boardMarker >= 0) {
            updateBoard(lines.subList(boardMarker + 1, lines.size()));
        }

        statusLabel.setText("Servidor " + host + ":" + port + " | Estado: " + translateStatus(status));
        if (message != null && message.startsWith("ERROR ")) {
            JOptionPane.showMessageDialog(this, message.substring(6), "Buscaminas", JOptionPane.WARNING_MESSAGE);
        }
        if ("WON".equals(status)) {
            JOptionPane.showMessageDialog(this, "Ganaste 🎉", "Buscaminas", JOptionPane.INFORMATION_MESSAGE);
        } else if ("LOST".equals(status)) {
            JOptionPane.showMessageDialog(this, "Pisaste una mina. Fin de la partida.", "Buscaminas", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void rebuildBoard(int newRows, int newCols) {
        this.rows = newRows;
        this.cols = newCols;
        this.cells = new JButton[rows][cols];

        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(rows, cols, 2, 2));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        Font cellFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, Math.min(20, 280 / Math.max(rows, cols))));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int row = r;
                final int col = c;
                JButton button = new JButton("#");
                button.setFont(cellFont);
                button.setFocusPainted(false);
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            sendCommandAsync("BANDERA " + row + " " + col);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            sendCommandAsync("ABRIR " + row + " " + col);
                        }
                    }
                });
                cells[r][c] = button;
                boardPanel.add(button);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
        pack();
        setSize(Math.max(520, cols * 45), Math.max(580, rows * 45 + 100));
        setLocationRelativeTo(null);
    }

    private void updateBoard(List<String> boardLines) {
        for (String line : boardLines) {
            int firstBar = line.indexOf('|');
            int lastBar = line.lastIndexOf('|');
            if (firstBar < 0 || lastBar <= firstBar) {
                continue;
            }

            String rowText = line.substring(0, firstBar).trim();
            int row;
            try {
                row = Integer.parseInt(rowText);
            } catch (NumberFormatException e) {
                continue;
            }
            if (row < 0 || row >= rows) {
                continue;
            }

            String content = line.substring(firstBar + 1, lastBar);
            for (int col = 0; col < cols; col++) {
                int symbolIndex = col * 3 + 1;
                if (symbolIndex >= content.length()) {
                    break;
                }
                char symbol = content.charAt(symbolIndex);
                JButton button = cells[row][col];
                button.setText(symbol == ' ' ? "" : String.valueOf(symbol));
                button.setEnabled(symbol == '#' || symbol == 'F');
            }
        }
    }

    private void setBoardEnabled(boolean enabled) {
        for (JButton[] row : cells) {
            for (JButton button : row) {
                if (button.getText().equals("#") || button.getText().equals("F")) {
                    button.setEnabled(enabled);
                }
            }
        }
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "WON" -> "GANASTE";
            case "LOST" -> "PERDISTE";
            default -> "JUGANDO";
        };
    }

    private void showConnectionError(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        statusLabel.setText("Sin conexion");
        JOptionPane.showMessageDialog(this,
                "No fue posible comunicarse con el servidor:\n" + cause.getMessage(),
                "Error de red",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;
        SwingUtilities.invokeLater(() -> new BuscaminasSwingClient(host, port).setVisible(true));
    }
}
