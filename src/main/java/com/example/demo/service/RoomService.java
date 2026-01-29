package com.example.demo.service;

import com.example.demo.model.Block;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.model.Room.RoomStatus;
import com.example.demo.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public Room createRoom(User host, Block block, int numQuestions, int secondsPerQuestion, boolean randomOrder) {
        if (block.getQuestions().size() < 20) {
            throw new RuntimeException("Block must have at least 20 questions to be used in a room.");
        }

        Room room = new Room();
        room.setHost(host);
        room.setBlock(block);
        room.setNumberOfQuestions(numQuestions);
        room.setSecondsPerQuestion(secondsPerQuestion);
        room.setRandomOrder(randomOrder);
        room.setStatus(RoomStatus.WAITING);

        // Generate unique PIN
        String pin;
        do {
            pin = String.format("%04d", new Random().nextInt(10000));
        } while (roomRepository.findByPin(pin).isPresent());

        room.setPin(pin);

        return roomRepository.save(room);
    }

    public Optional<Room> getRoomByPin(String pin) {
        return roomRepository.findByPin(pin);
    }

    public void updateRoomStatus(Long roomId, RoomStatus status) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        room.setStatus(status);
        roomRepository.save(room);
    }
}
