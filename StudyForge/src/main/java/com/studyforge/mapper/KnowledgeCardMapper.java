package com.studyforge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyforge.entity.KnowledgeCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeCardMapper extends BaseMapper<KnowledgeCard> {

    @Select("SELECT * FROM knowledge_card WHERE document_id = #{documentId} ORDER BY id ASC")
    List<KnowledgeCard> listByDocument(@Param("documentId") Long documentId);
}
