package org.example.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.om.schema.FileChunk;

import java.util.List;

@Mapper
public interface FileChunkMapper {
    @Insert("""
        insert into file_chunk 
        (task_id,hash,chunk_index,size) 
        values (#{taskId},#{hash},#{chunkIndex},#{size})
        """)
    void insertChunk(FileChunk fileChunk);

    @Select("select chunk_index from file_chunk where task_id = #{taskId} order by chunk_index asc")
    List<Long> getChunkIndexList(Long taskId);

    @Select("select * from file_chunk where task_id = #{taskId} and chunk_index = #{chunkIndex}")
    FileChunk getChunk(@Param("taskId") Long taskId, @Param("chunkIndex") Long chunkIndex);
}
