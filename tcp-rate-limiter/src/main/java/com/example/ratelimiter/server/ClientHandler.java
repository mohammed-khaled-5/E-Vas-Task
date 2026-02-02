package com.example.ratelimiter.server;

import com.example.ratelimiter.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private static final RateLimiterService rateLimiterService = new RateLimiterService();
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();

        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                clientSocket.getOutputStream(), true
            )
        ) {
            String request;
            while ((request = in.readLine()) != null) {
                if ("REQUEST".equals(request)) {
                    boolean allowed = rateLimiterService.allowRequest(clientIp);

                    if (allowed) {
                        out.println("ALLOW");
                        logger.info("Request allowed | clientIp={}", clientIp);
                    } else {
                        out.println("DENY"); // Already logged in RateLimiterService
                    }
                } else {
                    out.println("INVALID");
                    logger.warn("Invalid request | clientIp={} | request={}", clientIp, request);
                }
            }

        } catch (IOException e) {
            logger.error("IO error with client {}: {}", clientIp, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error with client {}: {}", clientIp, e.getMessage(), e);
        } finally {
            try {
                clientSocket.close();
                logger.info("Connection closed | clientIp={}", clientIp);
            } catch (IOException e) {
                logger.error("Error closing client socket {}: {}", clientIp, e.getMessage(), e);
            }
        }
    }
}
