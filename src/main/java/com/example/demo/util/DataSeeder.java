package com.example.demo.util;

import com.example.demo.model.Block;
import com.example.demo.model.Question;
import com.example.demo.model.User;
import com.example.demo.repository.BlockRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component //componente gestionado por Spring.
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public void run(String... args) throws Exception {
    	// comprobar si hay usuarios
        if (userRepository.count() == 0) {
            System.out.println("SEEDING DATA..."); 

            // Crear Host
            User host = new User("profesor", "1234");
            userRepository.save(host);

            // Crear Block
            Block block = new Block("Cultura General", "Preguntas variadas para probar el sistema", host);
            block = blockRepository.save(block);

            // Crear 25 preguntas
            for (int i = 1; i <= 25; i++) {
                Question q = new Question(
                        "¿Pregunta número " + i + "?",
                        "Opción A (Incorrecta)",
                        "Opción B (Correcta)",
                        "Opción C (Incorrecta)",
                        "Opción D (Incorrecta)",
                        2 
                );
                q.setBlock(block); // Vinculamos la pregunta al bloque "Cultura General"
                questionRepository.save(q); //guardamos en la BBDD
            }

            System.out.println("DATA SEEDED: User 'profesor', 1 Block, 25 Questions."); // confirmación
        }
    }
}
