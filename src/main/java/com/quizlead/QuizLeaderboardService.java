package com.quizlead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class QuizLeaderboardService {
    public static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    public static final String REG_NO = "2024CS101";
    public static final int POLL_COUNT = 10;
    public static final int DELAY_MS = 5000;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private QuizLeaderboardService() {
        // utility class
    }

    public static List<LeaderboardRow> computeLeaderboard() throws IOException, InterruptedException {
        Set<String> seenEvents = new HashSet<>();
        Map<String, Integer> participantScores = new HashMap<>();

        for (int poll = 0; poll < POLL_COUNT; poll++) {
            MessageResponse response = fetchPoll(HTTP_CLIENT, poll);
            processEvents(response.events, seenEvents, participantScores);
            if (poll < POLL_COUNT - 1) {
                Thread.sleep(DELAY_MS);
            }
        }

        return buildLeaderboard(participantScores);
    }

    public static int computeTotalScore(List<LeaderboardRow> leaderboard) {
        return leaderboard.stream().mapToInt(LeaderboardRow::getTotalScore).sum();
    }

    public static void processEvents(List<Event> events, Set<String> seenEvents, Map<String, Integer> participantScores) {
        for (Event event : events) {
            String eventKey = event.roundId + "|" + event.participant;
            if (!seenEvents.add(eventKey)) {
                continue;
            }
            participantScores.merge(event.participant, event.score, Integer::sum);
        }
    }

    public static MessageResponse fetchPoll(HttpClient client, int pollIndex) throws IOException, InterruptedException {
        URI uri = URI.create(String.format("%s/quiz/messages?regNo=%s&poll=%d", BASE_URL, REG_NO, pollIndex));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected response code: " + response.statusCode());
        }
        return OBJECT_MAPPER.readValue(response.body(), MessageResponse.class);
    }

    public static List<LeaderboardRow> buildLeaderboard(Map<String, Integer> participantScores) {
        return participantScores.entrySet().stream()
                .map(entry -> new LeaderboardRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(LeaderboardRow::getTotalScore).reversed()
                        .thenComparing(LeaderboardRow::getParticipant))
                .collect(Collectors.toList());
    }

    public static List<LeaderboardRow> parseLeaderboardJson(String json) throws IOException {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageResponse {
        public String regNo;
        public String setId;
        public int pollIndex;
        public List<Event> events = Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        public String roundId;
        public String participant;
        public int score;
    }

    public static class LeaderboardRow {
        private final String participant;
        private final int totalScore;

        public LeaderboardRow(String participant, int totalScore) {
            this.participant = participant;
            this.totalScore = totalScore;
        }

        public String getParticipant() {
            return participant;
        }

        public int getTotalScore() {
            return totalScore;
        }
    }
}
