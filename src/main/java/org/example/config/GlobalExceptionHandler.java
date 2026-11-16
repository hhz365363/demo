package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.om.vo.ResultVO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResultVO<Void> exception(Exception e) {
        log.error("系统出现异常：" + e.getMessage(), e);
        return ResultVO.lose(e.getMessage());
    }
}
