/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.helper.datahelper;

import java.util.Calendar;
import java.util.Date;

public class DataHelperToDateTimeWithCalnTest extends DataHelperTestCases {
    public DataHelperToDateTimeWithCalnTest(String name) {
        super(name);
    }

    public void testToDateTimeWithFullSetting() {
        Calendar controlCalendar = Calendar.getInstance();
        controlCalendar.clear();
        controlCalendar.set(Calendar.YEAR, 2001);
        controlCalendar.set(Calendar.MONTH, 3);
        controlCalendar.set(Calendar.DATE, 1);
        controlCalendar.set(Calendar.HOUR, 12);
        controlCalendar.set(Calendar.MINUTE, 23);
        controlCalendar.set(Calendar.SECOND, 11);
        controlCalendar.set(Calendar.MILLISECOND, 1);
        String tm = dataHelper.toDateTime(controlCalendar);
        this.assertEquals("2001-04-01T12:23:11.001", tm);
    }

    public void testToDateTimeWithDefault() {
        Calendar controlCalendar = Calendar.getInstance();
        controlCalendar.clear();
        String tm = dataHelper.toDateTime(controlCalendar);
        this.assertEquals("1970-01-01T00:00:00.0", tm);
    }

    public void testToDateTimeWithNullInput() {
        Calendar controlCalendar = null;
        String tm = dataHelper.toDateTime(controlCalendar);
        this.assertNull(tm);
    }
}