package org.example.controller;

import org.example.om.dto.MergeDTO;
import org.example.om.dto.NewTaskDTO;
import org.example.om.dto.UploadDTO;
import org.example.om.schema.UploadTask;
import org.example.om.vo.MergeVO;
import org.example.om.vo.ResultVO;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
public class TaskController {
    @Autowired
    private TaskService taskService;

    /**
     * 全量上传
     *
     * @param dto
     * @return
     */
    @PostMapping("/newTask")
    public ResultVO<UploadTask> newTask(@RequestBody NewTaskDTO dto) throws IOException {
        return ResultVO.win(taskService.newTask(dto));
    }

    /**
     * 断点续传，返回未上传的分片序号
     *
     * @param taskId
     * @return
     */
    @PostMapping("/keep")
    public ResultVO<List<Long>> keep(Long taskId) {
        return ResultVO.win(taskService.keep(taskId));
    }

    /**
     * 分片上传，返回int报告上传状态
     *
     * @param dto
     * @return
     */
    @PostMapping("/upload")
    public ResultVO<Void> upload(UploadDTO dto) throws IOException, NoSuchAlgorithmException {
        taskService.upload(dto);
        return ResultVO.win();
    }

    /**
     * 合并文件
     *
     * @param dto
     * @return
     */
    @PostMapping("/merge")
    public ResultVO<MergeVO> merge(@RequestBody MergeDTO dto) throws NoSuchAlgorithmException, IOException {
        return ResultVO.win(taskService.merge(dto));
    }

    /**
     * 取消上传
     *
     * @param taskId
     * @return
     */
    @PostMapping("/close")
    public ResultVO<Void> close(Long taskId) {
        taskService.close(taskId);
        return ResultVO.win();
    }

    /**
     * 获取未完成的上传任务列表
     *
     * @param userId
     * @return
     */
    @PostMapping("/getUnfinishedTaskList")
    public ResultVO<List<UploadTask>> getUnfinishedTaskList(Integer userId) {
        return ResultVO.win(taskService.getUnfinishedTaskList(userId));
    }
}
