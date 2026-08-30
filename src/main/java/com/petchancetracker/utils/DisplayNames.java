package com.petchancetracker.utils;

public class DisplayNames {
    public static String format(Enum<?> value) {
        if (value == null) {
            return null;
        }
        String name = value.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}