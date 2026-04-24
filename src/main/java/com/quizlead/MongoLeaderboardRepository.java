package com.quizlead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Sorts.descending;

public final class MongoLeaderboardRepository {
    private static final String DEFAULT_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DB = "quizlead";
    private static final String DEFAULT_COLLECTION = "leaderboards";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoLeaderboardRepository() {
        String uri = System.getenv().getOrDefault("MONGODB_URI", DEFAULT_URI);
        String dbName = System.getenv().getOrDefault("MONGODB_DB", DEFAULT_DB);
        String collectionName = System.getenv().getOrDefault("MONGODB_COLLECTION", DEFAULT_COLLECTION);

        this.client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(uri))
                .build());

        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
    }

    public LeaderboardRecord saveLeaderboard(List<QuizLeaderboardService.LeaderboardRow> leaderboard, int totalScore) {
        Instant now = Instant.now();
        Document document = new Document();
        document.append("regNo", QuizLeaderboardService.REG_NO);
        document.append("createdAt", now.toString());
        document.append("totalScore", totalScore);
        document.append("rows", toDocumentList(leaderboard));

        collection.insertOne(document);
        return new LeaderboardRecord(document.getObjectId("_id").toHexString(), document);
    }

    public LeaderboardRecord fetchLatestLeaderboard() {
        FindIterable<Document> iterable = collection.find(exists("createdAt", true))
                .sort(descending("createdAt"))
                .limit(1);
        Document doc = iterable.first();
        return doc == null ? null : new LeaderboardRecord(doc.getObjectId("_id").toHexString(), doc);
    }

    private List<Document> toDocumentList(List<QuizLeaderboardService.LeaderboardRow> leaderboard) {
        List<Document> docs = new ArrayList<>();
        for (QuizLeaderboardService.LeaderboardRow row : leaderboard) {
            docs.add(new Document(Map.of(
                    "participant", row.getParticipant(),
                    "totalScore", row.getTotalScore()
            )));
        }
        return docs;
    }

    public void close() {
        client.close();
    }

    public static final class LeaderboardRecord {
        private final String id;
        private final Document document;

        public LeaderboardRecord(String id, Document document) {
            this.id = id;
            this.document = document;
        }

        public String getId() {
            return id;
        }

        public Document getDocument() {
            return document;
        }

        public String toJson() throws IOException {
            return OBJECT_MAPPER.writeValueAsString(document);
        }
    }
}
