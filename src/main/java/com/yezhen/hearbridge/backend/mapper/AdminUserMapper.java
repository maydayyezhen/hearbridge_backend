package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员用户 Mapper。
 */
@Mapper
public interface AdminUserMapper {

    /**
     * 根据用户名查询管理员。
     *
     * @param username 用户名
     * @return 管理员用户
     */
    AdminUser selectByUsername(@Param("username") String username);

    /**
     * 根据 ID 查询管理员。
     *
     * @param id 管理员 ID
     * @return 管理员用户
     */
    AdminUser selectById(@Param("id") Long id);

    /**
     * 更新管理员密码。
     *
     * @param id           管理员 ID
     * @param passwordHash 新密码哈希
     * @return 影响行数
     */
    int updatePassword(
            @Param("id") Long id,
            @Param("passwordHash") String passwordHash
    );
}
