package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameParticipantDao;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.GameParticipantEntity;
import org.rsinitsyn.quiz.entity.GameParticipantRole;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.rsinitsyn.quiz.entity.GameParticipantId.gameParticipantId;
import static org.rsinitsyn.quiz.entity.GameParticipantRole.PLAYER;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameParticipantService {

    private final GameParticipantDao gameParticipantDao;
    private final GameDao gameDao;
    private final UserDao userDao;

    public void addPlayer(UUID gameId, String username) {
        addPlayer(gameId, userDao.findByUsername(username).orElseThrow());
    }

    public Optional<GameParticipantEntity> findParticipant(UUID gameId, UUID userId) {
        return gameParticipantDao.findById(gameParticipantId(gameId, userId));
    }

    @Transactional
    public void addPlayer(UUID gameId, UserEntity user) {
        final var userId = user.getId();
        if (gameParticipantDao.existsById(gameParticipantId(gameId, userId))) {
            log.debug("User {} is already a participant of game {}", userId, gameId);
            return;
        }
        final var game = gameDao.getReferenceById(gameId);

        final var entity = GameParticipantEntity.builder()
                .id(gameParticipantId(gameId, userId))
                .game(game)
                .user(user)
                .role(PLAYER)
                .joinDate(now())
                .build();
        gameParticipantDao.save(entity);
        log.debug(
                "Player added to game {}, userId={}, username={}",
                gameId,
                userId,
                user.getUsername()
        );
    }

    @Transactional
    public void removePlayer(UUID gameId, UUID userId) {
        final var id = gameParticipantId(gameId, userId);
        if (!gameParticipantDao.existsById(id)) {
            return;
        }
        gameParticipantDao.deleteById(id);
        log.debug("Player {} removed from game {}", userId, gameId);
    }

    @Transactional
    public void changeRole(UUID gameId, UUID userId, GameParticipantRole role) {
        final var id = gameParticipantId(gameId, userId);
        final var participant = gameParticipantDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User %s is not a participant of game %s".formatted(userId, gameId)));
        participant.setRole(role);
        log.debug("Changed role for user {} in game {} to {}", userId, gameId, role);
    }
}