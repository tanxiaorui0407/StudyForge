package com.studyforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyforge.entity.ApiCallLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {
}
