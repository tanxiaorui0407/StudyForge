package com.studyforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyforge.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
