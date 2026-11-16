package org.example.service;

import org.example.om.dto.MergeDTO;
import org.example.om.dto.NewTaskDTO;
import org.example.om.dto.UploadDTO;
import org.example.om.schema.UploadTask;
import org.example.om.schema.UserFile;
import org.example.om.vo.MergeVO;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface TaskService {
    UploadTask newTask(NewTaskDTO dto) throws IOException;

    List<Long> keep(Long taskId);

    void upload(UploadDTO dto) throws IOException, NoSuchAlgorithmException;

    void close(Long taskId);

    MergeVO merge(MergeDTO dto) throws NoSuchAlgorithmException, IOException;

    List<UploadTask> getUnfinishedTaskList(Integer userId);

    void syncUserFile(UploadTask task, UserFile userFile);

    List<UploadTask> getCloseTaskList();

    void deleteTaskList(List<Long> idList);
}
