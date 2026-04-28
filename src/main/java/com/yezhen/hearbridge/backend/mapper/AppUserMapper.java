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

    int updateRecentPracticeById(@Param("id") Long id,
                                 @Param("resourceId") Long resourceId,
                                 @Param("resourceCode") String resourceCode,
                                 @Param("chineseName") String chineseName,
                                 @Param("sigmlUrl") String sigmlUrl,
                                 @Param("coverUrl") String coverUrl);
}
