package org.example.om.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UploadDTO {
    private MultipartFile file;
    private String hash;
    private Long taskId;
    private Long chunkIndex;
}
