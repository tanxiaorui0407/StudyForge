package com.studyforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyforge.entity.DocumentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文档表 Mapper。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentRecord> {

    /** 按文件哈希查询已成功处理过的文档（用于缓存复用，省 token） */
    @Select("SELECT id FROM document WHERE file_hash = #{hash} AND status = 'SUCCEEDED' ORDER BY id DESC LIMIT 1")
    Long findSucceededIdByHash(@Param("hash") String hash);
}
