package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model;

import java.time.LocalDate;

public class NationalHoliday {
    private final LocalDate date;
    private final String name;

    public NationalHoliday(LocalDate date, String name) {
        this.date = date;
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }
}
