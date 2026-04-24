package com.yezhen.hearbridge.backend.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppUser {

    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;
}
