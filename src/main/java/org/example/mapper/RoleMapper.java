package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.Role;

import java.util.List;

@Mapper
public interface RoleMapper {

    Role findById(@Param("id") Long id);

    Role findByCode(@Param("code") String code);

    List<Role> findAll();

    int insert(Role role);

    int updateById(Role role);
}
