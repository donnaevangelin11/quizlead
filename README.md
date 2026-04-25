# QuizLead

A Java application for the Quiz Leaderboard System assignment.

## What it does

- Polls the validator API 10 times with `poll=0` through `poll=9`
- Waits 5 seconds between each poll
- Deduplicates events by `roundId + participant`
- Aggregates scores per participant
- Builds a sorted leaderboard by total score
- Computes total score across all participants
- Submits the leaderboard once

## Project structure

- `pom.xml` — Maven build configuration
- `src/main/java/com/quizlead/QuizLeaderboardApp.java` — main application

## Requirements

- Java 17+
- Maven

## Build

```bash
mvn clean package
```

## Run

```bash
mvn clean package
```

### CLI mode

```bash
java -jar target/quizlead-0.1.0-jar-with-dependencies.jar
```

### Server mode

```bash
java -cp target/quizlead-0.1.0-jar-with-dependencies.jar com.quizlead.QuizLeaderboardServer
```

Then open `http://localhost:8080`.

### GitHub Pages

The frontend can be hosted on GitHub Pages from the `docs/` folder in this repository.
Copy or deploy the `docs/` directory as the Pages source in repository settings to publish the static UI.

> Note: GitHub Pages only serves the static frontend. The Java backend and MongoDB database still need a separate host.

### Best hosting combination

- Frontend: GitHub Pages (`docs/` folder)
- Backend: Railway or Render running the Java `QuizLeaderboardServer`
- Database: MongoDB Atlas managed cluster

### Recommended deployment for a complete pathway

1. Push this repository to GitHub.
2. Enable GitHub Pages with the `docs/` folder as the source.
3. Create a free MongoDB Atlas cluster and note the connection string.
4. Deploy the Java backend on Railway or Render using this repository.
5. Configure environment variables for the backend service:
   - `MONGODB_URI`
   - `MONGODB_DB=quizlead`
   - `MONGODB_COLLECTION=leaderboards`
6. Deploy the backend with the start command:

```bash
java -cp target/quizlead-0.1.0-jar-with-dependencies.jar com.quizlead.QuizLeaderboardServer
```

7. If the frontend is hosted separately on GitHub Pages, update the frontend API URL to the deployed backend host.

## Frontend

A simple frontend is available under `frontend/`:

- `frontend/index.html`
- `frontend/styles.css`
- `frontend/app.js`

The frontend uses `axios` to fetch the leaderboard from `/api/leaderboard`.

## Notes

The application uses the provided `regNo=2024CS101` and the validator base URL.
It prints the accepted events, duplicate events ignored, final leaderboard, total score, and the submit response.
