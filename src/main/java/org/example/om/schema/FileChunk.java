package org.example.om.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FileChunk {
    private Long id;
    private Long taskId;
    private String hash;
    private Long chunkIndex;
    private Long size;
}