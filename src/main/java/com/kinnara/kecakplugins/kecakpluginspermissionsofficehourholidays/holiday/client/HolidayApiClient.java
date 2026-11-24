package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.client;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.model.NationalHoliday;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class HolidayApiClient {
    public static final String SOURCE = "APIHariLibur_V2";
    private static final String API_URL =
            "https://raw.githubusercontent.com/guangrei/APIHariLibur_V2/main/holidays.json";

    public List<NationalHoliday> fetchNationalHolidays(int year) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Kecak-DayOfWeekAndHolidayPermission/1.1.0");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Holiday API returned HTTP " + status);
            }

            try (InputStream input = connection.getInputStream()) {
                return parseNationalHolidays(readResponse(input), year);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to retrieve Indonesian national holidays", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    List<NationalHoliday> parseNationalHolidays(String json, int year) {
        JSONObject calendar;
        try {
            calendar = new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalStateException("Holiday API returned invalid JSON", e);
        }

        List<NationalHoliday> holidays = new ArrayList<>();
        Iterator<?> keys = calendar.keys();
        while (keys.hasNext()) {
            String dateKey = String.valueOf(keys.next());
            if (!dateKey.startsWith(year + "-")) {
                continue;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateKey);
            } catch (DateTimeParseException e) {
                throw new IllegalStateException("Holiday API returned an invalid date: " + dateKey, e);
            }

            JSONObject item = calendar.optJSONObject(dateKey);
            String name = item == null ? "" : item.optString("summary", "").trim();
            if (name.isEmpty()) {
                throw new IllegalStateException("Holiday API returned no name for " + dateKey);
            }

            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (normalizedName.contains("cuti bersama")
                    || normalizedName.contains("joint holiday")
                    || normalizedName.contains("belum pasti")) {
                continue;
            }

            holidays.add(new NationalHoliday(date, name));
        }

        if (holidays.isEmpty()) {
            throw new IllegalStateException("Holiday API returned no confirmed national holidays for " + year);
        }
        return holidays;
    }

    private String readResponse(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            total += bytesRead;
            if (total > 1024 * 1024) {
                throw new IOException("Holiday API response exceeded 1 MB");
            }
            output.write(buffer, 0, bytesRead);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
