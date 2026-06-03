package com.qinghe.mall.util;

import org.apache.commons.codec.digest.DigestUtils;

public class CommonUtils {

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static String md5(String str) {
        return DigestUtils.md5Hex(str).toUpperCase();
    }
}
