package org.example.om.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserFile {
    private Long id;
    private Integer userId;
    private String originalFileName;
    private String localFileName;
    private Long fileSize;
    private String localParent;
    private String localPath;
    private String hash;
    private Integer state;
    private Long totalCount;
}