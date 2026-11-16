package org.example.om.enums;

import lombok.Data;

public enum TaskStateEnum {
    ING(0, "未完成上传"),
    CLOSE(1, "取消上传"),
    FINISH(2, "完成上传");

    private Integer state;
    private String desc;

    TaskStateEnum(Integer state, String desc) {
        this.state = state;
        this.desc = desc;
    }


    public Integer getState() {
        return state;
    }

    public String getDesc() {
        return desc;
    }
}
