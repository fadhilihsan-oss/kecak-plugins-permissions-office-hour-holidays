package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.repository;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HolidayRepositoryTest {
    @Test
    public void acceptsSimpleSqlIdentifiers() {
        HolidayRepository.validateIdentifier("app_holiday_calendar", "holiday table");
        HolidayRepository.validateIdentifier("holiday_date", "holiday date column");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSqlExpressionsAsTableNames() {
        HolidayRepository.validateIdentifier("calendar; DROP TABLE users", "holiday table");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyColumnNames() {
        HolidayRepository.validateIdentifier("", "holiday date column");
    }

    @Test
    public void parsesActiveAndInactiveHolidayStatusValues() {
        assertTrue(HolidayRepository.parseActiveStatus("Aktif"));
        assertTrue(HolidayRepository.parseActiveStatus("true"));
        assertTrue(HolidayRepository.parseActiveStatus("1"));
        assertFalse(HolidayRepository.parseActiveStatus("Tidak Aktif"));
        assertFalse(HolidayRepository.parseActiveStatus("false"));
        assertFalse(HolidayRepository.parseActiveStatus(""));
        assertFalse(HolidayRepository.parseActiveStatus(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownHolidayStatusValues() {
        HolidayRepository.parseActiveStatus("pending");
    }

    @Test
    public void supportsAllRecognizedActiveStatusAliases() {
        for (String value : new String[]{
                "active", "aktif", "yes", "Y", "on", "checked", "1"}) {
            assertTrue(HolidayRepository.parseActiveStatus(value));
        }
    }

    @Test
    public void supportsAllRecognizedInactiveStatusAliases() {
        for (String value : new String[]{
                "inactive", "inaktif", "non-aktif", "non_active", "false",
                "0", "no", "N", "off", "unchecked", "tidak aktif"}) {
            assertFalse(HolidayRepository.parseActiveStatus(value));
        }
    }

    @Test
    public void rejectsWhitespaceSqlIdentifiers() {
        try {
            HolidayRepository.validateIdentifier("   ", "holiday table");
            fail("Expected whitespace identifier to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid holiday table"));
        }
        try {
            HolidayRepository.validateIdentifier(null, "holiday column");
            fail("Expected null identifier to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid holiday column"));
        }
    }
}
