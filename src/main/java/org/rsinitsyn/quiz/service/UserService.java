package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(value = "allRegisteredUsers", allEntries = true)
    public UserEntity registerUser(String username, String password) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(password));
        entity.setRegistrationDate(LocalDateTime.now());
        entity.setLastVisitDate(LocalDateTime.now());
        return userDao.save(entity);
    }

    @Cacheable(value = "allRegisteredUsers")
    public List<UserEntity> findAllOrderByVisitDateDesc() {
        return userDao.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getLastVisitDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<UserEntity> findAllExceptCurrent() {
        return findAllOrderByVisitDateDesc()
                .stream()
                .filter(userEntity -> !userEntity.getUsername().equals(SessionWrapper.getLoggedUser()))
                .toList();
    }

    public UserEntity findByUsername(String username) {
        return userDao.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found " + username));
    }
}
