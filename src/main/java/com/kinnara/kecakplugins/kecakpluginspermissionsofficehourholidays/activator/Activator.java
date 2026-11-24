package com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.activator;

import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.dayofweek.DayOfWeekAndHolidayPermission;
import com.kinnara.kecakplugins.kecakpluginspermissionsofficehourholidays.permission.workhours.WorkHourPermission;
import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration<?>> registrationList;

    @Override
    public void start(BundleContext context) {
        registrationList = new ArrayList<>();

        // Register each permission plugin as an OSGi service.
        registrationList.add(context.registerService(
                WorkHourPermission.class.getName(), new WorkHourPermission(), null));
        registrationList.add(context.registerService(
                DayOfWeekAndHolidayPermission.class.getName(),
                new DayOfWeekAndHolidayPermission(), null));
    }

    @Override
    public void stop(BundleContext context) {
        for (ServiceRegistration<?> registration : registrationList) {
            registration.unregister();
        }
    }
}