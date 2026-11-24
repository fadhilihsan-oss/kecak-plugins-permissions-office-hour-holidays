package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class DayOfWeekAndHolidayValidator {
    public Set<DayOfWeek> parseAllowedDays(String value) {
        Set<DayOfWeek> allowedDays = EnumSet.noneOf(DayOfWeek.class);
        if (value == null || value.trim().isEmpty()) {
            return allowedDays;
        }

        for (String day : value.split(";")) {
            String normalizedDay = day.trim().toUpperCase(Locale.ROOT);
            if (!normalizedDay.isEmpty()) {
                allowedDays.add(DayOfWeek.valueOf(normalizedDay));
            }
        }
        return allowedDays;
    }

    public boolean isAllowed(LocalDate date,
                             Set<DayOfWeek> allowedDays,
                             boolean nationalHoliday,
                             boolean allowNationalHoliday) {
        if (nationalHoliday) {
            return allowNationalHoliday;
        }
        return allowedDays.contains(date.getDayOfWeek());
    }

    public boolean isAllowedWithHolidayStatus(LocalDate date,
                                              Set<DayOfWeek> allowedDays,
                                              Boolean holidayStatusActive,
                                              boolean allowNationalHoliday) {
        if (holidayStatusActive == null || !holidayStatusActive) {
            return allowedDays.contains(date.getDayOfWeek());
        }
        return allowNationalHoliday;
    }
}
