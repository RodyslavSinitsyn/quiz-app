package org.rsinitsyn.quiz.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.UserStatsModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final GameDao gameDao;
    private final QuestionDao questionDao;
    private final UserDao userDao;

    public List<UserStatsModel> getAllUsersStats() {
        List<UserEntity> allUsers = userDao.findAll();

        return allUsers.stream()
                .map(userEntity -> {

                    List<QuestionEntity> questions = questionDao.findAllByCreatedBy(userEntity.getUsername());
                    List<GameEntity> gamesCreated = gameDao.findAllByCreatedBy(userEntity.getUsername())
                            .stream().filter(gameEntity -> gameEntity.getStatus().equals(GameStatus.FINISHED)).toList();
                    List<GameEntity> gamesPlayed = gameDao.findAllByPlayerName(userEntity.getUsername())
                            .stream().filter(gameEntity -> gameEntity.getStatus().equals(GameStatus.FINISHED)).toList();

                    List<Set<GameQuestionEntity>> allQuestions = gamesPlayed.stream().map(GameEntity::getGameQuestions).toList();

                    long totalAnsweredCount = allQuestions.stream().mapToLong(Collection::size).sum();
                    long correctAnswersCount = allQuestions.stream().flatMap(Collection::stream).filter(GameQuestionEntity::getAnswered).count();

                    return new UserStatsModel(
                            userEntity.getUsername(),
                            questions.size(),
                            gamesCreated.size(),
                            gamesPlayed.size(),
                            correctAnswersCount + "/" + totalAnsweredCount,
                            totalAnsweredCount == 0 ? "" : (correctAnswersCount * 100 / totalAnsweredCount) + "%"
                    );
                })
                .toList();
    }
}
