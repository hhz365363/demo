package org.example.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.om.schema.UserFile;

import java.util.List;

@Mapper
public interface UserFileMapper {
    @Insert("""
        insert into user_file (user_id,original_file_name,local_file_name,file_size,local_parent,local_path,hash,state,total_count) 
        values 
        (#{userId},#{originalFileName},#{localFileName},#{fileSize},#{localParent},#{localPath},#{hash},#{state},#{totalCount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUserFile(UserFile userFile);

    @Select("select * from user_file where state = #{state} order by file_size desc limit #{limit}")
    List<UserFile> get100DeletedList(@Param("state") Integer state, @Param("limit") int limit);

    void deleteFiles(@Param("idList") List<Long> idList);
}