package org.example.service.impl;

import org.example.dao.UserFileMapper;
import org.example.om.enums.UserFileStateEnum;
import org.example.om.schema.UserFile;
import org.example.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFileServiceImpl implements UserFileService {

    @Autowired
    private UserFileMapper userFileMapper;

    @Override
    public void insertUserFile(UserFile userFile) {
        userFileMapper.insertUserFile(userFile);
    }

    @Override
    public List<UserFile> get100DeletedList() {
        return userFileMapper.get100DeletedList(UserFileStateEnum.DELETE.getState(), 100);
    }

    @Override
    public void deleteFiles(List<Long> idList) {
        if (!idList.isEmpty()) {
            userFileMapper.deleteFiles(idList);
        }
    }
}
