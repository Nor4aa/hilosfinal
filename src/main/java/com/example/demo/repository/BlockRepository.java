package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Block;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByOwnerId(Long userId);
}
