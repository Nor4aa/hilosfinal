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
    private RoomService roomService; // To update DB status

    // Manage multiple rooms concurrently: Map<PIN, ActiveRoom>
    private final ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();

    public void createGame(Room room, List<Question> questions) {
        // Apply randomization if needed
        List<Question> gameQuestions = new ArrayList<>(questions);
        if (room.isRandomOrder()) {
            Collections.shuffle(gameQuestions);
        }

        // Limit to NumberOfQuestions if specified
        if (room.getNumberOfQuestions() > 0 && room.getNumberOfQuestions() < gameQuestions.size()) {
            gameQuestions = gameQuestions.subList(0, room.getNumberOfQuestions());
        }

        ActiveRoom activeRoom = new ActiveRoom(room, gameQuestions);
        activeRooms.put(room.getPin(), activeRoom);

        roomService.updateRoomStatus(room.getId(), RoomStatus.WAITING);
    }

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

    public void startGame(String pin) {
        ActiveRoom room = activeRooms.get(pin);
        if (room != null) {
            // Update DB status?
            room.startGame();
        }
    }
}
