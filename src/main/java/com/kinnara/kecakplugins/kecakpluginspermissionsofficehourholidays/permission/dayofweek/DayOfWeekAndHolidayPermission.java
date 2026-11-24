package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.dayofweek;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.repository.HolidayRepository;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.holiday.service.HolidayService;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.validator.DayOfWeekAndHolidayValidator;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.AbstractSubForm;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormContainer;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.form.lib.DatePicker;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;

public class DayOfWeekAndHolidayPermission extends UserviewPermission implements FormPermission, PluginWebSupport {
    private static final String DEFAULT_HOLIDAY_TABLE = "app_holiday_calendar";
    private static final String DEFAULT_HOLIDAY_DATE_COLUMN = "holiday_date";

    private final DayOfWeekAndHolidayValidator validator = new DayOfWeekAndHolidayValidator();
    private FormData formData;
    private Element element;

    @Override
    public boolean isAuthorize() {
        if (WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN)) {
            return true;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ApplicationContext context = AppUtil.getApplicationContext();
        DataSource dataSource = (DataSource) context.getBean("setupDataSource");
        boolean allowNationalHoliday = getBooleanProperty("allowNationalHoliday", false);
        boolean useLegacyCustomTable = getBooleanProperty("useCustomHolidayTable", false);
        String holidayTable = DEFAULT_HOLIDAY_TABLE;
        String holidayDateColumn = DEFAULT_HOLIDAY_DATE_COLUMN;
        String holidayStatusColumn = getPropertyString("customHolidayStatusColumn");
        String holidayFormId = getPropertyString("holidayFormId");
        String selectedTableValue = getPropertyString("customHolidayTable");
        String customSourceProperty = getPropertyString("useCustomHolidaySource");
        boolean hasHolidayForm = holidayFormId != null && !holidayFormId.trim().isEmpty();
        boolean hasLegacyCustomTable = useLegacyCustomTable
                || (selectedTableValue != null && !selectedTableValue.trim().isEmpty());
        boolean useCustomHolidaySource = customSourceProperty == null
                ? hasHolidayForm || hasLegacyCustomTable
                : Boolean.parseBoolean(customSourceProperty.trim());
        if (useCustomHolidaySource && hasHolidayForm) {
            if (!holidayFormId.equals(selectedTableValue)) {
                throw new IllegalArgumentException("Select the holiday table for the chosen form");
            }
            AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
            FormDefinition formDefinition = findFormDefinition(appDefinition, holidayFormId);
            if (formDefinition == null) {
                throw new IllegalArgumentException("The selected holiday form is not available in this app");
            }
            String tableName = formDefinition.getTableName();
            if (tableName == null || tableName.trim().isEmpty()) {
                tableName = formDefinition.getId();
            }
            holidayTable = "app_fd_" + tableName;
            holidayDateColumn = getPropertyString("customHolidayDateColumn");
            if (holidayDateColumn == null || holidayDateColumn.trim().isEmpty()) {
                throw new IllegalArgumentException("Select a holiday date field");
            }
        } else if (useCustomHolidaySource && hasLegacyCustomTable) {
            holidayTable = selectedTableValue == null ? DEFAULT_HOLIDAY_TABLE : selectedTableValue;
            holidayDateColumn = getPropertyString("customHolidayDateColumn");
            if (holidayDateColumn == null || holidayDateColumn.trim().isEmpty()) {
                holidayDateColumn = DEFAULT_HOLIDAY_DATE_COLUMN;
            }
        } else if (useCustomHolidaySource) {
            throw new IllegalArgumentException("Select a holiday form for the custom holiday source");
        }

        HolidayService holidayService = new HolidayService(new HolidayRepository(dataSource));
        boolean usesInternalCalendar = DEFAULT_HOLIDAY_TABLE.equals(holidayTable)
                && DEFAULT_HOLIDAY_DATE_COLUMN.equals(holidayDateColumn);
        if (usesInternalCalendar) {
            holidayService.synchronizeYearIfNeeded(today.getYear());
        }
        Set<DayOfWeek> allowedDays = getAllowedDays();
        Boolean holidayStatusActive = null;
        boolean nationalHoliday;
        boolean allowed;
        if (usesInternalCalendar) {
            nationalHoliday = holidayService.isNationalHoliday(today);
            allowed = validator.isAllowed(today, allowedDays, nationalHoliday, allowNationalHoliday);
        } else if (holidayStatusColumn != null && !holidayStatusColumn.trim().isEmpty()) {
            holidayStatusActive = holidayService.getHolidayStatus(
                    today, holidayTable, holidayDateColumn, holidayStatusColumn.trim());
            nationalHoliday = Boolean.TRUE.equals(holidayStatusActive);
            allowed = validator.isAllowedWithHolidayStatus(
                    today, allowedDays, holidayStatusActive, allowNationalHoliday);
        } else {
            nationalHoliday = holidayService.isHoliday(today, holidayTable, holidayDateColumn);
            allowed = validator.isAllowed(today, allowedDays, nationalHoliday, allowNationalHoliday);
        }
        LogUtil.debug(getClassName(), "Date access check: date=" + today
                + ", nationalHoliday=" + nationalHoliday
                + ", holidayStatusActive=" + holidayStatusActive
                + ", allowNationalHoliday=" + allowNationalHoliday
                + ", allowedDays=" + allowedDays
                + ", allowed=" + allowed);

        if (!allowed && getElement() instanceof Form) {
            String blockedMessage = getPropertyString("blockedMessage");
            if (!blockedMessage.trim().isEmpty()) {
                Element form = getElement();
                form.setProperty("noPermissionMessage", blockedMessage);
            }
        }
        return allowed;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String action = request.getParameter("action");
        ApplicationContext context = AppUtil.getApplicationContext();
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        if (appDefinition == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "An app context is required");
            return;
        }
        JSONArray options = new JSONArray();
        Map<String, String> emptyOption = new HashMap<>();
        emptyOption.put("value", "");
        emptyOption.put("label", "");
        options.put(emptyOption);

