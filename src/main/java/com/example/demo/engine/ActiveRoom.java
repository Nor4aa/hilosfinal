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


public class ActiveRoom {
	
	//escribir en la consola lo que va pasando y ayudar a depurar.

    private static final Logger logger = LoggerFactory.getLogger(ActiveRoom.class);

    private final String pin;
    private final Room roomDb; 
    private final List<Question> questions;
    private final int secondsPerQuestion;

    // CONCURRENCIAS (HILOS)
    
    // hay 1 hilo dedicado solo a mirar el reloj.
    private final ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();
    // 10 HILOS --> procesar 10 respuestas de alumnos SIMULTÁNEAMENTE
    private final ExecutorService answerProcessor = Executors.newFixedThreadPool(10); 

    // ESTADO 
    
    private AtomicInteger currentQuestionIndex = new AtomicInteger(-1); 
    private AtomicBoolean isQuestionOpen = new AtomicBoolean(false);

    // DATOS JUGADORES
    //GUARDAR PUNTOS Y QUIÉN A RESPONDIDO
    private final ConcurrentHashMap<String, Integer> playerScores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> playerAnsweredCurrentQuestion = new ConcurrentHashMap<>();

    //LOGICA JUEGO
    //CONTRUCTOR -->  Prepara la sala con los datos que le manda el GameEngine
    public ActiveRoom(Room roomDb, List<Question> questions) {
        this.roomDb = roomDb;
        this.pin = roomDb.getPin();
        this.questions = questions;
        this.secondsPerQuestion = roomDb.getSecondsPerQuestion();
        logger.info("[Room {}] Initalized with {} questions.", pin, questions.size());
    }

    /**
     * Añade un jugador a la sala activa.
     *
     * @putIfAbsent devuelve null si es nuevo. 
     * Si devuelve algo, es que ya existía.
     */
    
    //ARRANCA LA PARTIDA
    public boolean addPlayer(String playerName) {
        return playerScores.putIfAbsent(playerName, 0) == null;
    }

    public void startGame() {
        logger.info("[Room {}] Starting game...", pin);
        nextQuestion();
    }

    private void nextQuestion() {
    	//// Incrementamos el índice de forma atómica 
        int index = currentQuestionIndex.incrementAndGet();
        if (index >= questions.size()) {
            finishGame();
            return;
        }

        Question q = questions.get(index);
        logger.info("[Room {}] Starting Question {}: {}", pin, index + 1, q.getEnunciado());

        //RESETEAMOS
        playerAnsweredCurrentQuestion.clear();
        isQuestionOpen.set(true);

        // TEMPORIZADOR
        timerScheduler.schedule(() -> {
            String threadName = Thread.currentThread().getName();
            logger.info("[Room {}] [{}] [Timer] Time up for question {}!", pin, threadName, index + 1);
            closeQuestion();
        }, secondsPerQuestion, TimeUnit.SECONDS);
    }

    // CERRAMOS LA PREGUNTA CUANDO EL TIEMPO TERMINA
    private void closeQuestion() {
        isQuestionOpen.set(false); // NO SE ACEPTAN + RESPUESTAS
        
        try {
            Thread.sleep(3000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        nextQuestion();
    }

    //Termina el juego y apaga los hilos
    private void finishGame() {
        logger.info("[Room {}] Game Finished!", pin);
        isQuestionOpen.set(false);
        shutdown();
        //Importante: Apagar los hilos o el servidor no se cerrará bien nunca
    }
    
    //PROCESAR RESPUESTAS
    public void submitAnswer(String playerName, int selectedOption) {
    	/*
    	 *	NO procesamos la respuesta en el hilo principal 
        	La enviamos al 'answerProcessor' (el pool de 10 hilos) 
   			para que lo haga en paralelo.
    	 */
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

            // COMPROBAMOS SI ES CORRECTA 
            int idx = currentQuestionIndex.get();
            if (idx >= 0 && idx < questions.size()) {
                Question q = questions.get(idx);
                boolean correct = (q.getRespuCorrect() == selectedOption);

                logger.info("[Room {}] [{}] Processing answer for {}: {} (Correct: {})",
                        pin, threadName, playerName, selectedOption, correct);

                //compute: función atómica para actualizar el valor del mapa.
                if (correct) {
                    playerScores.compute(playerName, (k, v) -> v == null ? 1 : v + 1);
                }
            }
        });
    }

  
    //INDICE PREGUNTA
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex.get();
    }

    public boolean isQuestionOpen() {
        return isQuestionOpen.get();
    }

    //RANKING
    public Map<String, Integer> getRanking() {

        return playerScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));
    }

    //OBJETO PREGUNTA
    public Question getCurrentQuestion() {
        int idx = currentQuestionIndex.get();
        if (idx >= 0 && idx < questions.size())
            return questions.get(idx);
        return null;
    }

    // APAGAR TODO
    public void shutdown() {
        timerScheduler.shutdown();
        answerProcessor.shutdown();
    }
}
