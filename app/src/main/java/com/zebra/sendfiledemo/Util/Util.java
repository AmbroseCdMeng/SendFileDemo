package com.zebra.sendfiledemo.Util;

import java.util.UUID;

public class Util {

    public static String GUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        String uuidStr = str.replace("-", "");
        return uuidStr;
    }
}
