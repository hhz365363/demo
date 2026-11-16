package org.example.om.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResultVO<T> {
    private Integer state;
    public static final int WIN = 1;
    public static final int LOSE = 0;

    private String msg;
    private T data;

    public static ResultVO<Void> win() {
        return new ResultVO<>(WIN, null, null);
    }

    public static <T> ResultVO<T> win(T data) {
        return new ResultVO<>(WIN, null, data);
    }

    public static ResultVO<Void> lose(String msg) {
        return new ResultVO<>(LOSE, msg, null);
    }
}