package org.example.service;

import org.example.om.schema.FileChunk;

import java.util.List;

public interface FileChunkService {
    List<Long> getChunkIndexList(Long taskId);

    FileChunk getChunk(Long taskId, Long chunkIndex);

    void insertChunk(FileChunk fileChunk);

    void clearChunk(Long taskId, String localParent, Long totalCount);
}
