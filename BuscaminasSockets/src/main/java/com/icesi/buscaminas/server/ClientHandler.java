package com.icesi.buscaminas.server;

import com.icesi.buscaminas.game.ActionResult;
import com.icesi.buscaminas.game.MinesweeperGame;
import com.icesi.buscaminas.protocol.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Locale;

public class ClientHandler implements Runnable {
    private static final int CLIENT_TIMEOUT_MS = 5 * 60 * 1000;
    private final Socket socket;
    private MinesweeperGame game = new MinesweeperGame(9, 9, 10);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket client = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            client.setSoTimeout(CLIENT_TIMEOUT_MS);
            System.out.println("Cliente conectado: " + client.getRemoteSocketAddress()
                    + " | hilo=" + Thread.currentThread().getName());

            sendWelcome(out);

            String line;
            while ((line = in.readLine()) != null) {
                boolean keepConnected = processCommand(line, out);
                if (!keepConnected) {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Cliente desconectado por inactividad: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("Error atendiendo cliente " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        } finally {
            System.out.println("Sesion finalizada: " + socket.getRemoteSocketAddress());
        }
    }

    private boolean processCommand(String rawCommand, PrintWriter out) {
        String commandLine = rawCommand.trim();
        if (commandLine.isEmpty()) {
            sendResponse(out, "ERROR Comando vacio.");
            return true;
        }

        String[] parts = commandLine.split("\\s+");
        String command = normalizeCommand(parts[0]);

        try {
            switch (command) {
                case "HELP" -> sendHelp(out);
                case "NEW" -> newGame(parts, out);
                case "OPEN" -> openCell(parts, out);
                case "FLAG" -> toggleFlag(parts, out);
                case "BOARD" -> sendGameState(out, "Tablero actual.");
                case "STATUS" -> sendResponse(out, "STATUS " + game.getStatus());
                case "QUIT" -> {
                    sendResponse(out, "BYE Gracias por jugar.");
                    return false;
                }
                default -> sendResponse(out, "ERROR Comando desconocido. Escribe AYUDA.");
            }
        } catch (NumberFormatException e) {
            sendResponse(out, "ERROR Filas, columnas, minas y coordenadas deben ser numeros enteros.");
        } catch (IllegalArgumentException e) {
            sendResponse(out, "ERROR " + e.getMessage());
        } catch (Exception e) {
            sendResponse(out, "ERROR No se pudo procesar el comando: " + e.getMessage());
        }
        return true;
    }

    private void newGame(String[] parts, PrintWriter out) {
        if (parts.length == 1) {
            game = new MinesweeperGame(9, 9, 10);
        } else if (parts.length == 4) {
            int rows = Integer.parseInt(parts[1]);
            int cols = Integer.parseInt(parts[2]);
            int mines = Integer.parseInt(parts[3]);
            game = new MinesweeperGame(rows, cols, mines);
        } else {
            sendResponse(out, "ERROR Uso: NUEVO [filas columnas minas]");
            return;
        }
        sendGameState(out, "Nueva partida creada: " + game.getRows() + "x" + game.getCols()
                + " con " + game.getMineCount() + " minas.");
    }

    private void openCell(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            sendResponse(out, "ERROR Uso: ABRIR fila columna");
            return;
        }
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        ActionResult result = game.openCell(row, col);
        sendGameState(out, (result.success() ? "OK " : "ERROR ") + result.message());
    }

    private void toggleFlag(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            sendResponse(out, "ERROR Uso: BANDERA fila columna");
            return;
        }
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        ActionResult result = game.toggleFlag(row, col);
        sendGameState(out, (result.success() ? "OK " : "ERROR ") + result.message());
    }

    private void sendWelcome(PrintWriter out) {
        out.println("OK BUSCAMINAS TCP - SESION INICIADA");
        out.println("Cada cliente tiene una partida independiente.");
        out.println("Escribe AYUDA para ver los comandos.");
        out.println("STATUS " + game.getStatus());
        out.println("SIZE " + game.getRows() + " " + game.getCols() + " " + game.getMineCount());
        out.println("BOARD");
        printMultiline(out, game.renderBoard());
        out.println(Protocol.END);
    }

    private void sendHelp(PrintWriter out) {
        out.println("COMANDOS");
        out.println("NUEVO                         -> tablero 9x9 con 10 minas");
        out.println("NUEVO filas columnas minas    -> tablero personalizado (max. 30x30)");
        out.println("ABRIR fila columna            -> abre una casilla");
        out.println("BANDERA fila columna          -> pone o quita una bandera");
        out.println("TABLERO                       -> muestra el tablero");
        out.println("ESTADO                        -> muestra PLAYING, WON o LOST");
        out.println("AYUDA                         -> muestra esta ayuda");
        out.println("SALIR                         -> cierra la conexion");
        out.println("Simbolos: # oculta | F bandera | * mina | 1..8 minas vecinas");
        out.println(Protocol.END);
    }

    private void sendGameState(PrintWriter out, String message) {
        out.println(message);
        out.println("STATUS " + game.getStatus());
        out.println("SIZE " + game.getRows() + " " + game.getCols() + " " + game.getMineCount());
        out.println("BOARD");
        printMultiline(out, game.renderBoard());
        out.println(Protocol.END);
    }

    private void sendResponse(PrintWriter out, String message) {
        out.println(message);
        out.println(Protocol.END);
    }

    private void printMultiline(PrintWriter out, String text) {
        for (String line : text.split("\\R")) {
            out.println(line);
        }
    }

    private String normalizeCommand(String command) {
        String upper = command.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "AYUDA" -> "HELP";
            case "NUEVO", "NUEVA" -> "NEW";
            case "ABRIR" -> "OPEN";
            case "BANDERA", "MARCAR" -> "FLAG";
            case "TABLERO" -> "BOARD";
            case "ESTADO" -> "STATUS";
            case "SALIR", "FIN" -> "QUIT";
            default -> upper;
        };
    }
}
