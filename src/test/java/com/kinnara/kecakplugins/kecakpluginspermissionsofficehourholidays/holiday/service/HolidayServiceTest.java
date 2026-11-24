package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.service;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.client.HolidayApiClient;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model.NationalHoliday;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.repository.HolidayRepository;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HolidayServiceTest {
    @Test
    public void synchronizesOnlyWhenYearWasNotSynced() {
        RecordingRepository repository = new RecordingRepository();
        RecordingApiClient apiClient = new RecordingApiClient();
        HolidayService service = new HolidayService(repository, apiClient);

        service.synchronizeYearIfNeeded(2026);

        assertTrue(apiClient.fetchCalled);
        assertEquals(2026, repository.replacedYear);
        assertEquals(1, repository.replacedHolidays.size());
    }

    @Test
    public void skipsSynchronizationForAlreadySyncedYear() {
        RecordingRepository repository = new RecordingRepository();
        repository.synced = true;
        RecordingApiClient apiClient = new RecordingApiClient();
        HolidayService service = new HolidayService(repository, apiClient);

        service.synchronizeYearIfNeeded(2026);

        assertFalse(apiClient.fetchCalled);
        assertEquals(0, repository.replacedYear);
    }

    @Test
    public void delegatesHolidayQueriesToRepository() {
        RecordingRepository repository = new RecordingRepository();
        HolidayService service = new HolidayService(repository, new RecordingApiClient());
        LocalDate date = LocalDate.of(2026, 1, 1);

        assertTrue(service.isNationalHoliday(date));
        assertTrue(service.isHoliday(date, "app_holiday", "holiday_date"));
        assertEquals(Boolean.TRUE, service.getHolidayStatus(
                date, "app_holiday", "holiday_date", "active"));
    }

    private DataSource emptyDataSource() {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(),
                new Class[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("unwrap".equals(method.getName())) {
                        return null;
                    }
                    if ("isWrapperFor".equals(method.getName())) {
                        return false;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private class RecordingRepository extends HolidayRepository {
        private boolean synced;
        private int replacedYear;
        private java.util.List<NationalHoliday> replacedHolidays =
                java.util.Collections.emptyList();

        RecordingRepository() {
            super(emptyDataSource());
        }

        @Override
        public boolean hasApiSyncedYear(int year) {
            return synced;
        }

        @Override
        public void replaceApiHolidaysForYear(int year, java.util.List<NationalHoliday> holidays) {
            replacedYear = year;
            replacedHolidays = holidays;
        }

        @Override
        public boolean containsNationalHoliday(LocalDate date) {
            return true;
        }

        @Override
        public boolean containsHoliday(LocalDate date, String tableName, String dateColumn) {
            return true;
        }

        @Override
        public Boolean getHolidayStatus(LocalDate date, String tableName,
                                        String dateColumn, String statusColumn) {
            return Boolean.TRUE;
        }
    }

    private class RecordingApiClient extends HolidayApiClient {
        private boolean fetchCalled;

        @Override
        public java.util.List<NationalHoliday> fetchNationalHolidays(int year) {
            fetchCalled = true;
            return Arrays.asList(new NationalHoliday(
                    LocalDate.of(year, 1, 1), "New Year"));
        }
    }
}
