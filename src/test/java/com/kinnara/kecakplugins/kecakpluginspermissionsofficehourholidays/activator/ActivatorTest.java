package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.activator;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.dayofweek.DayOfWeekAndHolidayPermission;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.workhours.WorkHourPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Activator")
class ActivatorTest {

    @Test
    @DisplayName("start registers both permissions and stop unregisters them")
    void startAndStop_WhenLifecycleRuns_RegistersAndUnregistersPlugin() {
        // Arrange
        Activator activator = new Activator();
        BundleContext context = mock(BundleContext.class);
        ServiceRegistration workHourRegistration = mock(ServiceRegistration.class);
        ServiceRegistration dayOfWeekPermissionRegistration = mock(ServiceRegistration.class);
        when(context.registerService(
                eq(WorkHourPermission.class.getName()),
                any(WorkHourPermission.class),
                isNull())).thenReturn(workHourRegistration);
        when(context.registerService(
                eq(DayOfWeekAndHolidayPermission.class.getName()),
                any(DayOfWeekAndHolidayPermission.class),
                isNull())).thenReturn(dayOfWeekPermissionRegistration);

        // Act
        activator.start(context);

        // Assert
        ArgumentCaptor<WorkHourPermission> pluginCaptor = ArgumentCaptor.forClass(WorkHourPermission.class);
        verify(context).registerService(
                eq(WorkHourPermission.class.getName()),
                pluginCaptor.capture(),
                isNull());
        assertThat(pluginCaptor.getValue()).isInstanceOf(WorkHourPermission.class);
        assertThat(pluginCaptor.getValue().getClassName())
                .isEqualTo(WorkHourPermission.class.getName());
        ArgumentCaptor<DayOfWeekAndHolidayPermission> dayOfWeekPermissionCaptor =
                ArgumentCaptor.forClass(DayOfWeekAndHolidayPermission.class);
        verify(context).registerService(
                eq(DayOfWeekAndHolidayPermission.class.getName()),
                dayOfWeekPermissionCaptor.capture(),
                isNull());
        assertThat(dayOfWeekPermissionCaptor.getValue())
                .isInstanceOf(DayOfWeekAndHolidayPermission.class);
        assertThat(dayOfWeekPermissionCaptor.getValue().getClassName())
                .isEqualTo(DayOfWeekAndHolidayPermission.class.getName());
        assertThat(activator.registrationList).hasSize(2);

        // Act
        activator.stop(context);

        // Assert test
        verify(workHourRegistration).unregister();
        verify(dayOfWeekPermissionRegistration).unregister();
    }
}
