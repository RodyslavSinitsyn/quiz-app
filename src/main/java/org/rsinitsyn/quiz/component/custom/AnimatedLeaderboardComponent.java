package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;

import java.time.Duration;
import java.util.List;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.*;

public class AnimatedLeaderboardComponent extends VerticalLayout {

    private static final String ROW_HEIGHT_PX = "56";

    public AnimatedLeaderboardComponent(
            final List<UserStateSnapshot> initialState,
            final List<List<UserStateSnapshot>> history,
            final Duration delay) {

        setWidthFull();
        addClassName("animated-leaderboard");
        getStyle()
                .set("position", "relative")
                .set("overflow", "hidden");

        final var container = new Div();
        container.setId("leaderboard-container");
        container.getStyle()
                .set("position", "relative")
                .set("width", "100%");
        container.getStyle().set("height", (initialState.size() * Integer.parseInt(ROW_HEIGHT_PX)) + "px");

        initialState.forEach(snapshot -> {
            final var row = buildRow(snapshot, initialState.size());
            positionRow(row, snapshot.position());
            container.add(row);
        });

        add(container);
        addClassName(LumoUtility.FontSize.MEDIUM);

        final var historyJson = serializeHistory(history);
        getElement().executeJs(buildAnimationScript(historyJson, delay));
    }

    private Div buildRow(UserStateSnapshot snapshot, int totalUsers) {
        final var row = new Div();
        row.addClassName("leaderboard-row");
        row.getElement().setAttribute("data-username", snapshot.username());

        Span emojiSpan;
        if (snapshot.position() == 1) {
            emojiSpan = emojiSmall(Emoji.randomGreat().value);
        } else if (snapshot.position() == totalUsers) {
            emojiSpan = emojiSmall(Emoji.randomBad().value);
        } else {
            emojiSpan = emojiSmall(Emoji.randomGood().value);
        }

        final var positionSpan = new Span(String.valueOf(snapshot.position()));
        positionSpan.addClassName("position-label");

        final var profileComponent = userProfile(snapshot.profile());

        final var scoreSpan = new Span(String.valueOf(snapshot.score()));
        scoreSpan.addClassName("score-label");

        final var inner = horizontalLayoutBetween(emojiSpan, positionSpan, profileComponent, scoreSpan);
        row.add(inner);
        return row;
    }

    private void positionRow(Div row, int position) {
        final var topPx = (position - 1) * Integer.parseInt(ROW_HEIGHT_PX);
        row.getStyle()
                .set("position", "absolute")
                .set("width", "100%")
                .set("top", topPx + "px")
                .set("transition", "transform 1s cubic-bezier(0.4, 0, 0.2, 1)");
    }

    private String serializeHistory(List<List<UserStateSnapshot>> roundHistory) {
        final var sb = new StringBuilder("[");
        roundHistory.forEach((snapshots) -> {
            sb.append("[");
            snapshots.forEach(snapshot -> {
                sb.append("{")
                        .append("\"username\":\"").append(snapshot.username()).append("\",")
                        .append("\"position\":").append(snapshot.position()).append(",")
                        .append("\"score\":").append(snapshot.score())
                        .append("},");
            });
            if (!snapshots.isEmpty()) sb.deleteCharAt(sb.length() - 1);
            sb.append("],");
        });
        if (!roundHistory.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private String buildAnimationScript(String historyJson, Duration delay) {
        return """
            (function(container) {
                const history = %s;
                const ROW_HEIGHT = %s;
                const STEP_DELAY = %s;

                function applyRound(round) {
                    round.forEach(function(userState) {
                        const row = container.querySelector('[data-username="' + userState.username + '"]');
                        if (!row) return;
                        const targetY = (userState.position - 1) * ROW_HEIGHT;
                        row.style.transform = 'translateY(' + targetY + 'px)';
                        row.style.top = '0px';

                        const posLabel = row.querySelector('.position-label');
                        if (posLabel) posLabel.textContent = userState.position;

                        const scoreLabel = row.querySelector('.score-label');
                        if (scoreLabel) scoreLabel.textContent = userState.score;
                    });
                }

                let step = 0;
                const interval = setInterval(function() {
                    if (step >= history.length) {
                        clearInterval(interval);
                        return;
                    }
                    applyRound(history[step]);
                    step++;
                }, STEP_DELAY);
            })($0);
            """.formatted(historyJson, ROW_HEIGHT_PX, String.valueOf(delay.toMillis()));
    }
}
