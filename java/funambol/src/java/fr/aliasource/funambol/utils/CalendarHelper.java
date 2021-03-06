package fr.aliasource.funambol.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.obm.sync.calendar.Attendee;
import org.obm.sync.calendar.Event;
import org.obm.sync.calendar.EventRecurrence;
import org.obm.sync.calendar.ParticipationState;
import org.obm.sync.calendar.RecurrenceKind;

import com.funambol.common.pim.calendar.ExceptionToRecurrenceRule;
import com.funambol.common.pim.calendar.RecurrencePattern;
import com.funambol.common.pim.calendar.RecurrencePatternException;

public class CalendarHelper extends Helper {

	// --------------------------------------------------------------- Constants
	private static final String DATE_UTC_PATTERN = "yyyyMMdd'T'HHmmss'Z'";
	private static final String DATE_FORMAT = "yyyyMMdd";
	private static final String DATE_FORMAT_T = "yyyy-MM-dd";
	private static final String DATE_FORMAT_EU = "yyyyMMdd'T'HHmmss";

	private static final SimpleDateFormat dateFormat;
	private static final SimpleDateFormat dateFormatEurope;
	private static final SimpleDateFormat dateFormatTiret;
	private static final SimpleDateFormat dateFormatUTC;

	private static Log logger = LogFactory.getLog(CalendarHelper.class);

	private static final byte[] foundationWeekDays = {
			RecurrencePattern.DAY_OF_WEEK_SUNDAY,
			RecurrencePattern.DAY_OF_WEEK_MONDAY,
			RecurrencePattern.DAY_OF_WEEK_TUESDAY,
			RecurrencePattern.DAY_OF_WEEK_WEDNESDAY,
			RecurrencePattern.DAY_OF_WEEK_THURSDAY,
			RecurrencePattern.DAY_OF_WEEK_FRIDAY,
			RecurrencePattern.DAY_OF_WEEK_SATURDAY, };

	static {
		dateFormat = new SimpleDateFormat(DATE_FORMAT);
		dateFormatTiret = new SimpleDateFormat(DATE_FORMAT_T);

		dateFormatUTC = new SimpleDateFormat(DATE_UTC_PATTERN);
		dateFormatUTC.setTimeZone(TimeZone.getTimeZone("GMT"));

		dateFormatEurope = new SimpleDateFormat(DATE_FORMAT_EU);
		dateFormatEurope.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
	}

	// ------------ Public Methods -----------

	/**
	 * Returns the given date in utc format.
	 * 
	 * @param date
	 *            Date
	 * @return String
	 */
	public static String getUTCFormat(Date date) {
		String utc = null;
		if (date != null) {
			utc = dateFormatUTC.format(date);
		}
		logger.info("date: " + date + " converted to " + utc);
		return utc;
	}

	public static String getUTCFormatAllDay(Date date) {
		String utc = null;
		if (date != null) {
			utc = dateFormatUTC.format(date);
		}
		return utc;
	}

	/**
	 * Returns a java.util.Date from the given sDate in utc format.
	 * 
	 * @param sDate
	 *            String
	 * @return Date
	 * @throws Exception
	 */
	public static Date getDateFromUTCString(String sDate) {
		Date date = new Date();

		if (sDate != null) {
			try {
				if (sDate.contains("T")) {
					if (!sDate.endsWith("Z")) {
						date = dateFormatEurope.parse(sDate);
					} else {
						date = dateFormatUTC.parse(sDate);
					}
				} else {
					if (sDate.contains("-")) {
						date = dateFormatTiret.parse(sDate);
					} else {
						date = dateFormat.parse(sDate);
					}
				}
				logger.info("parsed '" + sDate + "' as '" + date + "'");
			} catch (ParseException e) {
				logger.error("cannot parse crappy date: " + sDate);
			}
		}

		return date;
	}

	/**
	 * Return the first category
	 * 
	 * @param propertyValueAsString
	 * @return
	 */
	public static String getOneCategory(String categories) {
		String ret = "";
		if (categories != null) {
			String[] result = categories.split(";|,");
			if (result.length > 0) {
				ret = result[0];
			}
		}
		return ret;
	}

	/**
	 * Convert an OBM reccurence in a foundation recurrence
	 * 
	 * @param obmrec
	 * @return
	 */
	public static RecurrencePattern getRecurrence(Date dstart, Date dend,
			EventRecurrence obmrec) {

		RecurrencePattern result = null;

		int interval = obmrec.getFrequence();
		Date cend = obmrec.getEnd();
		boolean noEndDate = true;
		Date endrec = dend;

		if (cend != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("GMT"));
			cal.setTime(cend);
			if (cal.get(Calendar.YEAR) > 2017) {
				cal.set(Calendar.YEAR, 2017);
			}
			noEndDate = false;
			endrec = cal.getTime();
		}

