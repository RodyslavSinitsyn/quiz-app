package org.rsinitsyn.quiz.utils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Profiles {
    DEV("dev"),
    PROD("prod");

    public final String value;
}
