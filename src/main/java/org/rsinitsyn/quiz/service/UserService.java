package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.rsinitsyn.quiz.utils.QuizUtils.generateFilenameWithExt;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final QuizAppProperties properties;
    private final ResourceService resourceService;

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

    @Transactional
    public UserEntity updatePhoto(String username, String filename, InputStream photoStream) {
        return userDao.findByUsername(username).map(user -> {
                    if (StringUtils.isEmpty(filename)) {
                        return user;
                    }
                    final var filepath = properties.getFilesFolder()
                            + generateFilenameWithExt(FilenameUtils.getExtension(filename));
                    resourceService.saveImageFromInputStream(filepath, photoStream);
                    user.setPhotoFilename(filepath);
                    return user;
                })
                .orElseThrow();
    }

    @Cacheable(value = "allRegisteredUsers")
    public List<UserEntity> findAllOrderByVisitDateDesc() {
        return userDao.findAllByOrderByLastVisitDateDesc();
    }

    public List<UserEntity> findAllExceptLogged() {
        return userDao.findAllByUsernameNotOrderByLastVisitDateDesc(SessionWrapper.getLoggedUser());
    }

    public UserEntity findByUsername(String username) {
        return userDao.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found " + username));
    }
}
