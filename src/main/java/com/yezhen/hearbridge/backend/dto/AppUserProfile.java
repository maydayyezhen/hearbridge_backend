package com.yezhen.hearbridge.backend.dto;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppUserProfile {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    public static AppUserProfile from(AppUser user, MinioProperties minioProperties) {
        return new AppUserProfile(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                minioProperties.buildObjectUrl(user.getAvatarUrl())
        );
    }
}
