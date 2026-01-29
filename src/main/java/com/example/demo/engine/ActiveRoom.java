package com.example.demo.engine;

import com.example.demo.model.Player;
import com.example.demo.model.Question;
import com.example.demo.model.Room;
import com.example.demo.repository.RoomRepository; // Ideally use service, but repository is direct here for simplicity
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Represents a running game room in memory.
 * Handles concurrency for timers and answer processing.
 */
public class ActiveRoom {

    private static final Logger logger = LoggerFactory.getLogger(ActiveRoom.class);

    private final String pin;
    private final Room roomDb; // Reference to DB entity
    private final List<Question> questions;
    private final int secondsPerQuestion;

    // Concurrency components
    private final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService answerProcessor = Executors.newFixedThreadPool(10); // Process up to 10 answers in
                                                                                      // parallel

    // State
    private AtomicInteger currentQuestionIndex = new AtomicInteger(-1);
    private AtomicBoolean isQuestionOpen = new AtomicBoolean(false);

    // Players and Scores (Thread-safe)
    private final ConcurrentHashMap<String, Integer> playerScores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> playerAnsweredCurrentQuestion = new ConcurrentHashMap<>();

    // Callbacks/Listeners could be added here for UI updates (e.g., via WebSocket
    // if we had it, or polling)

    public ActiveRoom(Room roomDb, List<Question> questions) {
        this.roomDb = roomDb;
        this.pin = roomDb.getPin();
        this.questions = questions;
        this.secondsPerQuestion = roomDb.getSecondsPerQuestion();
        logger.info("[Room {}] Initalized with {} questions.", pin, questions.size());
    }

    public void addPlayer(String playerName) {
        playerScores.putIfAbsent(playerName, 0);
    }

    public void startGame() {
        logger.info("[Room {}] Starting game...", pin);
        nextQuestion();
    }

    private void nextQuestion() {
        int index = currentQuestionIndex.incrementAndGet();
        if (index >= questions.size()) {
            finishGame();
            return;
        }

        Question q = questions.get(index);
        logger.info("[Room {}] Starting Question {}: {}", pin, index + 1, q.getEnunciado());

        // Reset per-question state
        playerAnsweredCurrentQuestion.clear();
        isQuestionOpen.set(true);

        // Schedule timer to close question
        timerScheduler.schedule(() -> {
            logger.info("[Room {}] [Timer] Time up for question {}!", pin, index + 1);
            closeQuestion();
        }, secondsPerQuestion, TimeUnit.SECONDS);
    }

    private void closeQuestion() {
        isQuestionOpen.set(false);
        // Wait a bit or move immediately? For simplicity, wait 3 seconds then next
        try {
            Thread.sleep(3000); // Simple pause between questions
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        nextQuestion();
    }

    private void finishGame() {
        logger.info("[Room {}] Game Finished!", pin);
        isQuestionOpen.set(false);
        shutdown();
        // Here we would save final results to DB if not done incrementally
    }

    public void submitAnswer(String playerName, int selectedOption) {
        // Submit task to thread pool
        answerProcessor.submit(() -> {
            String threadName = Thread.currentThread().getName();

            if (!isQuestionOpen.get()) {
                logger.warn("[Room {}] [{}] Answer rejected (Time up) for player {}", pin, threadName, playerName);
                return;
            }

            if (playerAnsweredCurrentQuestion.putIfAbsent(playerName, true) != null) {
                logger.warn("[Room {}] [{}] Answer rejected (Already answered) for player {}", pin, threadName,
                        playerName);
                return;
            }

            // Check correctness
            int idx = currentQuestionIndex.get();
            if (idx >= 0 && idx < questions.size()) {
                Question q = questions.get(idx);
                boolean correct = (q.getRespuCorrect() == selectedOption);

                logger.info("[Room {}] [{}] Processing answer for {}: {} (Correct: {})",
                        pin, threadName, playerName, selectedOption, correct);

                if (correct) {
                    playerScores.compute(playerName, (k, v) -> v == null ? 1 : v + 1);
                }
            }
        });
    }

    // Getters for UI polling
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex.get();
    }

    public boolean isQuestionOpen() {
        return isQuestionOpen.get();
    }

    public Map<String, Integer> getRanking() {
        // Return sorted copy
        return playerScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));
    }

    public Question getCurrentQuestion() {
        int idx = currentQuestionIndex.get();
        if (idx >= 0 && idx < questions.size())
            return questions.get(idx);
        return null;
    }

    public void shutdown() {
        timerScheduler.shutdown();
        answerProcessor.shutdown();
    }
}
