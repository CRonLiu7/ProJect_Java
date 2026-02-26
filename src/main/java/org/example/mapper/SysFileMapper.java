package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.SysFile;

import java.util.List;

@Mapper
public interface SysFileMapper {

    SysFile findById(@Param("id") Long id);

    List<SysFile> findAll();

    int insert(SysFile file);

    int deleteById(@Param("id") Long id);
}
