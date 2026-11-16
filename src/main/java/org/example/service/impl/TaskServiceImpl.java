package org.example.service.impl;

import org.example.Main;
import org.example.dao.TaskMapper;
import org.example.om.dto.MergeDTO;
import org.example.om.dto.NewTaskDTO;
import org.example.om.dto.UploadDTO;
import org.example.om.enums.TaskStateEnum;
import org.example.om.enums.UserFileStateEnum;
import org.example.om.schema.FileChunk;
import org.example.om.schema.UploadTask;
import org.example.om.schema.UserFile;
import org.example.om.vo.MergeVO;
import org.example.service.FileChunkService;
import org.example.service.TaskService;
import org.example.service.UserFileService;
import org.example.util.RandomUtil;
import org.example.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.example.Main.FILE_PATH;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private FileChunkService fileChunkService;

    @Autowired
    private UserFileService userFileService;

    @Autowired
    private TaskService self;

    @Autowired
    private ThreadPoolExecutor clearChunkExecutor;

    @Override
    public UploadTask newTask(NewTaskDTO dto) throws IOException {
        // 创建上传任务
        UploadTask uploadTask = new UploadTask();
        BeanUtils.copyProperties(dto, uploadTask);
        // 创建目录
        String uniqueID = RandomUtil.getRandomUniqueID();
        String localParent = String.join("\\", FILE_PATH, uniqueID);
        Files.createDirectories(Paths.get(localParent));
        uploadTask.setLocalParent(localParent);
        // 用UUID作为文件名，不需要后缀
        String localFileName = RandomUtil.getRandomStr();
        uploadTask.setLocalFileName(localFileName);
        // 文件路径：C:\Users\admin\Desktop\files\{uniqueID}\{UUID}
        uploadTask.setLocalPath(String.join("\\", FILE_PATH, uniqueID, localFileName));
        // 计算分片总数
        Long fileSize = dto.getFileSize();
        if (fileSize <= Main.CHUNK_SIZE) {
            uploadTask.setTotalCount(1L);
        } else {
            long totalCount = fileSize / Main.CHUNK_SIZE + (fileSize % Main.CHUNK_SIZE == 0 ? 0 : 1);
            uploadTask.setTotalCount(totalCount);
        }
        uploadTask.setState(TaskStateEnum.ING.getState());
        // 保存到数据库，自动生成主键
        taskMapper.insertTask(uploadTask);
        return uploadTask;
    }

    @Override
    public List<Long> keep(Long taskId) {
        UploadTask task = taskMapper.getById(taskId);
        checkTask(task);
        // 遍历筛选未上传的分片
        List<Long> chunkIndexList = fileChunkService.getChunkIndexList(taskId);
        return findMissChunk(task, chunkIndexList);
    }

    @Override
    public void upload(UploadDTO dto) throws IOException, NoSuchAlgorithmException {
        UploadTask task = taskMapper.getById(dto.getTaskId());
        checkTask(task);
        MultipartFile file = dto.getFile();
        String hash = dto.getHash();
        if (file.isEmpty() || file.getSize() <= 0) {
            throw new RuntimeException("分片不能为空");
        }
        // 计算哈希值是否相同
        if (!StringUtil.equals(hash, StringUtil.digest("SHA-256", file.getBytes()))) {
            throw new RuntimeException("分片文件内容错误");
        }
        FileChunk chunk = fileChunkService.getChunk(dto.getTaskId(), dto.getChunkIndex());
        if (chunk == null) {
            String localParent = task.getLocalParent();
            String path = localParent + "\\" + dto.getChunkIndex();
            // 文件不存在就创建，创建再写入
            // 文件已存在就清空，清空再写入
            Files.write(Paths.get(path), file.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
            // 写完文件再写入数据库
            FileChunk fileChunk = new FileChunk();
            fileChunk.setChunkIndex(dto.getChunkIndex());
            fileChunk.setTaskId(dto.getTaskId());
            fileChunk.setHash(dto.getHash());
            fileChunk.setSize(file.getSize());
            fileChunkService.insertChunk(fileChunk);
        }
    }

    @Override
    public MergeVO merge(MergeDTO dto) throws NoSuchAlgorithmException, IOException {
        UploadTask task = taskMapper.getById(dto.getTaskId());
        checkTask(task);
        List<Long> chunkIndexList = fileChunkService.getChunkIndexList(task.getId());
        List<Long> missList = findMissChunk(task, chunkIndexList);
        // 还有未上传的分片
        if (!missList.isEmpty()) {
            return new MergeVO(null, missList);
        }
        String localPath = task.getLocalPath();
        String localParent = task.getLocalParent();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        // 合并分片
        for (Long index : chunkIndexList) {
            byte[] bytes = Files.readAllBytes(Paths.get(localParent + "\\" + index));
            Files.write(Paths.get(localPath), bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            digest.update(bytes);
        }
        String hash = StringUtil.bytes2HexStr(digest.digest());
        if (!StringUtil.equals(hash, dto.getHash())) {
            throw new RuntimeException("文件完整性校验失败");
        }
        // 合并完成，更新Task状态为finish，并且插入到UserFile中
        task.setState(TaskStateEnum.FINISH.getState());
        task.setHash(hash);
        UserFile userFile = new UserFile();
        BeanUtils.copyProperties(task, userFile);
        userFile.setState(UserFileStateEnum.SAVE.getState());
        self.syncUserFile(task, userFile);
        // 异步清理分片文件
        clearChunkExecutor.execute(() -> fileChunkService.clearChunk(task.getId(), task.getLocalParent(), task.getTotalCount()));
        return new MergeVO(userFile.getId(), null);
    }

    @Override
    public void close(Long taskId) {
        taskMapper.updateTaskState(taskId, TaskStateEnum.CLOSE.getState());
    }

    @Override
    public List<UploadTask> getUnfinishedTaskList(Integer userId) {
        return taskMapper.getUnfinishedTaskList(userId, TaskStateEnum.ING.getState());
    }

    @Transactional
    @Override
    public void syncUserFile(UploadTask task, UserFile userFile) {
        taskMapper.updateTaskState(task.getId(), task.getState());
        userFileService.insertUserFile(userFile);
    }

    @Override
    public List<UploadTask> getCloseTaskList() {
        return taskMapper.getCloseTaskList(TaskStateEnum.CLOSE.getState());
    }

    @Override
    public void deleteTaskList(List<Long> idList) {
        if (!idList.isEmpty()) {
            taskMapper.deleteTaskList(idList);
        }
    }

    private void checkTask(UploadTask task) {
        if (task == null) {
            throw new RuntimeException("上传任务不存在");
        }
        Integer state = task.getState();
        if (state.equals(TaskStateEnum.CLOSE.getState())) {
            throw new RuntimeException("上传任务不存在");
        } else if (state.equals(TaskStateEnum.FINISH.getState())) {
            throw new RuntimeException("上传任务已完成");
        }
    }

    private List<Long> findMissChunk(UploadTask task, List<Long> chunkIndexList) {
        Long totalCount = task.getTotalCount();
        ArrayList<Long> missList = new ArrayList<>();
        for (long i = 1L; i <= totalCount; i++) {
            if (!chunkIndexList.contains(i)) {
                missList.add(i);
            }
        }
        return missList;
    }
}
