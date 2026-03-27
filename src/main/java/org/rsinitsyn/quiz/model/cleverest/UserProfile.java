package org.rsinitsyn.quiz.model.cleverest;

import com.vaadin.flow.server.StreamResource;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.rsinitsyn.quiz.utils.QuizUtils.createStreamResourceForPhoto;

public record UserProfile(String username, String color, byte[] avatar, Optional<StreamResource> avatarResource) {

    public UserProfile(final String username, final String color, final byte[] avatar) {
        this(username, color, avatar, Optional.ofNullable(avatar)
                .map(data -> createStreamResourceForPhoto(randomUUID().toString(), data)));
    }

    public UserProfile withColorAndAvatar(String color, byte[] avatar) {
        return new UserProfile(username, color, avatar, avatarResource);
    }
}
