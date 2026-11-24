package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class NationalHolidayTest {
    @Test
    public void nationalHolidayExposesItsValueObject() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        NationalHoliday holiday = new NationalHoliday(date, "Hari Tahun Baru");

        assertEquals(date, holiday.getDate());
        assertEquals("Hari Tahun Baru", holiday.getName());
    }

}
