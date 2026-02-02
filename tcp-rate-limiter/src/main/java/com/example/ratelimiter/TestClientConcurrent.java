package com.example.ratelimiter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestClientConcurrent {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 9090;
    private static final int CLIENT_COUNT = 15; // number of concurrent clients

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT);

        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int clientId = i + 1;
            executor.submit(() -> sendRequest(clientId));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void sendRequest(int clientId) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("REQUEST");
            String response = in.readLine();
            System.out.printf("Client %d received: %s%n", clientId, response);

        } catch (IOException e) {
            System.err.printf("Client %d error: %s%n", clientId, e.getMessage());
        }
    }
}
