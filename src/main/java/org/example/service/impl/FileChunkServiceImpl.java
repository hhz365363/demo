package org.example.service.impl;

import org.example.dao.FileChunkMapper;
import org.example.om.schema.FileChunk;
import org.example.service.FileChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FileChunkServiceImpl implements FileChunkService {

    @Autowired
    private FileChunkMapper fileChunkMapper;

    @Override
    public List<Long> getChunkIndexList(Long taskId) {
        return fileChunkMapper.getChunkIndexList(taskId);
    }

    @Override
    public FileChunk getChunk(Long taskId, Long chunkIndex) {
        return fileChunkMapper.getChunk(taskId, chunkIndex);
    }

    @Override
    public void insertChunk(FileChunk fileChunk) {
        fileChunkMapper.insertChunk(fileChunk);
    }

    @Override
    public void clearChunk(Long taskId, String localParent, Long totalCount) {
        for (long i = 1L; i <= totalCount; i++) {
            String chunkPath = localParent + "\\" + i;
            try {
                Files.deleteIfExists(Paths.get(chunkPath));
            } catch (IOException e) {
                throw new RuntimeException("清理分片失败，taskId：" + taskId + "，分片序号：" + i, e);
            }
        }
    }
}