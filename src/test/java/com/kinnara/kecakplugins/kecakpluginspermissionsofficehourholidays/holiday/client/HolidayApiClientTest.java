package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.client;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model.NationalHoliday;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HolidayApiClientTest {
    private final HolidayApiClient apiClient = new HolidayApiClient();

    @Test
    public void importsConfirmedNationalHolidaysButExcludesCollectiveAndTentativeDates() {
        String response = "{"
                + "\"2026-01-01\":{\"summary\":\"Hari Tahun Baru\"},"
                + "\"2026-02-16\":{\"summary\":\"Cuti Bersama Tahun Baru Imlek\"},"
                + "\"2026-03-25\":{\"summary\":\"Joint Holiday for Good Friday\"},"
                + "\"2026-04-03\":{\"summary\":\"Wafat Isa Almasih\"},"
                + "\"2026-05-01\":{\"summary\":\"Hari Buruh (belum pasti)\"},"
                + "\"2027-01-01\":{\"summary\":\"Hari Tahun Baru\"},"
                + "\"info\":{\"author\":\"APIHariLibur_V2\"}"
                + "}";

        List<NationalHoliday> holidays = apiClient.parseNationalHolidays(response, 2026);
        List<LocalDate> dates = holidays.stream()
                .map(NationalHoliday::getDate)
                .collect(Collectors.toList());

        assertEquals(2, holidays.size());
        assertEquals("Hari Tahun Baru", holidays.stream()
                .filter(holiday -> LocalDate.of(2026, 1, 1).equals(holiday.getDate()))
                .findFirst().get().getName());
        assertTrue(dates.contains(LocalDate.of(2026, 4, 3)));
        assertFalse(dates.contains(LocalDate.of(2026, 2, 16)));
        assertFalse(dates.contains(LocalDate.of(2026, 5, 1)));
    }

    @Test
    public void rejectsInvalidJson() {
        try {
            apiClient.parseNationalHolidays("{", 2026);
            fail("Expected invalid JSON to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("invalid JSON"));
        }
    }

    @Test
    public void rejectsInvalidDateKey() {
        try {
            apiClient.parseNationalHolidays("{\"2026-99-99\":{\"summary\":\"Invalid\"}}", 2026);
            fail("Expected invalid date to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("invalid date"));
        }
    }

    @Test
    public void rejectsEntryWithoutName() {
        try {
            apiClient.parseNationalHolidays("{\"2026-01-01\":{\"summary\":\" \"}}", 2026);
            fail("Expected missing name to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("no name"));
        }
    }

    @Test
    public void rejectsYearWithNoConfirmedHoliday() {
        try {
            apiClient.parseNationalHolidays(
                    "{\"2026-02-16\":{\"summary\":\"Cuti Bersama\"}}", 2026);
            fail("Expected year without confirmed holidays to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("no confirmed"));
        }
    }

    @Test
    public void rejectsEntryWithoutJsonObject() {
        try {
            apiClient.parseNationalHolidays("{\"2026-01-01\":null}", 2026);
            fail("Expected missing object to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("no name"));
        }
    }
}
