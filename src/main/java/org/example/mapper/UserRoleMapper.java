package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.UserRole;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    List<UserRole> findByUserId(@Param("userId") Long userId);

    int insert(UserRole userRole);

    int deleteByUserId(@Param("userId") Long userId);

    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
