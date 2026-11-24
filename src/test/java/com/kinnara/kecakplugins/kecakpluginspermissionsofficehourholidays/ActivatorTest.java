package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.mockito.ArgumentCaptor;

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
    @DisplayName("start registers WorkHourPermission and stop unregisters it")
    void startAndStop_WhenLifecycleRuns_RegistersAndUnregistersPlugin() {
        // Arrange
        Activator activator = new Activator();
        BundleContext context = mock(BundleContext.class);
        ServiceRegistration registration = mock(ServiceRegistration.class);
        when(context.registerService(eq(WorkHourPermission.class.getName()), any(WorkHourPermission.class), isNull())).thenReturn(registration);

        // Act
        activator.start(context);

        // Assert
        ArgumentCaptor<WorkHourPermission> pluginCaptor = ArgumentCaptor.forClass(WorkHourPermission.class);
        verify(context).registerService(
                eq(WorkHourPermission.class.getName()),
                pluginCaptor.capture(),
                isNull());
        assertThat(pluginCaptor.getValue()).isInstanceOf(WorkHourPermission.class);
        assertThat(activator.registrationList).hasSize(1);

        // Act
        activator.stop(context);

        // Assert
        verify(registration).unregister();
    }
}
