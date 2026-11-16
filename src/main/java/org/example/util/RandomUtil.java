package org.example.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RandomUtil {
    // 用时间戳作为初始值，逐个自增保证唯一
    private static AtomicLong atomicTime = new AtomicLong(System.currentTimeMillis());

    public static String getRandomStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getRandomUniqueID() {
        // ++i
        return String.valueOf(atomicTime.addAndGet(1));
    }
}
