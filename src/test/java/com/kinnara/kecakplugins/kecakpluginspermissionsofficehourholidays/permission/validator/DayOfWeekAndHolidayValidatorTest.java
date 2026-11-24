package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.validator;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DayOfWeekAndHolidayValidatorTest {
    private final DayOfWeekAndHolidayValidator validator = new DayOfWeekAndHolidayValidator();

    @Test
    public void allowsConfiguredDaysUnlessNationalHolidayIsBlocked() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);

        assertTrue(validator.isAllowed(monday, allDays, false, false));
        assertFalse(validator.isAllowed(monday, allDays, true, false));
        assertTrue(validator.isAllowed(monday, allDays, true, true));
    }

    @Test
    public void eachDayHasAnIndependentSetting() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        LocalDate sunday = LocalDate.of(2026, 11, 15);

        assertTrue(validator.isAllowed(monday, EnumSet.of(DayOfWeek.MONDAY), false, false));
        assertFalse(validator.isAllowed(monday, EnumSet.of(DayOfWeek.TUESDAY), false, false));
        assertTrue(validator.isAllowed(saturday, EnumSet.of(DayOfWeek.SATURDAY), false, false));
        assertFalse(validator.isAllowed(sunday, EnumSet.of(DayOfWeek.SATURDAY), false, false));
    }

    @Test
    public void disablingAnyDayBlocksOnlyThatDay() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = monday.plusDays(dayOffset);
            Set<DayOfWeek> allowedDays = EnumSet.allOf(DayOfWeek.class);
            allowedDays.remove(date.getDayOfWeek());

            assertFalse(validator.isAllowed(date, allowedDays, false, false));
        }
    }

    @Test
    public void nationalHolidaySettingOverridesDaySetting() {
        LocalDate sundayHoliday = LocalDate.of(2026, 11, 15);
        LocalDate saturdayHoliday = LocalDate.of(2026, 11, 14);
        Set<DayOfWeek> weekdaysOnly = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);

        assertTrue(validator.isAllowed(sundayHoliday, weekdaysOnly, true, true));
        assertFalse(validator.isAllowed(sundayHoliday, allDays, true, false));
        assertTrue(validator.isAllowed(saturdayHoliday, weekdaysOnly, true, true));
        assertTrue(validator.isAllowed(sundayHoliday, allDays, true, true));
        assertFalse(validator.isAllowed(sundayHoliday, weekdaysOnly, false, true));
    }

    @Test
    public void activeHolidayUsesAllowSettingAndInactiveHolidayUsesAllowedDays() {
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        LocalDate thursday = LocalDate.of(2026, 11, 19);
        Set<DayOfWeek> weekdaysOnly = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        Set<DayOfWeek> thursdayOnly = EnumSet.of(DayOfWeek.THURSDAY);
        Set<DayOfWeek> fridayOnly = EnumSet.of(DayOfWeek.FRIDAY);

        assertTrue(validator.isAllowedWithHolidayStatus(saturday, weekdaysOnly, true, true));
        assertFalse(validator.isAllowedWithHolidayStatus(saturday, weekdaysOnly, false, true));
        assertFalse(validator.isAllowedWithHolidayStatus(saturday, weekdaysOnly, true, false));
        assertFalse(validator.isAllowedWithHolidayStatus(saturday, weekdaysOnly, null, false));
        assertTrue(validator.isAllowedWithHolidayStatus(thursday, thursdayOnly, false, true));
        assertTrue(validator.isAllowedWithHolidayStatus(thursday, thursdayOnly, false, false));
        assertFalse(validator.isAllowedWithHolidayStatus(thursday, fridayOnly, false, true));
    }

    @Test
    public void checkboxGroupValueParsesSemicolonSeparatedWeekdays() {
        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
                validator.parseAllowedDays("MONDAY;WEDNESDAY;SUNDAY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownWeekdayInCheckboxGroupIsRejected() {
        validator.parseAllowedDays("MONDAY;FUNDAY");
    }
}
