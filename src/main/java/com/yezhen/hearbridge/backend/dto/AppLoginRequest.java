package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppLoginRequest {

    private String username;

    private String password;
}
