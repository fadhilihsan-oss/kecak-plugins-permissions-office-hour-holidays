package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays;

import org.joget.apps.app.service.AppUtil;
import org.joget.directory.model.User;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("WorkHourPermission")
class WorkHourPermissionTest {

    private MockedStatic<AppUtil> appUtil;

    @AfterEach
    void tearDown() {
        if (appUtil != null) {
            appUtil.close();
            appUtil = null;
        }
    }

    @Test
    @DisplayName("isAuthorize when property is null falls back to property string")
    void isAuthorize_WhenPropertyIsNull_UsesPropertyString() {
        // Arrange
        WorkHourPermission permission = spy(new WorkHourPermission());
        Mockito.doReturn(null).when(permission).getProperty("workIntervals");
        Mockito.doReturn("[]").when(permission).getPropertyString("workIntervals");
        appUtil = Mockito.mockStatic(AppUtil.class);
        appUtil.when(AppUtil::getApplicationContext).thenThrow(new IllegalStateException("no context"));

        // Act
        boolean result = permission.isAuthorize();

        // Assert
        assertThat(result).isFalse();
        verify(permission).getProperty("workIntervals");
        verify(permission).getPropertyString("workIntervals");
    }

    @Test
    @DisplayName("isAuthorize when property exists uses it without property string fallback")
    void isAuthorize_WhenPropertyExists_DoesNotUsePropertyString() {
        // Arrange
        WorkHourPermission permission = spy(new WorkHourPermission());
        Mockito.doReturn("[]").when(permission).getProperty("workIntervals");
        appUtil = Mockito.mockStatic(AppUtil.class);
        appUtil.when(AppUtil::getApplicationContext).thenThrow(new IllegalStateException("no context"));

        // Act
        boolean result = permission.isAuthorize();

        // Assert
        assertThat(result).isFalse();
        verify(permission).getProperty("workIntervals");
        verify(permission, Mockito.never()).getPropertyString("workIntervals");
    }

