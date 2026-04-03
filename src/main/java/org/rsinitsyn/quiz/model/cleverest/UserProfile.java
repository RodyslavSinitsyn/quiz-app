package org.rsinitsyn.quiz.model.cleverest;

import java.util.Optional;

public record UserProfile(String username,
                          String color,
                          Optional<String> photoFilename) {

    public UserProfile withColorAndAvatar(String photoUrl) {
        return new UserProfile(username, color, Optional.ofNullable(photoUrl));
    }
}
