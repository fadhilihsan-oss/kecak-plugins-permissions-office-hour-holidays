package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.service;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.client.HolidayApiClient;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model.NationalHoliday;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.List;

public class HolidayService {
    private final HolidayRepository repository;
    private final HolidayApiClient apiClient;

    public HolidayService(HolidayRepository repository) {
        this(repository, new HolidayApiClient());
    }

    HolidayService(HolidayRepository repository, HolidayApiClient apiClient) {
        this.repository = repository;
        this.apiClient = apiClient;
    }

    public void synchronizeYearIfNeeded(int year) {
        synchronized (HolidayService.class) {
            if (repository.hasApiSyncedYear(year)) {
                return;
            }

            List<NationalHoliday> holidays = apiClient.fetchNationalHolidays(year);
            repository.replaceApiHolidaysForYear(year, holidays);
        }
    }

    public boolean isNationalHoliday(LocalDate date) {
        return repository.containsNationalHoliday(date);
    }

    public boolean isHoliday(LocalDate date, String tableName, String dateColumn) {
        return repository.containsHoliday(date, tableName, dateColumn);
    }

    public Boolean getHolidayStatus(LocalDate date, String tableName,
                                    String dateColumn, String statusColumn) {
        return repository.getHolidayStatus(date, tableName, dateColumn, statusColumn);
    }
}
