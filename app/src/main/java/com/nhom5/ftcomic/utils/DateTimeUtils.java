package com.nhom5.ftcomic.utils;

import android.text.format.DateUtils;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    public static String formatChapterDate(String isoDateStr) {
        if (isoDateStr == null || isoDateStr.isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(isoDateStr);
            long millis = odt.toInstant().toEpochMilli();
            long now = System.currentTimeMillis();
            long diff = now - millis;
            
            // Up truyện dưới 1 tuần thì hiển thị dạng gần đây
            if (diff < 7 * 24 * 60 * 60 * 1000L && diff >= 0) {
                long seconds = diff / 1000L;
                long minutes = seconds / 60L;
                long hours = minutes / 60L;
                long days = hours / 24L;

                if (minutes < 1) {
                    return "Vừa xong";
                } else if (minutes < 60) {
                    return minutes + " phút trước";
                } else if (hours < 24) {
                    return hours + " giờ trước";
                } else {
                    return days + " ngày trước";
                }
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return odt.format(formatter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return isoDateStr;
        }
    }
}
