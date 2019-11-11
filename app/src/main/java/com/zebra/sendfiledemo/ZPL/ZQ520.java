package com.zebra.sendfiledemo.ZPL;

public class ZQ520 {
    public static final Integer BIT = 8;//  8bit/mm

    public static String Calc(Double i) {
        return String.valueOf(i * BIT);
    }
}
