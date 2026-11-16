package org.example.scheduled;

import org.example.om.schema.UserFile;
import org.example.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserFileClearTask {

    @Autowired
    private UserFileService userFileService;

    /**
     * 每小时执行一次，删除前100条体积最大的文件
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void execute() {
        List<UserFile> list = userFileService.get100DeletedList();
        // 清理磁盘
        for (UserFile userFile : list) {
            try {
                clearDisk(userFile);
            } catch (IOException e) {
                throw new RuntimeException("删除磁盘文件失败，userFile对象：" + userFile, e);
            }
        }
        // 清理数据库
        List<Long> idList = list
            .stream()
            .map(UserFile::getId)
            .collect(Collectors.toList());
        userFileService.deleteFiles(idList);
    }

    private void clearDisk(UserFile userFile) throws IOException {
        String localPath = userFile.getLocalPath();
        String localParent = userFile.getLocalParent();
        Files.deleteIfExists(Paths.get(localPath));
        Files.deleteIfExists(Paths.get(localParent));
    }
}
