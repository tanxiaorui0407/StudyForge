package com.studyforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyforge.entity.Chunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChunkMapper extends BaseMapper<Chunk> {

    @Select("SELECT * FROM chunk WHERE document_id = #{documentId} AND strategy = #{strategy} ORDER BY chunk_index ASC")
    List<Chunk> listByDocumentAndStrategy(@Param("documentId") Long documentId,
                                          @Param("strategy") String strategy);

    @Select("SELECT * FROM chunk WHERE document_id = #{documentId} ORDER BY chunk_index ASC")
    List<Chunk> listByDocument(@Param("documentId") Long documentId);
}