		String sPatternStart = getUTCFormat(dstart);
		String sPatternEnd = getUTCFormat(endrec);
		short dayOfWeekMask = getDayOfWeekMask(obmrec.getDays());

		try {
			if (obmrec.getKind() == RecurrenceKind.daily) {

				result = RecurrencePattern.getDailyRecurrencePattern(interval,
						sPatternStart, sPatternEnd, noEndDate);

			} else if (obmrec.getKind() == RecurrenceKind.weekly) {

				result = RecurrencePattern.getWeeklyRecurrencePattern(interval,
						dayOfWeekMask, sPatternStart, sPatternEnd, noEndDate);

			} else if (obmrec.getKind() == RecurrenceKind.monthlybydate) {

				result = RecurrencePattern.getMonthlyRecurrencePattern(
						interval, getDayOfMonth(dstart), sPatternStart,
						sPatternEnd, noEndDate);

			} else if (obmrec.getKind() == RecurrenceKind.monthlybyday) {

				result = RecurrencePattern.getMonthNthRecurrencePattern(
						interval, getDayOfWeek(dstart), getNthDay(dstart),
						sPatternStart, sPatternEnd);

			} else if (obmrec.getKind() == RecurrenceKind.yearly) {

				result = RecurrencePattern.getYearlyRecurrencePattern(interval,
						getDayOfMonth(dstart), getMonthOfYear(dstart),
						sPatternStart, sPatternEnd, noEndDate);

			}
		} catch (RecurrencePatternException e) {
			logger.error("recpattern: " + e.getMessage(), e);
		}
		return result;
	}

	private static short getMonthOfYear(Date date) {
		java.util.Calendar temp = java.util.Calendar.getInstance();
		temp.setTime(date);

		return (short) (temp.get(java.util.Calendar.MONTH) + 1);
	}

	private static short getNthDay(Date date) {
		java.util.Calendar temp = java.util.Calendar.getInstance();
		temp.setTime(date);

		return (short) temp.get(java.util.Calendar.DAY_OF_WEEK_IN_MONTH);
	}

	private static short getDayOfWeek(Date date) {
		java.util.Calendar temp = java.util.Calendar.getInstance();
		temp.setTime(date);

		short result = 0;

		switch (temp.get(java.util.Calendar.DAY_OF_WEEK)) {
		case java.util.Calendar.FRIDAY:
			result += RecurrencePattern.DAY_OF_WEEK_FRIDAY;
			break;
		case java.util.Calendar.MONDAY:
			result += RecurrencePattern.DAY_OF_WEEK_MONDAY;
			break;
		case java.util.Calendar.SATURDAY:
			result += RecurrencePattern.DAY_OF_WEEK_SATURDAY;
			break;
		case java.util.Calendar.SUNDAY:
			result += RecurrencePattern.DAY_OF_WEEK_SUNDAY;
			break;
		case java.util.Calendar.THURSDAY:
			result += RecurrencePattern.DAY_OF_WEEK_THURSDAY;
			break;
		case java.util.Calendar.TUESDAY:
			result += RecurrencePattern.DAY_OF_WEEK_TUESDAY;
			break;
		case java.util.Calendar.WEDNESDAY:
			result += RecurrencePattern.DAY_OF_WEEK_WEDNESDAY;
			break;
		default:
			break;
		}

		return result;
	}

	/**
	 * Get the day number (1-31) form a date
	 * 
	 * @param dstart
	 * @return
	 */
	private static short getDayOfMonth(Date date) {
		java.util.Calendar temp = java.util.Calendar.getInstance();
		temp.setTime(date);
		return (short) temp.get(java.util.Calendar.DAY_OF_MONTH);
	}

	private static short getDayOfWeekMask(String days) {
		short result = 0;

		if (days == null || days.equals("") || days.length() < 7) {
		} else {
			for (int i = 0; i < 7; i++) {
				if (days.charAt(i) == '1') {
					result += foundationWeekDays[i];
				}
			}
		}
		return result;
	}

	// Foundation to obm

	/**
	 * Construct an OBM event recurrence from a foundation recurrence
	 * 
	 * @param rec
	 * @param dend
	 * @param allDay
	 * @return
	 */
	public static EventRecurrence getRecurrenceFromFoundation(
			RecurrencePattern rec, Date dend, boolean allDay) {
		EventRecurrence recurrence = new EventRecurrence();

		recurrence.setFrequence(rec.getInterval());
		recurrence.setDays("");

		List<ExceptionToRecurrenceRule> recexs = rec.getExceptions();
		if (recexs != null) {
			Set<Date> exs = new HashSet<Date>();
			for (ExceptionToRecurrenceRule exceptionToRecurrenceRule : recexs) {
				exs.add(CalendarHelper
						.getDateFromUTCString(exceptionToRecurrenceRule
								.getDate()));
			}
			recurrence.setExceptions(exs.toArray(new Date[exs.size()]));
		} else {
			recurrence.setExceptions(new Date[0]);
		}

		java.util.Calendar cEndRec = java.util.Calendar.getInstance();
		logger.info("recurrence: " + rec);
		if (rec.getOccurrences() > 0) {
			Date begin = getDateFromUTCString(rec.getStartDatePattern());
			short type = rec.getTypeId();
			Calendar endTime = Calendar
					.getInstance(TimeZone.getTimeZone("GMT"));
			endTime.setTime(begin);
			switch (type) {
			case RecurrencePattern.TYPE_DAYLY:
				endTime.add(Calendar.DAY_OF_MONTH, (rec.getOccurrences() - 1)
						* rec.getInterval());
				break;
			case RecurrencePattern.TYPE_MONTHLY:
			case RecurrencePattern.TYPE_MONTH_NTH:
				endTime.add(Calendar.MONTH, (rec.getOccurrences() - 1)
						* rec.getInterval());
				break;
			case RecurrencePattern.TYPE_WEEKLY:
				endTime.add(Calendar.WEEK_OF_YEAR, (rec.getOccurrences() - 1)
						* rec.getInterval());
				break;
			case RecurrencePattern.TYPE_YEARLY:
			case RecurrencePattern.TYPE_YEAR_NTH:
				endTime.add(Calendar.YEAR, (rec.getOccurrences() - 1)
						* rec.getInterval());
				break;
			}
			// funambol perd la tz en calculant la startDatePattern : le 19 à
			// 23h utc (20 à 0h sur paris), devient le 19
			if (allDay) {
				endTime.add(Calendar.DAY_OF_YEAR, 1);
			}
			logger.info("Computed end date : " + endTime.getTime());
			cEndRec.setTime(endTime.getTime());
		} else if (!rec.isNoEndDate()) {
			Date dEndRec = getDateFromUTCString(rec.getEndDatePattern());
			cEndRec.setTime(dEndRec);
		} else {
			/* infinite */
			cEndRec.set(Calendar.YEAR, 2017);
			// cEndRec = null;
		}
		recurrence.setEnd(cEndRec.getTime());

		switch (rec.getTypeId()) {
		case RecurrencePattern.TYPE_DAYLY:
			recurrence.setKind(RecurrenceKind.daily);
			break;
		case RecurrencePattern.TYPE_WEEKLY:
			recurrence.setKind(RecurrenceKind.weekly);
			recurrence.setDays(getOBMDayOfWeekMask(rec.getDayOfWeekMask()));
			break;
		case RecurrencePattern.TYPE_MONTHLY:
			recurrence.setKind(RecurrenceKind.monthlybydate);
			break;
		case RecurrencePattern.TYPE_MONTH_NTH:
			// only one nth day supported by OBM
			recurrence.setKind(RecurrenceKind.monthlybyday);
			break;
		case RecurrencePattern.TYPE_YEARLY:
			recurrence.setKind(RecurrenceKind.yearly);
			break;
		case RecurrencePattern.TYPE_YEAR_NTH:
			// not supported by OBM
			recurrence.setKind(RecurrenceKind.yearly);
			break;
		}

		return recurrence;
	}

	private static String getOBMDayOfWeekMask(short dayOfWeekMask) {
		String result = "";

		for (int i = 0; i < 7; i++) {
			if ((dayOfWeekMask & foundationWeekDays[i]) == foundationWeekDays[i]) {
				result += "1";
			} else {
				result += "0";
			}
		}
		return result;
	}

	public static boolean isUserRefused(String userEmail, List<Attendee> list) {
		for (Attendee at : list) {
			if (at.getEmail().equals(userEmail)
					&& at.getState() == ParticipationState.DECLINED) {
				return true;
			}
		}
		return false;
	}

	public static void refuseEvent(Event event, String userEmail) {
		for (Attendee at : event.getAttendees()) {
			if (at.getEmail().equals(userEmail)) {
				at.setState(ParticipationState.DECLINED);
				logger.info("DECLINED for email " + userEmail);
				return;
			}
		}
		logger
				.error("Did not find attendee to refuse with email: "
						+ userEmail);
	}

	public static String formatWithTiret(String propValue) {
		if (propValue == null || propValue.length() == 0) {
			return "";
		}
		Date d = getDateFromUTCString(propValue);
		return dateFormatTiret.format(d);
	}

}
