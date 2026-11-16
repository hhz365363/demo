package org.example.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class StringUtil {
    public static boolean equals(String a, String b) {
        return a.equals(b);
    }

    public static String bytes2HexStr(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static String digest(String algorithm, byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] bytes = digest.digest(input);
        return StringUtil.bytes2HexStr(bytes);
    }
}
