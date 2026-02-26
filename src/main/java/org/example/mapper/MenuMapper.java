package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.Menu;

import java.util.List;

@Mapper
public interface MenuMapper {

    Menu findById(@Param("id") Long id);

    List<Menu> findAll();

    List<Menu> findByParentId(@Param("parentId") Long parentId);

    int insert(Menu menu);

    int updateById(Menu menu);
}
