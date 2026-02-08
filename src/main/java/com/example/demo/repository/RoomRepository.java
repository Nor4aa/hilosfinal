package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Room;
import com.example.demo.model.Room.RoomStatus;
import java.util.Optional;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
   
	Optional<Room> findByPin(String pin);

    List<Room> findByStatus(RoomStatus status);
}