        try {
            if ("getHolidayForms".equals(action)) {
                for (FormDefinition formDefinition : getFormDefinitions(appDefinition)) {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", formDefinition.getId());
                    option.put("label", formDefinition.getName() + " (" + formDefinition.getId() + ")");
                    options.put(option);
                }
            } else if ("getHolidayTables".equals(action)) {
                String formId = request.getParameter("holidayFormId");
                FormDefinition formDefinition = findFormDefinition(appDefinition, formId);
                if (formDefinition != null) {
                    String tableName = formDefinition.getTableName();
                    if (tableName == null || tableName.trim().isEmpty()) {
                        tableName = formDefinition.getId();
                    }
                    String physicalTableName = "app_fd_" + tableName;
                    HolidayRepository.validateIdentifier(physicalTableName, "holiday table");
                    Map<String, String> option = new HashMap<>();
                    option.put("value", formDefinition.getId());
                    option.put("label", physicalTableName);
                    options.put(option);
                }
            } else if ("getHolidayDateColumns".equals(action)) {
                String formId = request.getParameter("customHolidayTable");
                FormDefinition formDefinition = findFormDefinition(appDefinition, formId);
                if (formDefinition != null) {
                    if (formDefinition.getJson() == null || formDefinition.getJson().trim().isEmpty()) {
                        throw new IllegalArgumentException("The selected form has no saved definition");
                    }
                    FormService formService = (FormService) context.getBean("formService");
                    Form form = (Form) formService.createElementFromJson(formDefinition.getJson(), false);
                    addDatePickerOptions(form, options);
                }
            } else if ("getHolidayStatusColumns".equals(action)) {
                String formId = request.getParameter("customHolidayTable");
                FormDefinition formDefinition = findFormDefinition(appDefinition, formId);
                if (formDefinition != null) {
                    if (formDefinition.getJson() == null || formDefinition.getJson().trim().isEmpty()) {
                        throw new IllegalArgumentException("The selected form has no saved definition");
                    }
                    FormService formService = (FormService) context.getBean("formService");
                    Form form = (Form) formService.createElementFromJson(formDefinition.getJson(), false);
                    addStatusFieldOptions(form, options);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            options.write(response.getWriter());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (JSONException e) {
            LogUtil.error(getClassName(), e, "Unable to serialize holiday table options");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private List<FormDefinition> getFormDefinitions(AppDefinition appDefinition) {
        if (appDefinition == null || appDefinition.getFormDefinitionList() == null) {
            return java.util.Collections.emptyList();
        }
        return new java.util.ArrayList<>(appDefinition.getFormDefinitionList());
    }

    private FormDefinition findFormDefinition(AppDefinition appDefinition, String formId) {
        if (formId == null || formId.trim().isEmpty()) {
            return null;
        }
        for (FormDefinition formDefinition : getFormDefinitions(appDefinition)) {
            if (formId.equals(formDefinition.getId())) {
                return formDefinition;
            }
        }
        return null;
    }

    private void addDatePickerOptions(Element element, JSONArray options) throws JSONException {
        if (element instanceof DatePicker) {
            String fieldId = element.getPropertyString("id");
            if (fieldId != null && !fieldId.trim().isEmpty()) {
                String columnName = "c_" + fieldId;
                HolidayRepository.validateIdentifier(columnName, "holiday date column");
                Map<String, String> option = new HashMap<>();
                option.put("value", columnName);
                option.put("label", element.getPropertyString("label") + " (" + fieldId + ")");
                options.put(option);
            }
        }

        if (!(element instanceof AbstractSubForm) && element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                addDatePickerOptions(child, options);
            }
        }
    }

    private void addStatusFieldOptions(Element element, JSONArray options) throws JSONException {
        if (!(element instanceof Form)
                && !(element instanceof FormContainer)
                && !(element instanceof AbstractSubForm)
                && !(element instanceof DatePicker)) {
            String fieldId = element.getPropertyString("id");
            if (fieldId != null && !fieldId.trim().isEmpty()) {
                String columnName = "c_" + fieldId;
                HolidayRepository.validateIdentifier(columnName, "holiday status column");
                Map<String, String> option = new HashMap<>();
                option.put("value", columnName);
                option.put("label", element.getPropertyString("label") + " (" + fieldId + ")");
                options.put(option);
            }
        }

        if (!(element instanceof AbstractSubForm) && element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                addStatusFieldOptions(child, options);
            }
        }
    }

    private Set<DayOfWeek> getAllowedDays() {
        String configuredDays = getPropertyString("allowedDays");
        if (configuredDays != null) {
            return validator.parseAllowedDays(configuredDays);
        }

        String[] legacyProperties = {
                "allowMonday", "allowTuesday", "allowWednesday", "allowThursday",
                "allowFriday", "allowSaturday", "allowSunday"
        };
        boolean hasLegacyConfiguration = false;
        for (String property : legacyProperties) {
            hasLegacyConfiguration |= getPropertyString(property) != null;
        }
        if (!hasLegacyConfiguration) {
            return EnumSet.allOf(DayOfWeek.class);
        }

        Set<DayOfWeek> allowedDays = EnumSet.noneOf(DayOfWeek.class);
        DayOfWeek[] days = DayOfWeek.values();
        for (int i = 0; i < days.length; i++) {
            if (getBooleanProperty(legacyProperties[i], true)) {
                allowedDays.add(days[i]);
            }
        }
        return allowedDays;
    }

    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = getPropertyString(name);
        return value == null
                ? defaultValue
                : Boolean.parseBoolean(value.trim());
    }

    @Override
    public FormData getFormData() {
        return formData;
    }

    @Override
    public void setFormData(FormData formData) {
        this.formData = formData;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void setElement(Element element) {
        this.element = element;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("dayOfWeekHoliday.title", getClassName(),
                "/messages/DayOfWeekAndHolidayPermission");
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        Object[] arguments = new Object[]{
                getClassName(), getClassName(), getClassName(), getClassName()
        };
        return AppUtil.readPluginResource(getClassName(),
                "/properties/DayOfWeekAndHolidayPermission.json", arguments, false,
                "/messages/DayOfWeekAndHolidayPermission");
    }
}
