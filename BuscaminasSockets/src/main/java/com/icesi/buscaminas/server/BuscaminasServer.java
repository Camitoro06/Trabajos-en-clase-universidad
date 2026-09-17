package com.icesi.buscaminas.server;

import com.icesi.buscaminas.protocol.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BuscaminasServer {
    private static final int THREADS = 20;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        Runtime.getRuntime().addShutdownHook(new Thread(pool::shutdown));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("===========================================");
            System.out.println(" Buscaminas TCP Server");
            System.out.println(" Puerto: " + port);
            System.out.println(" Pool de hilos: " + THREADS);
            System.out.println("===========================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar/continuar el servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
