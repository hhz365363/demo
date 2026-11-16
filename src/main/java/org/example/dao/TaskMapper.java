package org.example.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.om.schema.UploadTask;

import java.util.List;

@Mapper
public interface TaskMapper {
    @Insert("""
        insert into upload_task (user_id,original_file_name,local_file_name,file_size,local_parent,local_path,state,total_count) 
        values 
        (#{userId},#{originalFileName},#{localFileName},#{fileSize},#{localParent},#{localPath},#{state},#{totalCount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertTask(UploadTask uploadTask);

    @Select("select * from upload_task where id = #{taskId}")
    UploadTask getById(Long taskId);

    @Update("update upload_task set state = #{state} where id = #{taskId}")
    void updateTaskState(@Param("taskId") Long taskId, @Param("state") Integer state);

    @Select("select * from upload_task where user_id = #{userId} and state = #{state}")
    List<UploadTask> getUnfinishedTaskList(@Param("userId") Integer userId, @Param("state") Integer state);

    @Select("select * from upload_task where state = #{state}")
    List<UploadTask> getCloseTaskList(Integer state);

    void deleteTaskList(@Param("idList") List<Long> idList);
}