    @ParameterizedTest
    @CsvSource({"null, 7", "empty, 7", "blank, 7", "+8, 8", "-5, -5", "5.5, 5.5", "Asia/Jakarta, 7"})
    @DisplayName("parseUserTimeZone accepts supported timezone representations")
    void parseUserTimeZone_WhenSupported_ReturnsExpectedZone(String input, String expectedOffset) {
        // Arrange
        String value = input.equals("null") ? null : input;
        if (input.equals("empty")) value = "";
        if (input.equals("blank")) value = "   ";

        // Act
        ZoneId actual = WorkHourPermission.parseUserTimeZone(value);

        // Assert
        if (input.equals("Asia/Jakarta")) {
            assertThat(actual).isEqualTo(ZoneId.of("Asia/Jakarta"));
        } else {
            if (input.equals("null") || input.equals("empty") || input.equals("blank")) {
                assertThat(actual).isEqualTo(ZoneId.of("Asia/Jakarta"));
            } else {
                double hours = Double.parseDouble(expectedOffset);
                assertThat(actual).isEqualTo(ZoneOffset.ofTotalSeconds((int) Math.round(hours * 3600)));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Not/AZone", "GMT+99:99"})
    @DisplayName("parseUserTimeZone falls back for invalid zone IDs")
    void parseUserTimeZone_WhenInvalid_FallsBackToJakarta(String input) {
        // Arrange
        // no setup needed

        // Act
        ZoneId actual = WorkHourPermission.parseUserTimeZone(input);

        // Assert
        assertThat(actual).isEqualTo(ZoneId.of("Asia/Jakarta"));
    }

    @Test
    @DisplayName("resolveCurrentUserZoneId falls back when application context throws")
    void isAuthorized_WhenContextThrows_FallsBackToJakarta() {
        // Arrange
        appUtil = Mockito.mockStatic(AppUtil.class);
        appUtil.when(AppUtil::getApplicationContext).thenThrow(new RuntimeException("boom"));

        // Act
        boolean result = WorkHourPermission.isAuthorized(Collections.emptyList());

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("resolveCurrentUserZoneId returns fallback when bean is missing")
    void isAuthorized_WhenUserManagerMissing_FallsBackToJakarta() {
        // Arrange
        appUtil = Mockito.mockStatic(AppUtil.class);
        ApplicationContext context = mock(ApplicationContext.class);
        appUtil.when(AppUtil::getApplicationContext).thenReturn(context);
        Mockito.when(context.getBean("workflowUserManager")).thenReturn(null);

        // Act
        boolean result = WorkHourPermission.isAuthorized(Collections.emptyList());

        // Assert
        assertThat(result).isFalse();
        verify(context).getBean("workflowUserManager");
        verifyNoMoreInteractions(context);
    }

    @Test
    @DisplayName("resolveCurrentUserZoneId returns fallback when current user is missing")
    void isAuthorized_WhenCurrentUserMissing_FallsBackToJakarta() {
        // Arrange
        appUtil = Mockito.mockStatic(AppUtil.class);
        ApplicationContext context = mock(ApplicationContext.class);
        WorkflowUserManager manager = mock(WorkflowUserManager.class);
        appUtil.when(AppUtil::getApplicationContext).thenReturn(context);
        Mockito.when(context.getBean("workflowUserManager")).thenReturn(manager);
        Mockito.when(manager.getCurrentUser()).thenReturn(null);

        // Act
        boolean result = WorkHourPermission.isAuthorized(Collections.emptyList());

        // Assert
        assertThat(result).isFalse();
        verify(context).getBean("workflowUserManager");
        verify(manager).getCurrentUser();
    }

    @Test
    @DisplayName("resolveCurrentUserZoneId returns fallback when timezone is missing")
    void isAuthorized_WhenTimezoneMissing_FallsBackToJakarta() {
        // Arrange
        appUtil = Mockito.mockStatic(AppUtil.class);
        ApplicationContext context = mock(ApplicationContext.class);
        WorkflowUserManager manager = mock(WorkflowUserManager.class);
        User user = mock(User.class);
        appUtil.when(AppUtil::getApplicationContext).thenReturn(context);
        Mockito.when(context.getBean("workflowUserManager")).thenReturn(manager);
        Mockito.when(manager.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getTimeZone()).thenReturn(null);

        // Act
        boolean result = WorkHourPermission.isAuthorized(Collections.emptyList());

        // Assert
        assertThat(result).isFalse();
        verify(user).getTimeZone();
    }

    @Test
    @DisplayName("resolveCurrentUserZoneId uses numeric timezone of current user")
    void isAuthorized_WhenCurrentUserHasNumericTimezone_ReturnsAuthorizedInsideInterval() {
        // Arrange
        appUtil = Mockito.mockStatic(AppUtil.class);
        ApplicationContext context = mock(ApplicationContext.class);
        WorkflowUserManager manager = mock(WorkflowUserManager.class);
        User user = mock(User.class);
        appUtil.when(AppUtil::getApplicationContext).thenReturn(context);
        Mockito.when(context.getBean("workflowUserManager")).thenReturn(manager);
        Mockito.when(manager.getCurrentUser()).thenReturn(user);
        Mockito.when(user.getTimeZone()).thenReturn("0");
        ZoneId utc = ZoneOffset.UTC;
        LocalTime now = LocalTime.now(utc);
        String start = now.minusHours(1).toString().substring(0, 5);
        String finish = now.plusHours(1).toString().substring(0, 5);

        // Act
        boolean result = WorkHourPermission.isAuthorized(List.of(Map.of("startTime", start, "finishTime", finish)));

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAuthorized uses default zone and denies null and empty interval values")
    void isAuthorized_WhenValueNullOrEmpty_ReturnsFalse() {
        // Arrange
        // no setup

        // Act
        boolean nullResult = WorkHourPermission.isAuthorized(null, null);
        boolean emptyResult = WorkHourPermission.isAuthorized("  ", ZoneOffset.UTC);
        boolean emptyJsonResult = WorkHourPermission.isAuthorized("[]", ZoneOffset.UTC);

        // Assert
        assertThat(nullResult).isFalse();
        assertThat(emptyResult).isFalse();
        assertThat(emptyJsonResult).isFalse();
    }

    @Test
    @DisplayName("isAuthorized accepts Object array intervals")
    void isAuthorized_WhenObjectArrayProvided_ReturnsTrueInsideInterval() {
        // Arrange
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        Object[] intervals = {Map.of("startTime", now.minusHours(1).toString().substring(0, 5),
                "finishTime", now.plusHours(1).toString().substring(0, 5))};

        // Act
        boolean result = WorkHourPermission.isAuthorized(intervals, ZoneOffset.UTC);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAuthorized accepts JSONArray intervals")
    void isAuthorized_WhenJSONArrayProvided_ReturnsTrueInsideInterval() throws Exception {
        // Arrange
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        JSONArray intervals = new JSONArray()
                .put(new JSONObject().put("startTime", now.minusHours(1).toString().substring(0, 5))
                        .put("finishTime", now.plusHours(1).toString().substring(0, 5)));

        // Act
        boolean result = WorkHourPermission.isAuthorized(intervals, ZoneOffset.UTC);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAuthorized accepts JSONObject string intervals")
    void isAuthorized_WhenJsonStringProvided_ReturnsTrueInsideInterval() {
        // Arrange
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        String intervals = "[{\"startTime\":\"" + now.minusHours(1).toString().substring(0, 5)
                + "\",\"finishTime\":\"" + now.plusHours(1).toString().substring(0, 5) + "\"}]";

        // Act
        boolean result = WorkHourPermission.isAuthorized(intervals, ZoneOffset.UTC);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAuthorized returns false outside a standard interval")
    void isAuthorized_WhenOutsideStandardInterval_ReturnsFalse() {
        // Arrange
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        LocalTime start = now.plusHours(2);
        LocalTime finish = now.plusHours(4);

        // Act
        boolean result = WorkHourPermission.isAuthorized(List.of(Map.of(
                "startTime", start.toString().substring(0, 5),
                "finishTime", finish.toString().substring(0, 5))), ZoneOffset.UTC);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAuthorized catches malformed intervals and returns false")
    void isAuthorized_WhenMalformedInterval_ReturnsFalse() {
        // Arrange
        Map<String, Object> missingFinish = new HashMap<>();
        missingFinish.put("startTime", "09:00");

        // Act
        boolean result = WorkHourPermission.isAuthorized(List.of(missingFinish), ZoneOffset.UTC);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAuthorized returns false for an invalid JSON string")
    void isAuthorized_WhenInvalidJson_ReturnsFalse() {
        // Arrange
        String invalidJson = "[not-json]";

        // Act
        boolean result = WorkHourPermission.isAuthorized(invalidJson, ZoneOffset.UTC);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("parseTime rejects null with exact message")
    void parseTime_WhenNull_ThrowsExactException() {
        // Arrange
        Method method = privateMethod("parseTime", String.class);

        // Act + Assert
        assertThatThrownBy(() -> invoke(method, new Object[]{null}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interval time cannot be null.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "9:00", "24:00", "12:60", "aa:bb", "09:00:00"})
    @DisplayName("parseTime rejects malformed values with exact message")
    void parseTime_WhenMalformed_ThrowsExactException(String value) {
        // Arrange
        Method method = privateMethod("parseTime", String.class);

        // Act + Assert
        assertThatThrownBy(() -> invoke(method, value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interval times must use HH:mm format, got: " + value.trim());
    }

    @ParameterizedTest
    @CsvSource({"00:00,00:00", "09:30,17:45", "23:59,00:00"})
    @DisplayName("parseTime accepts valid HH:mm values")
    void parseTime_WhenValid_ReturnsLocalTime(String input, String ignored) {
        // Arrange
        Method method = privateMethod("parseTime", String.class);

        // Act
        LocalTime result = (LocalTime) invoke(method, input);

        // Assert
        assertThat(result).isEqualTo(LocalTime.parse(input));
    }

    @Test
    @DisplayName("parseIntervalObject rejects map missing start or finish")
    void parseIntervalObject_WhenMapMissingRequiredField_ThrowsExactException() {
        // Arrange
        Method method = privateMethod("parseIntervalObject", Object.class);

        // Act + Assert
        assertThatThrownBy(() -> invoke(method, Map.of("finishTime", "10:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each interval needs startTime and finishTime.");
        assertThatThrownBy(() -> invoke(method, Map.of("startTime", "09:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each interval needs startTime and finishTime.");
    }

    @Test
    @DisplayName("parseIntervalObject rejects unsupported row types")
    void parseIntervalObject_WhenUnsupportedType_ThrowsExactException() {
        // Arrange
        Method method = privateMethod("parseIntervalObject", Object.class);

        // Act + Assert
        assertThatThrownBy(() -> invoke(method, "invalid-row"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid work interval row type: java.lang.String");
    }

    @Test
    @DisplayName("parseIntervalObject accepts JSONObject and Map rows")
    void parseIntervalObject_WhenSupportedType_ReturnsInterval() throws Exception {
        // Arrange
        Method method = privateMethod("parseIntervalObject", Object.class);

        // Act
        Object fromMap = invoke(method, Map.of("startTime", "09:00", "finishTime", "17:00"));
        Object fromJson = invoke(method, new JSONObject().put("startTime", "09:00").put("finishTime", "17:00"));

        // Assert
        assertThat(fromMap).isNotNull();
        assertThat(fromJson).isNotNull();
    }

    @Test
    @DisplayName("parseIntervalJson rejects missing required fields")
    void parseIntervalJson_WhenFieldMissing_ThrowsExactException() throws Exception {
        // Arrange
        Method method = privateMethod("parseIntervalJson", JSONObject.class);

        // Act + Assert
        assertThatThrownBy(() -> invoke(method, new JSONObject().put("finishTime", "17:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each interval JSON object needs startTime and finishTime.");
        assertThatThrownBy(() -> invoke(method, new JSONObject().put("startTime", "09:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each interval JSON object needs startTime and finishTime.");
    }

    @Test
    @DisplayName("WorkInterval contains covers equal, standard, and cross-midnight ranges")
    void workInterval_WhenRangesEvaluated_ReturnsExpectedMembership() throws Exception {
        // Arrange
        Class<?> workIntervalClass = Class.forName(WorkHourPermission.class.getName() + "$WorkInterval");
        java.lang.reflect.Constructor<?> constructor = workIntervalClass.getDeclaredConstructor(LocalTime.class, LocalTime.class);
        constructor.setAccessible(true);
        Method contains = workIntervalClass.getDeclaredMethod("contains", LocalTime.class);
        contains.setAccessible(true);

        Object equal = constructor.newInstance(LocalTime.of(9, 0), LocalTime.of(9, 0));
        Object standard = constructor.newInstance(LocalTime.of(9, 0), LocalTime.of(17, 0));
        Object midnight = constructor.newInstance(LocalTime.of(22, 0), LocalTime.of(6, 0));

        // Act + Assert
        assertThat((Boolean) contains.invoke(equal, LocalTime.of(9, 30))).isFalse();
        assertThat((Boolean) contains.invoke(standard, LocalTime.of(9, 0))).isTrue();
        assertThat((Boolean) contains.invoke(standard, LocalTime.of(16, 59))).isTrue();
        assertThat((Boolean) contains.invoke(standard, LocalTime.of(17, 0))).isFalse();
        assertThat((Boolean) contains.invoke(standard, LocalTime.of(8, 59))).isFalse();
        assertThat((Boolean) contains.invoke(midnight, LocalTime.of(23, 0))).isTrue();
        assertThat((Boolean) contains.invoke(midnight, LocalTime.of(5, 59))).isTrue();
        assertThat((Boolean) contains.invoke(midnight, LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("getVersion returns manifest implementation version when packaged")
    void getVersion_WhenImplementationVersionIsPresent_ReturnsManifestVersion() throws Exception {
        // Arrange
        File jarFile = File.createTempFile("work-hour-plugin-", ".jar");
        jarFile.deleteOnExit();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "9.9.9");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            String className = WorkHourPermission.class.getName().replace('.', '/') + ".class";
            jar.putNextEntry(new JarEntry(className));
            jar.write(Files.readAllBytes(new File("target/classes/" + className).toPath()));
            jar.closeEntry();
        }

        URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()},
                WorkHourPermissionTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(WorkHourPermission.class.getName())) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) loaded = findClass(name);
                        if (resolve) resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };

        // Act
        Object permission = loader.loadClass(WorkHourPermission.class.getName()).getDeclaredConstructor().newInstance();
        Method getVersion = permission.getClass().getMethod("getVersion");
        Object version = getVersion.invoke(permission);

        // Assert
        assertThat(version).isEqualTo("9.9.9");
        loader.close();
    }

    @Test
    @DisplayName("plugin metadata methods return the expected values")
    void metadataMethods_WhenCalled_ReturnExpectedValues() {
        // Arrange
        WorkHourPermission permission = new WorkHourPermission();

        // Act + Assert
        assertThat(permission.getName()).isEqualTo("Work Hour Permission");
        assertThat(permission.getDescription()).isEqualTo("Restricts userview, form, and datalist access only during selected work hours.");
        assertThat(permission.getLabel()).isEqualTo("Work Hour Permission");
        assertThat(permission.getClassName()).isEqualTo(WorkHourPermission.class.getName());
        assertThat(permission.getVersion()).isNotBlank();
    }

    @Test
    @DisplayName("getPropertyOptions reads the plugin resource")
    void getPropertyOptions_WhenCalled_ReadsWorkHourJsonResource() {
        // Arrange
        WorkHourPermission permission = new WorkHourPermission();
        appUtil = Mockito.mockStatic(AppUtil.class);
        appUtil.when(() -> AppUtil.readPluginResource(
                WorkHourPermission.class.getName(),
                "/properties/WorkHourPermission.json",
                null,
                true,
                null)).thenReturn("[]");

        // Act
        String result = permission.getPropertyOptions();

        // Assert
        assertThat(result).isEqualTo("[]");
        appUtil.verify(() -> AppUtil.readPluginResource(
                WorkHourPermission.class.getName(),
                "/properties/WorkHourPermission.json",
                null,
                true,
                null));
    }

    private Method privateMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = WorkHourPermission.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private Object invoke(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new AssertionError(cause);
        }
    }
}
