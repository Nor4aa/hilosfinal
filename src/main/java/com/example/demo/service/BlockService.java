package com.example.demo.service;

import com.example.demo.model.Block;
import com.example.demo.model.Question;
import com.example.demo.model.User;
import com.example.demo.repository.BlockRepository;
import com.example.demo.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BlockService {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private QuestionRepository questionRepository;
    
    
// OBTIENE BLOQUES DE UN PROFESOR
    public List<Block> getBlocksByUser(User user) {
        return blockRepository.findByOwnerId(user.getId());
    }

    //BLOQUE VACIÍO
    public Block createBlock(String name, String description, User owner) {
        Block block = new Block(name, description, owner);
        return blockRepository.save(block);
    }

    public Optional<Block> getBlockById(Long id) {
        return blockRepository.findById(id);
    }

    public void deleteBlock(Long blockId) {
        // Cascade delete is handled by JPA if configured correctly in Entity
        blockRepository.deleteById(blockId);
    }

    
    @Transactional // Si algo falla, deshace todos los cambios de esta operación
    public Question addQuestionToBlock(Long blockId, Question question) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Block not found"));

        // VALIDACIONES
        if (question.getEnunciado() == null || question.getEnunciado().isEmpty())
            throw new RuntimeException("Enunciado empty");
        if (question.getOp1() == null || question.getOp1().isEmpty())
            throw new RuntimeException("Op1 empty");
        if (question.getOp2() == null || question.getOp2().isEmpty())
            throw new RuntimeException("Op2 empty");
        if (question.getOp3() == null || question.getOp3().isEmpty())
            throw new RuntimeException("Op3 empty");
        if (question.getOp4() == null || question.getOp4().isEmpty())
            throw new RuntimeException("Op4 empty");
        if (question.getRespuCorrect() < 1 || question.getRespuCorrect() > 4)
            throw new RuntimeException("Invalid correct answer index");

        block.addQuestion(question); //// Vincula la pregunta al bloque
        questionRepository.save(question); // GUARDAR BBDD
     
        return question;
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }
}
