package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    @Transactional
    public void loginUser(String username) {
        userDao.findByUsername(username)
                .ifPresentOrElse(userEntity -> {
                    userEntity.setLastVisitDate(LocalDateTime.now());
                }, () -> {
                    UserEntity entity = new UserEntity();
                    entity.setUsername(username);
                    entity.setRegistrationDate(LocalDateTime.now());
                    entity.setLastVisitDate(LocalDateTime.now());
                    userDao.save(entity);
                });
    }

    public List<UserEntity> findAllOrderByVisitDateDesc() {
        return userDao.findAll().stream()
                .sorted(Comparator.comparing(UserEntity::getLastVisitDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<UserEntity> findAllExceptCurrent() {
        return findAllOrderByVisitDateDesc()
                .stream()
                .filter(userEntity -> !userEntity.getUsername().equals(QuizUtils.getLoggedUser()))
                .toList();
    }

    public UserEntity findByUsername(String username) {
        return userDao.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found " + username));
    }
}
