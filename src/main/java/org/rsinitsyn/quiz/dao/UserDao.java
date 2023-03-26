package org.rsinitsyn.quiz.dao;

import java.util.Optional;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
}
