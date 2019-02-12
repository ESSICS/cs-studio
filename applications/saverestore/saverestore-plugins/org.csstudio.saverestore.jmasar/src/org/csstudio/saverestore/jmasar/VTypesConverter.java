/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.saverestore.jmasar;

import org.diirt.vtype.VType;
import org.diirt.vtype.ValueFactory;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Scalar;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VInt;

/**
 * Utility methods to convert back and forth between {@link org.diirt.vtype.VType}s
 * and {@link org.epics.vtype.VType}s.
 * 
 * 
 * @author georgweiss
 * Created 17 Jan 2019
 */
public class VTypesConverter {
	
	public static VType fromEpicsVType(org.epics.vtype.VType vType) {

		Class<?> clazz = org.epics.vtype.VType.typeOf(vType);

		if (clazz.equals(org.epics.vtype.VDouble.class)) {
			Scalar scalar = (Scalar) vType;
			Alarm alarm = scalar.getAlarm();
			Time time = scalar.getTime();

			return ValueFactory.newVDouble(((VDouble) vType).getValue(), fromEpicsAlarm(alarm), fromEpicsTime(time),
					ValueFactory.displayNone());
		} else if (clazz.equals(org.epics.vtype.VInt.class)) {
			Scalar scalar = (Scalar) vType;
			Alarm alarm = scalar.getAlarm();
			Time time = scalar.getTime();
			return ValueFactory.toVType(((VInt) vType).getValue(), fromEpicsAlarm(alarm), fromEpicsTime(time),
					ValueFactory.displayNone());
		}

		return null;
	}
	
	public static org.epics.vtype.VType toEpcisVType(VType vType){
		Class<?> clazz = vType.getClass();
		
		if(clazz.equals(org.diirt.vtype.VDouble.class)) {
			org.diirt.vtype.VDouble vDouble = (org.diirt.vtype.VDouble)vType;
			return VDouble.of(vDouble.getValue(), 
					toEpicsAlarm(vDouble.getAlarmSeverity(), vDouble.getAlarmName()), 
					Time.of(vDouble.getTimestamp()),
					Display.none());
		} else if(clazz.equals(org.diirt.vtype.VInt.class)) {
			org.diirt.vtype.VInt vInt = (org.diirt.vtype.VInt)vType;
			return VDouble.of(vInt.getValue(), 
					toEpicsAlarm(vInt.getAlarmSeverity(), vInt.getAlarmName()), 
					Time.of(vInt.getTimestamp()),
					Display.none());
		}
		return null;
	}

	private static org.diirt.vtype.Alarm fromEpicsAlarm(Alarm alarm) {
		return ValueFactory.newAlarm(fromEpicsAlarmSeverity(alarm.getSeverity()), alarm.getName());
	}
	
	private static Alarm toEpicsAlarm(org.diirt.vtype.AlarmSeverity alarmSeverity, String alarmName) {
		return Alarm.of(toEpicsAlarmSeverity(alarmSeverity), AlarmStatus.NONE, alarmName);
	}

	private static org.diirt.vtype.AlarmSeverity fromEpicsAlarmSeverity(AlarmSeverity alarmSeverity) {
		switch (alarmSeverity) {
		case INVALID:
			return org.diirt.vtype.AlarmSeverity.INVALID;
		case MINOR:
			return org.diirt.vtype.AlarmSeverity.MINOR;
		case MAJOR:
			return org.diirt.vtype.AlarmSeverity.MAJOR;
		case NONE:
			return org.diirt.vtype.AlarmSeverity.NONE;
		case UNDEFINED:
			return org.diirt.vtype.AlarmSeverity.UNDEFINED;
		default:

		}
		throw new RuntimeException("Alarm severity " + alarmSeverity.toString() + " not supported");
	}
	
	private static AlarmSeverity toEpicsAlarmSeverity(org.diirt.vtype.AlarmSeverity alarmSeverity) {
		switch(alarmSeverity) {
		case INVALID:
			return AlarmSeverity.INVALID;
		case MAJOR:
			return AlarmSeverity.MAJOR;
		case MINOR:
			return AlarmSeverity.MINOR;
		case NONE:
			return AlarmSeverity.NONE;
		case UNDEFINED:
			return AlarmSeverity.UNDEFINED;
		default:
		}
		throw new RuntimeException("Alarm severity " + alarmSeverity.toString() + " not supported");
	}
	

	private static org.diirt.vtype.Time fromEpicsTime(Time time) {
		return ValueFactory.newTime(time.getTimestamp());
	}
}


