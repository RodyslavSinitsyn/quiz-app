package org.rsinitsyn.quiz.model.cleverest;

import java.util.Optional;
import java.util.UUID;

public record UserProfile(UUID id,
                          String username,
                          String color,
                          Optional<String> photoFilename) {

    public UserProfile withColorAndAvatar(String color, String photoUrl) {
        return new UserProfile(this.id(), this.username, color, Optional.ofNullable(photoUrl));
    }
}
