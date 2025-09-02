package com.growplan.domain.recommendation.etf;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MarketClock {
    public enum Session { OPEN, CLOSED }

    public static Session krxSessionNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        DayOfWeek d = now.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return Session.CLOSED;
        LocalTime t = now.toLocalTime();
        // KRX 정규장 09:00~15:30
        return (t.isAfter(LocalTime.of(8,59)) && t.isBefore(LocalTime.of(15,31))) ? Session.OPEN : Session.CLOSED;
    }

    public static Session usSessionNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York")); // DST 자동 반영
        DayOfWeek d = now.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return Session.CLOSED;
        LocalTime t = now.toLocalTime();
        // NY 정규장 09:30~16:00 (프리/애프터는 여기선 제외)
        return (t.isAfter(LocalTime.of(9,29)) && t.isBefore(LocalTime.of(16,1))) ? Session.OPEN : Session.CLOSED;
    }

    public static boolean isDomesticKey(String key){ return key.matches("^\\d{6}$"); }
    public static boolean isUsKey(String key){ return key.contains("."); } // "ARCX.SPY" 등
}