package com.example.ratelimiter.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.ratelimiter.service.RateLimiterService;

public class TcpRateLimiterServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpRateLimiterServer.class);
    private static final int PORT = 9090;
    private static final int THREAD_POOL_SIZE = 10; // adjust as needed

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        RateLimiterService rateLimiterService = new RateLimiterService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            rateLimiterService.close();
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("TCP Rate Limiter Server started on port {}", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getInetAddress());

                // Submit client handler to thread pool
                executorService.submit(new ClientHandler(clientSocket, rateLimiterService));
            }

        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        } finally {
            executorService.shutdown();
            rateLimiterService.close();
            logger.info("Server shutting down");
        }
    }
}
