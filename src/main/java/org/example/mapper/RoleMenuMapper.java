package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.RoleMenu;

import java.util.List;

@Mapper
public interface RoleMenuMapper {

    List<RoleMenu> findByRoleId(@Param("roleId") Long roleId);

    int insert(RoleMenu roleMenu);

    int deleteByRoleId(@Param("roleId") Long roleId);
}
