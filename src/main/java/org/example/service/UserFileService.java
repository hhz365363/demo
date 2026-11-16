package org.example.service;

import org.example.om.schema.UserFile;

import java.util.List;

public interface UserFileService {
    void insertUserFile(UserFile userFile);

    List<UserFile> get100DeletedList();

    void deleteFiles(List<Long> idList);
}
