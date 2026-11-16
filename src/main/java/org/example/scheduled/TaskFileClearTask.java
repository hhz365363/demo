package org.example.scheduled;

import org.example.om.schema.UploadTask;
import org.example.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TaskFileClearTask {
    @Autowired
    private TaskService taskService;

    /**
     * 每小时执行一次，删除被取消的上传文件
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void execute() {
        List<UploadTask> list = taskService.getCloseTaskList();
        for (UploadTask task : list) {
            try {
                clearDisk(task);
            } catch (IOException e) {
                throw new RuntimeException("删除磁盘文件失败，uploadTask对象：" + task, e);
            }
        }
        // 清理数据库
        List<Long> idList = list
            .stream()
            .map(UploadTask::getId)
            .collect(Collectors.toList());
        taskService.deleteTaskList(idList);
    }

    private void clearDisk(UploadTask task) throws IOException {
        String localParent = task.getLocalParent();
        Path dir = Paths.get(localParent);
        if (Files.notExists(dir)) {
            return;
        }
        Stream<Path> stream = Files.list(dir);
        stream.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new RuntimeException("删除磁盘文件失败，文件路径：" + path, e);
            }
        });
        stream.close();
        Files.deleteIfExists(dir);
    }
}