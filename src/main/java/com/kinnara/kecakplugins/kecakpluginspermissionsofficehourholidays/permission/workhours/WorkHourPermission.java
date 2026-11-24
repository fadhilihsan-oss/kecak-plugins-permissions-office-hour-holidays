package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.workhours;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DatalistPermission;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.directory.model.User;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class WorkHourPermission extends UserviewPermission
        implements FormPermission, DatalistPermission {

    private static final Logger LOGGER =
            Logger.getLogger(WorkHourPermission.class.getName());
    private static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TIME_PATTERN =
            Pattern.compile("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$");

    @Override
    public boolean isAuthorize() {
        Object rawIntervals = getProperty("workIntervals");
        if (rawIntervals == null) {
            rawIntervals = getPropertyString("workIntervals");
        }
        ZoneId userZoneId = resolveCurrentUserZoneId();
        return isAuthorized(rawIntervals, userZoneId);
    }

    private static ZoneId resolveCurrentUserZoneId() {
        try {
            WorkflowUserManager userManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
            if (userManager != null) {
                User currentUser = userManager.getCurrentUser();
                if (currentUser != null && currentUser.getTimeZone() != null) {
                    return parseUserTimeZone(currentUser.getTimeZone());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Unable to retrieve the current user's time zone; falling back to " + DEFAULT_TIME_ZONE, e);
        }
        return DEFAULT_TIME_ZONE;
    }

    static ZoneId parseUserTimeZone(String rawTimeZone) {
        if (rawTimeZone == null || rawTimeZone.trim().isEmpty()) {
            return DEFAULT_TIME_ZONE;
        }
        String tz = rawTimeZone.trim();
        try {
            // If the value is a GMT offset number from the Joget/Kecak dropdown (for example, "7", "+8", "-5", or "5.5")
            if (tz.matches("^[+-]?\\d+(?:\\.\\d+)?$")) {
                double hoursDouble = Double.parseDouble(tz);
                int totalSeconds = (int) Math.round(hoursDouble * 3600);
                return ZoneOffset.ofTotalSeconds(totalSeconds);
            }
            // If the value is a standard zone ID (for example, "Asia/Jakarta", "Asia/Makassar", or "GMT+08:00")
            return ZoneId.of(tz);
        } catch (NumberFormatException | DateTimeException e) {
            LOGGER.log(Level.WARNING,
                    "Invalid user time zone value '" + tz + "'; falling back to " + DEFAULT_TIME_ZONE, e);
            return DEFAULT_TIME_ZONE;
        }
    }

    static boolean isAuthorized(Object intervalsValue) {
        return isAuthorized(intervalsValue, resolveCurrentUserZoneId());
    }

    static boolean isAuthorized(Object intervalsValue, ZoneId zoneId) {
        try {
            ZoneId effectiveZone = (zoneId != null) ? zoneId : DEFAULT_TIME_ZONE;
            LocalTime now = LocalTime.now(effectiveZone);
            List<WorkInterval> intervals = parseIntervals(intervalsValue);
            for (WorkInterval interval : intervals) {
                if (interval.contains(now)) {
                    return true;
                }
            }
            return false;
        } catch (DateTimeParseException | NullPointerException | IllegalArgumentException | JSONException e) {
            LOGGER.log(Level.WARNING, "Unable to evaluate configured work-hour intervals: " + intervalsValue, e);
            return false;
        }
    }

    private static List<WorkInterval> parseIntervals(Object intervalsValue) throws JSONException {
        List<WorkInterval> intervals = new ArrayList<>();
        if (intervalsValue == null) {
            return intervals;
        }

        if (intervalsValue instanceof List<?>) {
            for (Object rowValue : (List<?>) intervalsValue) {
                intervals.add(parseIntervalObject(rowValue));
            }
            return intervals;
        }

        if (intervalsValue instanceof Object[]) {
            for (Object rowValue : (Object[]) intervalsValue) {
                intervals.add(parseIntervalObject(rowValue));
            }
            return intervals;
        }

        if (intervalsValue instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) intervalsValue;
            for (int i = 0; i < jsonArray.length(); i++) {
                intervals.add(parseIntervalJson(jsonArray.getJSONObject(i)));
            }
            return intervals;
        }

        String value = intervalsValue.toString().trim();
        if (value.isEmpty() || "[]".equals(value)) {
            return intervals;
        }

        JSONArray jsonArray = new JSONArray(value);
        for (int i = 0; i < jsonArray.length(); i++) {
            intervals.add(parseIntervalJson(jsonArray.getJSONObject(i)));
        }
        return intervals;
    }

    private static WorkInterval parseIntervalObject(Object rowValue) throws JSONException {
        if (rowValue instanceof Map<?, ?>) {
            Map<?, ?> row = (Map<?, ?>) rowValue;
            Object startObj = row.get("startTime");
            Object finishObj = row.get("finishTime");
            if (startObj == null || finishObj == null) {
                throw new IllegalArgumentException("Each interval must include a start time and an end time.");
            }
            return new WorkInterval(
                    parseTime(startObj.toString()),
                    parseTime(finishObj.toString())
            );
        }
        if (rowValue instanceof JSONObject) {
            return parseIntervalJson((JSONObject) rowValue);
        }
        throw new IllegalArgumentException("Invalid work interval row type: " + rowValue.getClass().getName());
    }

    private static WorkInterval parseIntervalJson(JSONObject row) throws JSONException {
        if (!row.has("startTime") || !row.has("finishTime")) {
            throw new IllegalArgumentException("Each interval JSON object must include a start time and an end time.");
        }
        return new WorkInterval(
                parseTime(row.getString("startTime")),
                parseTime(row.getString("finishTime"))
        );
    }

    private static LocalTime parseTime(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Interval time cannot be null.");
        }
        String trimmed = value.trim();
        if (!TIME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Interval times must use HH:mm format, got: " + trimmed);
        }
        return LocalTime.parse(trimmed, TIME_FORMAT);
    }

    private static final class WorkInterval {
        private final LocalTime start;
        private final LocalTime finish;

        private WorkInterval(LocalTime start, LocalTime finish) {
            this.start = start;
            this.finish = finish;
        }

        private boolean contains(LocalTime time) {
            if (start.equals(finish)) {
                return false;
            }
            if (start.isBefore(finish)) {
                return !time.isBefore(start) && time.isBefore(finish);
            }
            // Support ranges that cross midnight (for example, 22:00 - 06:00)
            return !time.isBefore(start) || time.isBefore(finish);
        }
    }

    @Override
    public String getName() {
        return "Work Hours Permission";
    }

    @Override
    public String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version != null ? version : "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Restricts access to userviews, forms, and data lists to the selected work hours.";
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(
                getClass().getName(),
                "/properties/WorkHourPermission.json",
                null,
                true,
                null
        );
    }
}