package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository; //Inyección del Repositorio

    //REGISTRO NEW USUARIO
    public User registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
            // ERROR SI EXISTE
        }
        User user = new User(username, password);
        return userRepository.save(user); //guardar en BBDD
    }

    // Comprueba credenciales para login
    public Optional<User> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }
        return Optional.empty();
    }
    
    // Busca un usuario o lo crea si no existe 
    public User getOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, "1234"))); //PASS DEFECTO
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
