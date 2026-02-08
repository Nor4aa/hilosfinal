package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Question;
import java.util.List;

// Busca todas las preguntas que pertenecen a un bloque.
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByBlockId(Long blockId);
}
