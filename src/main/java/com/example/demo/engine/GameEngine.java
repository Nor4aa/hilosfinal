package com.example.demo.engine;

import com.example.demo.model.Block;
import com.example.demo.model.Question;
import com.example.demo.model.Room;
import com.example.demo.model.Room.RoomStatus;
import com.example.demo.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameEngine {

    @Autowired
    private RoomService roomService; // actualizar el estado de la sala en la Base de Datos.

    // MAPA SALAS ACTIVAS

    // permite que muchos hilos lean/escriban a la vez sin chocar.
    private final ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();

    public void createGame(Room room, List<Question> questions) { // crear una nueva partida en memoria

        // copia de la lista --> no tocamos la original
        List<Question> gameQuestions = new ArrayList<>(questions);
        // Si la sala está configurada como "Aleatoria" --> las desordenamos.
        if (room.isRandomOrder()) {
            Collections.shuffle(gameQuestions);
        }

        // Si se piden menos preguntas de las que hay --> recortamos la lista
        if (room.getNumberOfQuestions() > 0 && room.getNumberOfQuestions() < gameQuestions.size()) {
            gameQuestions = gameQuestions.subList(0, room.getNumberOfQuestions());
        }

        // CREACIOÓN SALA (obj que controla la logica)
        ActiveRoom activeRoom = new ActiveRoom(room, gameQuestions);
        activeRooms.put(room.getPin(), activeRoom); // uso de PIN como llave

        // ESPERANDO JUGADORES --> Actualización base de datos
        roomService.updateRoomStatus(room.getId(), RoomStatus.WAITING);
    }

    // recuperar una sala --> su PIN
    public ActiveRoom getRoom(String pin) {
        return activeRooms.get(pin);
    }

    public boolean joinPlayer(String pin, String playerName) {
        ActiveRoom room = activeRooms.get(pin);
        if (room != null) {
            return room.addPlayer(playerName);
        }
        return false;
    }

    // INICIAMOS JUEGO

    public void startGame(String pin) {
        ActiveRoom room = activeRooms.get(pin);
        if (room != null) {
            // Update DB status?
            room.startGame();
        }
    }
}
