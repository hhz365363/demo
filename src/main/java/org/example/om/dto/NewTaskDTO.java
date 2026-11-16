package org.example.om.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NewTaskDTO {
    private Integer userId;
    private String originalFileName;
    private Long fileSize;
}
