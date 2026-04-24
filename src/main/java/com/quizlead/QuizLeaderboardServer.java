package com.quizlead;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class QuizLeaderboardServer {
    private static final int PORT = 8080;
    private static final Path FRONTEND_PATH = Paths.get(System.getProperty("user.dir"), "frontend");
    private static final MongoLeaderboardRepository MONGO_REPOSITORY = new MongoLeaderboardRepository();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/leaderboard", new LeaderboardHandler());
        server.createContext("/api/leaderboard/latest", new LatestLeaderboardHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);

        Runtime.getRuntime().addShutdownHook(new Thread(MONGO_REPOSITORY::close));

        System.out.printf("Server started at http://localhost:%d%n", PORT);
        System.out.println("Open this address in a browser and click Fetch Leaderboard.");
        server.start();
    }

    private static class LeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            try {
                List<QuizLeaderboardService.LeaderboardRow> leaderboard = QuizLeaderboardService.computeLeaderboard();
                int totalScore = QuizLeaderboardService.computeTotalScore(leaderboard);
                MongoLeaderboardRepository.LeaderboardRecord record = MONGO_REPOSITORY.saveLeaderboard(leaderboard, totalScore);

                byte[] responseBody = QuizLeaderboardService.OBJECT_MAPPER.writeValueAsBytes(record.getDocument());
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            } catch (Exception e) {
                String message = "Error computing leaderboard: " + e.getMessage();
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                byte[] bytes = message.getBytes();
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private static class LatestLeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            MongoLeaderboardRepository.LeaderboardRecord record = MONGO_REPOSITORY.fetchLatestLeaderboard();
            if (record == null) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            byte[] responseBody = QuizLeaderboardService.OBJECT_MAPPER.writeValueAsBytes(record.getDocument());
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBody);
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            Path resolved = FRONTEND_PATH.resolve(path.substring(1)).normalize();
            if (!resolved.startsWith(FRONTEND_PATH) || !Files.exists(resolved) || Files.isDirectory(resolved)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String contentType = switch (getFileExtension(resolved.getFileName().toString())) {
                case "html" -> "text/html; charset=UTF-8";
                case "css" -> "text/css; charset=UTF-8";
                case "js" -> "application/javascript; charset=UTF-8";
                case "json" -> "application/json; charset=UTF-8";
                default -> "application/octet-stream";
            };
            byte[] bytes = Files.readAllBytes(resolved);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            return lastDot >= 0 ? fileName.substring(lastDot + 1) : "";
        }
    }
}
