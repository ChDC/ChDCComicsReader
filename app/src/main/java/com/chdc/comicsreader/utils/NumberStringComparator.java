package com.chdc.comicsreader.utils;

import java.util.Comparator;

/**
 * Created by Wen on 2017/7/11.
 */

public class NumberStringComparator implements Comparator<String> {

    public static NumberStringComparator INSTANCE = new NumberStringComparator();

//    private static Pattern CHINESE_NUMBER_PATTERN = Pattern.compile("([零十]*[一二三四五六七八九][零十百千万亿]?)+");

    @Override
    public int compare(String s1, String s2) {

        if(s1 == s2)
            return 0;
        if(s1 == null)
            return -1;
        if(s2 == null)
            return 1;

        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();

        int i,j;
        for(i = 0, j = 0; i < s1.length() && j < s2.length(); ){
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(j);
            if(c1 == c2) {
                i++;
                j++;
            }
            else if(Character.isDigit(c1) && Character.isDigit(c2)){
                // 向后取得所有的数字
                StringBuilder d1 = new StringBuilder();
                do{
                    d1.append(s1.charAt(i));
                    i++;
                }
                while(i < s1.length() && Character.isDigit(s1.charAt(i)));
                StringBuilder d2 = new StringBuilder();
                do{
                    d2.append(s2.charAt(j));
                    j++;
                }
                while (j < s2.length() && Character.isDigit(s2.charAt(j)));
                int i1 = Integer.valueOf(d1.toString());
                int i2 = Integer.valueOf(d2.toString());
                if(i1 != i2)
                    return i1 - i2;
            }
            else
                return c1 - c2;
        }
        if(i >= s1.length() && j >= s2.length())
            return 0;
        if(i < s1.length())
            return 1;
        return -1;
    }
}
