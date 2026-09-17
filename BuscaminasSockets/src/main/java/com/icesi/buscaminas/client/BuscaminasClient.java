package com.icesi.buscaminas.client;

import com.icesi.buscaminas.protocol.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class BuscaminasClient {
    private static final int SERVER_RESPONSE_TIMEOUT_MS = 15_000;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            socket.setSoTimeout(SERVER_RESPONSE_TIMEOUT_MS);
            socket.setTcpNoDelay(true);

            System.out.println("Conectado a " + host + ":" + port);
            readResponse(in);

            while (true) {
                System.out.print("buscaminas> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String command = scanner.nextLine().trim();
                if (command.isEmpty()) {
                    continue;
                }

                out.println(command);
                readResponse(in);

                String first = command.split("\\s+")[0];
                if (first.equalsIgnoreCase("SALIR") || first.equalsIgnoreCase("FIN")
                        || first.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: el servidor no respondio dentro del tiempo esperado.");
        } catch (IOException e) {
            System.err.println("No fue posible conectar/comunicarse con el servidor: " + e.getMessage());
        }
    }

    private static void readResponse(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (Protocol.END.equals(line)) {
                return;
            }
            System.out.println(line);
        }
        throw new IOException("El servidor cerro la conexion.");
    }
}
