package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.User;
import java.util.Optional;
/*    // Busca un usuario por su nombre de usuario
    // Retorna un 'Optional' porque puede que el usuario no exista.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
