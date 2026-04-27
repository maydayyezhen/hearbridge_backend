package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper {

    AppUser selectById(@Param("id") Long id);

    AppUser selectByUsername(@Param("username") String username);

    int insert(AppUser user);

    int updateProfileById(@Param("id") Long id,
                          @Param("nickname") String nickname,
                          @Param("avatarUrl") String avatarUrl);

    int updateAvatarById(@Param("id") Long id,
                         @Param("avatarUrl") String avatarUrl);

    int updatePasswordById(@Param("id") Long id,
                           @Param("passwordHash") String passwordHash);
}
