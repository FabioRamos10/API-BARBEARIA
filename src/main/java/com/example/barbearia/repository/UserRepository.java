package com.example.barbearia.repository;

import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends MongoRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludeId);

    List<User> findByRoleIn(Collection<Role> roles);

    List<User> findByRoleInAndAtivoTrue(Collection<Role> roles);

    List<User> findByAtivoTrueOrderByNomeAsc();
}
