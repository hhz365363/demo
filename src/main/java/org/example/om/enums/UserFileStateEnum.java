package org.example.om.enums;

public enum UserFileStateEnum {
    SAVE(0, "已保存"),
    DELETE(1, "已删除");
    private Integer state;
    private String desc;

    UserFileStateEnum(Integer state, String desc) {
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
