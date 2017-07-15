package com.chdc.comicsreader.utils;

import java.util.Objects;

/**
 * Created by Wen on 2017/7/15.
 */

public final class Utils {
    public static String stringJoin(CharSequence delimiter, CharSequence[] elements, int startIndex, int length) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        // Number of elements not likely worth Arrays.stream overhead.
        StringBuilder joiner = new StringBuilder();
        for(int i = startIndex; i < startIndex + length - 1; i++){
            joiner.append(elements[i]);
            joiner.append(delimiter);
        }
        joiner.append(elements[startIndex + length - 1]);
        return joiner.toString();
    }

    public static String stringJoin(CharSequence delimiter, CharSequence[] elements) {
        return stringJoin(delimiter, elements, 0, elements.length);
    }
}
