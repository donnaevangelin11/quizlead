package com.quizlead;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizLeaderboardApp {
    public static void main(String[] args) {
        System.out.println("Quiz Leaderboard Processor starting...");

        try {
            List<QuizLeaderboardService.LeaderboardRow> leaderboard = QuizLeaderboardService.computeLeaderboard();
            int totalScore = QuizLeaderboardService.computeTotalScore(leaderboard);

            System.out.println("\nFinal leaderboard:");
            leaderboard.forEach(row ->
                    System.out.printf("- %s: %d%n", row.getParticipant(), row.getTotalScore()));
            System.out.printf("Total score across all participants: %d%n", totalScore);

            submitLeaderboard(leaderboard);
        } catch (Exception e) {
            System.err.printf("Failed to compute leaderboard: %s%n", e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Quiz Leaderboard Processor completed.");
    }

    private static void submitLeaderboard(List<QuizLeaderboardService.LeaderboardRow> leaderboard) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("regNo", QuizLeaderboardService.REG_NO);
        requestBody.put("leaderboard", leaderboard);

        String bodyJson = QuizLeaderboardService.OBJECT_MAPPER.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(QuizLeaderboardService.BASE_URL + "/quiz/submit"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Submit failed with HTTP status " + response.statusCode());
        }

        Map<String, Object> responseJson = QuizLeaderboardService.OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
        });
        System.out.println("\nSubmission response:");
        responseJson.forEach((key, value) -> System.out.printf("%s: %s%n", key, value));
    }
}
