/**
 * Copyright (c) 2012-2013, Gerald Garcia, Timo Berger
 *
 * This file is part of Andoid Caldav Sync Adapter Free.
 *
 * Andoid Caldav Sync Adapter Free is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or at your option any later version.
 *
 * Andoid Caldav Sync Adapter Free is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Andoid Caldav Sync Adapter Free.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gege.caldavsyncadapter.caldav.entities;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;
import de.we.acaldav.App;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.apache.http.client.ClientProtocolException;
import org.gege.caldavsyncadapter.Event;
import org.gege.caldavsyncadapter.android.entities.AndroidEvent;
import org.gege.caldavsyncadapter.caldav.CaldavFacade;
import org.gege.caldavsyncadapter.caldav.CaldavProtocolException;
import org.gege.caldavsyncadapter.caldav.xml.MultiStatusHandler;
import org.gege.caldavsyncadapter.caldav.xml.sax.MultiStatus;
import org.gege.caldavsyncadapter.caldav.xml.sax.Prop;
import org.gege.caldavsyncadapter.caldav.xml.sax.PropStat;
import org.gege.caldavsyncadapter.caldav.xml.sax.Response;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class CalendarEvent extends org.gege.caldavsyncadapter.Event
{

	private static final String TAG = "CalendarEvent";

	public URL calendarURL;

	private String stringIcs;

	private Calendar calendar;

	private Component calendarComponent;

	private String eTag;

	private URI muri;

	private Uri mAndroidEventUri;

	private boolean mAllDay = false;

	private VTimeZone mVTimeZone = null;

	private TimeZone mTimeZone = null;

	private String mstrTimeZoneStart = "";

	private String mstrTimeZoneEnd = "";

	private Account mAccount = null;

	private ContentProviderClient mProvider = null;

	private String mstrcIcalPropertyError = "net.fortunal.ical4j.invalid:";

	public CalendarEvent(Account account, ContentProviderClient provider)
	{
		this.mAccount  = account;
		this.mProvider = provider;
	}

	private static Uri asSyncAdapter(Uri uri, String account, String accountType)
	{
		return uri.buildUpon()
		    .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
		    .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
		    .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType)
		    .build();
	}

	public String getETag()
	{
		return eTag;
	}

	public void setETag(String eTag)
	{
		this.eTag = eTag;
	}

	public URI getUri()
	{
		return muri;
	}

	public void setUri(URI uri)
	{
		this.muri = uri;
	}

	public void setICSasString(String ics)
	{
		this.stringIcs = ics;
	}

	public boolean setICSasMultiStatus(String stringMultiStatus)
	{
		boolean             Result = false;
		String              ics    = "";
		MultiStatus         multistatus;
		ArrayList<Response> responselist;
		Response            response;
		PropStat            propstat;
		Prop                prop;
		try
		{
			SAXParserFactory   factory        = SAXParserFactory.newInstance();
			SAXParser          parser         = factory.newSAXParser();
			XMLReader          reader         = parser.getXMLReader();
			MultiStatusHandler contentHandler = new MultiStatusHandler();
			reader.setContentHandler(contentHandler);
			reader.parse(new InputSource(new StringReader(stringMultiStatus)));

			multistatus = contentHandler.mMultiStatus;
			if (multistatus != null)
			{
				responselist = multistatus.responseList;
				if (responselist.size() == 1)
				{
					response = responselist.get(0);
					// HINT: bugfix for google calendar, zimbra replace("@", "%40")
					if (response.href.replace("@", "%40").equals(this.getUri().getRawPath().replace("@", "%40")))
					{
						propstat = response.propstat;
						if (propstat != null)
						{
							if (propstat.status.contains("200 OK"))
							{
								prop = propstat.prop;
								ics  = prop.calendardata;
								this.setETag(prop.getetag);
								Result = true;
							}
						}
					}
				}
			}
		}
		catch (ParserConfigurationException e1)
		{
			e1.printStackTrace();
		}
		catch (SAXException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		this.stringIcs = ics;
		return Result;
	}

	/**
	 * gets the Uri of the android event
	 */
	public Uri getAndroidEventUri()
	{
		return mAndroidEventUri;
	}

	public void setAndroidEventUri(Uri uri)
	{
		mAndroidEventUri = uri;
	}

	public boolean readContentValues()
	{
		this.ContentValues.put(Events.DTSTART, this.getStartTime());
		this.ContentValues.put(Events.EVENT_TIMEZONE, this.getTimeZoneStart());

		if (this.getRRule() == null && this.getRDate() == null)
		{
			this.ContentValues.put(Events.DTEND, this.getEndTime());
			this.ContentValues.put(Events.EVENT_END_TIMEZONE, this.getTimeZoneEnd());
		}
		else
		{
			this.ContentValues.put(Events.DURATION, this.getDuration());
		}
		int AllDay = this.getAllDay();
		this.ContentValues.put(Events.ALL_DAY, AllDay);

		this.ContentValues.put(Events.TITLE, this.getTitle());
		this.ContentValues.put(Events._SYNC_ID, this.getUri().toString());
		this.ContentValues.put(ETAG, this.getETag());
		this.ContentValues.put(Events.DESCRIPTION, this.getDescription());
		this.ContentValues.put(Events.EVENT_LOCATION, this.getLocation());
		this.ContentValues.put(Events.ACCESS_LEVEL, this.getAccessLevel());
		this.ContentValues.put(Events.STATUS, this.getStatus());
		this.ContentValues.put(Events.RDATE, this.getRDate());
		this.ContentValues.put(Events.RRULE, this.getRRule());
		this.ContentValues.put(Events.EXRULE, this.getExRule());
		this.ContentValues.put(Events.EXDATE, this.getExDate());
		this.ContentValues.put(UID, this.getUid());
		this.ContentValues.put(RAWDATA, this.stringIcs);

		return true;
	}

	/**
	 * receives a single event and parses its content
	 *
	 * @return success of this function
	 * @see CalendarEvent#parseIcs()
	 */
	public boolean fetchBody() throws ClientProtocolException, IOException, CaldavProtocolException, ParserException
	{
		boolean error = false;

		CaldavFacade.getEvent(this);

		boolean parsed = this.parseIcs();
		if (!parsed)
		{
			error = true;
		}

		return !error;
	}

	public java.util.ArrayList<ContentValues> getReminders()
	{
		java.util.ArrayList<ContentValues> lcResult = new java.util.ArrayList<ContentValues>();
		ContentValues                      Reminder;

		/*
		 * http://sourceforge.net/tracker/?func=detail&aid=3021704&group_id=107024&atid=646395
		 */

		net.fortuna.ical4j.model.component.VEvent event = (VEvent)this.calendarComponent;

		ComponentList ComList = event.getAlarms();

		if (ComList != null)
		{
			for (Object objCom : ComList)
			{
				Component Com = (Component)objCom;
				Reminder      = new ContentValues();

				Property TRIGGER = Com.getProperty("TRIGGER");
				if (TRIGGER != null)
				{
					Dur Duration = new Dur(TRIGGER.getValue());

					int intDuration = Duration.getMinutes() + Duration.getHours() * 60 + Duration.getDays() * 60 * 24;

					Reminder.put(Reminders.EVENT_ID, ContentUris.parseId(mAndroidEventUri));
					Reminder.put(Reminders.METHOD, Reminders.METHOD_ALERT);
					Reminder.put(Reminders.MINUTES, intDuration);

					lcResult.add(Reminder);
				}
			}
		}
		return lcResult;
	}

	public java.util.ArrayList<ContentValues> getAttandees()
	{
		java.util.ArrayList<ContentValues> lcResult = new java.util.ArrayList<ContentValues>();
		ContentValues                      Attendee;
		PropertyList                       lcPropertys = calendarComponent.getProperties(Property.ATTENDEE);
		if (lcPropertys != null)
		{
			for (Object objProperty : lcPropertys)
			{
				Property property = (Property)objProperty;
				Attendee          = ReadAttendeeProperties(property, Property.ATTENDEE);
				if (Attendee != null)
				{
					lcResult.add(Attendee);
				}
			}
		}
		lcPropertys = calendarComponent.getProperties(Property.ORGANIZER);
		if (lcPropertys != null)
		{
			for (Object objProperty : lcPropertys)
			{
				Property property = (Property)objProperty;
				Attendee          = ReadAttendeeProperties(property, Property.ORGANIZER);
				if (Attendee != null)
				{
					lcResult.add(Attendee);
				}
			}
		}

		return lcResult;
	}

	private ContentValues ReadAttendeeProperties(Property property, String Type)
	{
		ContentValues lcAttendee = null;

		ParameterList lcParameters = property.getParameters();
		Parameter     lcCN         = lcParameters.getParameter(Cn.CN);
		Parameter     lcROLE       = lcParameters.getParameter(Role.ROLE);
		Parameter     lcCUTYPE     = lcParameters.getParameter(CuType.CUTYPE);
		Parameter     lcPARTSTAT   = lcParameters.getParameter(PartStat.PARTSTAT);

		String strCN       = "";
		String strROLE     = "";
		String strCUTYPE   = "";
		String strValue    = property.getValue().replace("mailto:", "");
		String strPARTSTAT = "";

		if (strValue.startsWith(mstrcIcalPropertyError))
		{
			strValue = strValue.replace(mstrcIcalPropertyError, "");
			try
			{
				strValue = URLDecoder.decode(strValue, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e(getETag(), e.getMessage());
			}
		}

		if (lcCN != null)
		{
			strCN = lcCN.getValue();
		}
		if (lcROLE != null)
		{
			strROLE = lcROLE.getValue();
		}
		if (lcCUTYPE != null)
		{
			strCUTYPE = lcCUTYPE.getValue();
		}
		if (lcPARTSTAT != null)
		{
			strPARTSTAT = lcPARTSTAT.getValue();
		}

		if (strCN.equals(""))
		{
			if (!strValue.equals(""))
			{
				strCN = strValue;
			}
		}

		if (!strCN.equals(""))
		{
			if (strCUTYPE.equals("") || strCUTYPE.equals("INDIVIDUAL"))
			{
				lcAttendee = new ContentValues();

				lcAttendee.put(Attendees.EVENT_ID, ContentUris.parseId(mAndroidEventUri));

				lcAttendee.put(Attendees.ATTENDEE_NAME, strCN);
				lcAttendee.put(Attendees.ATTENDEE_EMAIL, strValue);

				if (strROLE.equals("OPT-PARTICIPANT"))
				{
					lcAttendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_OPTIONAL);
				}
				else if (strROLE.equals("NON-PARTICIPANT"))
				{
					lcAttendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_NONE);
				}
				else if (strROLE.equals("REQ-PARTICIPANT"))
				{
					lcAttendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
				}
				else if (strROLE.equals("CHAIR"))
				{
					lcAttendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
				}
				else
				{
					lcAttendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_NONE);
				}

				if (Type.equals(Property.ATTENDEE))
				{
					lcAttendee.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ATTENDEE);
				}
				else if (Type.equals(Property.ORGANIZER))
				{
					lcAttendee.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ORGANIZER);
				}
				else
				{
					lcAttendee.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_NONE);
				}

				if (strPARTSTAT.equals(PartStat.NEEDS_ACTION.getValue()))
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);
				}
				else if (strPARTSTAT.equals(PartStat.ACCEPTED.getValue()))
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_ACCEPTED);
				}
				else if (strPARTSTAT.equals(PartStat.DECLINED.getValue()))
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_DECLINED);
				}
				else if (strPARTSTAT.equals(PartStat.COMPLETED.getValue()))
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
				}
				else if (strPARTSTAT.equals(PartStat.TENTATIVE.getValue()))
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_TENTATIVE);
				}
				else
				{
					lcAttendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);
				}
			}
		}

		return lcAttendee;
	}

	private long getAccessLevel()
	{
		long     lcResult = Events.ACCESS_DEFAULT;
		String   lcValue  = "";
		Property property = calendarComponent.getProperty(Property.CLASS);
		if (property != null)
		{
			lcValue = property.getValue();
			if (lcValue.equals(Clazz.PUBLIC))
			{
				lcResult = Events.ACCESS_PUBLIC;
			}
			else if (lcValue.equals(Clazz.PRIVATE))
			{
				lcResult = Events.ACCESS_PRIVATE;
			}
			else if (lcValue.equals(Clazz.CONFIDENTIAL))
			{
				lcResult = Events.ACCESS_PRIVATE; // should be ACCESS_CONFIDENTIAL, but is not implemented within Android
			}
		}

		return lcResult;
	}

	private int getStatus()
	{
		int      lcResult = -1;
		String   lcValue  = "";
		Property property = calendarComponent.getProperty(Property.STATUS);
		if (property != null)
		{
			lcValue = property.getValue();
			if (lcValue.equals(Status.VEVENT_CONFIRMED.getValue()))
			{
				lcResult = Events.STATUS_CONFIRMED;
			}
			else if (lcValue.equals(Status.VEVENT_CANCELLED.getValue()))
			{
				lcResult = Events.STATUS_CANCELED;
			}
			else if (lcValue.equals(Status.VEVENT_TENTATIVE.getValue()))
			{
				lcResult = Events.STATUS_TENTATIVE;
			}
		}

		return lcResult;
	}

	private String getDescription()
	{
		String   lcResult = null;
		Property property = calendarComponent.getProperty(Property.DESCRIPTION);
		if (property != null)
		{
			lcResult = property.getValue();
		}

		return lcResult;
	}

	private String getLocation()
	{
		String   lcResult = null;
		Property property = calendarComponent.getProperty(Property.LOCATION);
		if (property != null)
		{
			lcResult = property.getValue();
		}

		return lcResult;
	}

	private String getTitle()
	{
		Property property = calendarComponent.getProperty(Property.SUMMARY);
		if (property != null)
		{
			return property.getValue();
		}
		else
		{
			return "**unkonwn**";
		}
	}

	private String getRRule()
	{
		String   lcResult = null;
		Property property = calendarComponent.getProperty(Property.RRULE);
		if (property != null)
		{
			lcResult = property.getValue();
		}

		return lcResult;
	}

	private String getExRule()
	{
		String   lcResult = null;
		Property property = calendarComponent.getProperty(Property.EXRULE);
		if (property != null)
		{
			lcResult = property.getValue();
		}

		return lcResult;
	}

	private String getRDate()
	{
		String lcResult = null;

		java.util.ArrayList<String> lcExDates = this.getRDates();
		for (String lcValue : lcExDates)
		{
			if (lcResult == null)
			{
				lcResult = "";
			}
			if (!lcResult.isEmpty())
			{
				lcResult += ",";
			}
			lcResult += lcValue;
		}

		return lcResult;
	}

	private java.util.ArrayList<String> getRDates()
	{
		ArrayList<String> lcResult  = new ArrayList<String>();
		PropertyList      Propertys = calendarComponent.getProperties(Property.RDATE);
		if (Propertys != null)
		{
			Property property;
			for (Object objProperty : Propertys)
			{
				property = (Property)objProperty;
				lcResult.add(property.getValue());
			}
		}

		return lcResult;
	}

	private String getExDate()
	{
		String lcResult = null;

		java.util.ArrayList<String> lcExDates = this.getExDates();
		for (String lcValue : lcExDates)
		{
			if (lcResult == null)
			{
				lcResult = "";
			}
			if (!lcResult.isEmpty())
			{
				lcResult += ",";
			}
			lcResult += lcValue;
		}

		return lcResult;
	}

	private java.util.ArrayList<String> getExDates()
	{
		ArrayList<String> lcResult                     = new ArrayList<String>();
		PropertyList      lcCalendarComponentPropertys = calendarComponent.getProperties(Property.EXDATE);
		if (lcCalendarComponentPropertys != null)
		{
			Property property;
			for (Object objProperty : lcCalendarComponentPropertys)
			{
				property             = (Property)objProperty;
				String propertyValue = property.getValue();
				if (property.getParameter("TZID") != null && !"UTC".equals(property.getParameter("TZID").getValue()))
				{
					try
					{
						SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
						df.setTimeZone(TimeZone.getTimeZone(property.getParameter("TZID").getValue()));
						Date date = df.parse(propertyValue);
						df.setTimeZone(TimeZone.getTimeZone("UTC"));
						propertyValue = df.format(date) + "Z";
					}
					catch (ParseException e)
					{
						Log.w("EXDATE", "Error during EXDATE parsing: " + e.getMessage());
					}
				}
				lcResult.add(propertyValue);
			}
		}

		return lcResult;
	}

	private String getUid()
	{
		String   lcResult = "";
		Property prop     = calendarComponent.getProperty(Property.UID);
		if (prop != null)
		{
			lcResult = prop.getValue();
		}

		return lcResult;
	}

	private Long getTimestamp(Property prop)
	{
		Long   lcResult    = null;
		String strTimeZone = "";
		try
		{
			String strDate = prop.getValue();

			Parameter parAllDay = prop.getParameter("VALUE");
			if (parAllDay != null)
			{
				if (parAllDay.getValue().equals("DATE"))
				{
					mAllDay = true;
					strDate += "T000000Z";
				}
			}

			Parameter propTZ = prop.getParameter(Property.TZID);
			if (propTZ != null)
			{
				strTimeZone = propTZ.getValue();
			}
			if (!strTimeZone.equals(""))
			{

				// 20130331T120000
				int lcYear   = Integer.parseInt(strDate.substring(0, 4));
				int lcMonth  = Integer.parseInt(strDate.substring(4, 6)) - 1;
				int lcDay    = Integer.parseInt(strDate.substring(6, 8));
				int lcHour   = Integer.parseInt(strDate.substring(9, 11));
				int lcMinute = Integer.parseInt(strDate.substring(11, 13));
				int lcSecond = Integer.parseInt(strDate.substring(13, 15));

				// time in UTC
				java.util.TimeZone jtz = java.util.TimeZone.getTimeZone("UTC");
				java.util.Calendar cal = java.util.GregorianCalendar.getInstance(jtz);
				cal.set(lcYear, lcMonth, lcDay, lcHour, lcMinute, lcSecond);
				cal.set(java.util.Calendar.MILLISECOND, 0);
				lcResult = cal.getTimeInMillis();

				// get the timezone
				String[] lcIDs  = java.util.TimeZone.getAvailableIDs();
				Boolean lcFound = false;
				for (int i = 0; i < lcIDs.length; i++)
				{
					lcFound = lcFound || lcIDs[i].equals(strTimeZone);
				}
				if (lcFound)
				{
					jtz = java.util.TimeZone.getTimeZone(strTimeZone);

					// subtract the offset
					lcResult -= jtz.getRawOffset();

					// remove dst
					if (jtz.inDaylightTime(new java.util.Date(lcResult)))
					{
						lcResult -= jtz.getDSTSavings();
					}
				}
				else
				{
					if (mTimeZone != null)
					{
						// subtract the offset
						lcResult -= mTimeZone.getRawOffset();

						// remove dst
						if (mTimeZone.inDaylightTime(new java.util.Date(lcResult)))
						{
							lcResult -= mTimeZone.getDSTSavings();
						}
					}
					else
					{
						// unknown Time?
						// treat as local time, should not happen too often :)
						lcResult = new DateTime(strDate).getTime();
					}
				}
			}
			else
			{
				if (strDate.endsWith("Z"))
				{
					// GMT or UTC
					lcResult = new DateTime(strDate).getTime();
				}
				else
				{
					// unknown Time?
					// treat as local time, should not happen too often :)
					lcResult = new DateTime(strDate).getTime();
				}
			}
		}
		catch (ParseException e)
		{
			Log.e(getETag(), e.getMessage());
		}

		return lcResult;
	}

	private long getStartTime()
	{
		long     lcResult = 0;
		Property prop;

		prop = calendarComponent.getProperty(Property.DTSTART);
		if (prop != null)
		{
			Parameter propTZ = prop.getParameter(Property.TZID);
			if (propTZ != null)
			{
				mstrTimeZoneStart = propTZ.getValue();
			}
			lcResult = getTimestamp(prop);
		}

		return lcResult;
	}

	private long getEndTime()
	{
		long     lcResult = 0;
		Property propDtEnd;
		Property propDuration;

		propDtEnd    = calendarComponent.getProperty(Property.DTEND);
		propDuration = calendarComponent.getProperty(Property.DURATION);
		if (propDtEnd != null)
		{
			Parameter propTZ = propDtEnd.getParameter(Property.TZID);
			if (propTZ != null)
			{
				mstrTimeZoneEnd = propTZ.getValue();
			}
			lcResult = getTimestamp(propDtEnd);
		}
		else if (propDuration != null)
		{
			long   Start       = this.getStartTime();
			String strDuration = propDuration.getValue();
			Dur    dur         = new Dur(strDuration);
			lcResult = Start + dur.getSeconds() * 1000 + dur.getMinutes() * 60 * 1000 + dur.getHours() * 60 * 60 * 1000 + dur.getDays() * 60 * 60 * 24 * 1000;

			mstrTimeZoneEnd = mstrTimeZoneStart;
		}

		return lcResult;
	}

	private String getTimeZoneStart()
	{
		String lcResult = "GMT";

		if (!mstrTimeZoneStart.equals(""))
		{
			lcResult = mstrTimeZoneStart;
		}

		return lcResult;
	}

	private String getTimeZoneEnd()
	{
		String lcResult = "GMT";

		if (!mstrTimeZoneEnd.equals(""))
		{
			lcResult = mstrTimeZoneEnd;
		}

		return lcResult;
	}

	private String getDuration()
	{
		String   lcResult = "";
		Property prop     = calendarComponent.getProperty("DURATION");

		if (prop != null)
		{
			// DURATION:PT1H
			lcResult = prop.getValue();
		}
		else
		{
			// no DURATION given by this event. we have to calculate it by ourself
			lcResult            = "P";
			long lcStartTime    = this.getStartTime();
			long lcEndTime      = this.getEndTime();
			long lcDurationTime = 0;
			if (lcEndTime != 0)
			{
				lcDurationTime = (lcEndTime - lcStartTime) / 1000; // get rid of the milliseconds, they cann't be described with RFC2445-Duration
			}

			if (lcDurationTime < 0)
			{
				lcDurationTime = 0;
			}
			int Days    = (int)Math.ceil(lcDurationTime / 24 / 60 / 60);
			int Hours   = (int)Math.ceil((lcDurationTime - (Days * 24 * 60 * 60)) / 60 / 60);
			int Minutes = (int)Math.ceil((lcDurationTime - (Days * 24 * 60 * 60) - (Hours * 60 * 60)) / 60);
			int Seconds = (int)(lcDurationTime - (Days * 24 * 60 * 60) - (Hours * 60 * 60) - (Minutes * 60));

			if (Days > 0)
			{
				lcResult += String.valueOf(Days) + "D";
			}

			if (!mAllDay)
			{
				// if a ALL_DAY event occurs, there is no need for hours, minutes and seconds (Android doesn't understand them)
				lcResult += "T";
				lcResult += String.valueOf(Hours) + "H";
				lcResult += String.valueOf(Minutes) + "M";
				lcResult += String.valueOf(Seconds) + "S";
			}
		}

		return lcResult;
	}

	private int getAllDay()
	{
		int lcResult = 0;

		if (mAllDay)
		{
			lcResult = 1;
		}

		return lcResult;
	}

	/**
	 * opens the first items of the event
	 *
	 * @return success of this function
	 * @see AndroidEvent#createIcs(String)
	 * @see CalendarEvent#fetchBody()
	 */
	private boolean parseIcs() throws CaldavProtocolException, IOException, ParserException
	{
		boolean lcError = false;
		Thread.currentThread().setContextClassLoader(App.getContext().getClassLoader());
		CalendarBuilder builder = new CalendarBuilder();
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);

		StringReader reader = new StringReader(this.stringIcs);
		try
		{
			this.calendar = builder.build(reader);
		}
		catch (ParserException ex)
		{
			// ical4j fails with this: "Cannot set timezone for UTC properties"
			// CREATED;TZID=America/New_York:20130129T140250
			lcError = true;
		}

		if (!lcError)
		{
			ComponentList components = null;
			components               = this.calendar.getComponents(Component.VEVENT);
			if (components.size() == 0)
			{
				components = this.calendar.getComponents(Component.VTODO);
				if (components.size() == 0)
				{
					throw new CaldavProtocolException("unknown events in ICS");
				}
				else
				{
					Log.e(TAG, "unsupported event TODO in ICS");
					lcError = true;
				}
			}
			else if (components.size() > 1)
			{
				Log.e(TAG, "Several events in ICS -> only first will be processed");
			}

			// get the TimeZone information
			Component mCom = this.calendar.getComponent(Component.VTIMEZONE);
			if (mCom != null)
			{
				mVTimeZone = (VTimeZone)this.calendar.getComponent(Component.VTIMEZONE);
			}
			if (mVTimeZone != null)
			{
				mTimeZone = new TimeZone(mVTimeZone);
			}

			if (!lcError)
			{
				calendarComponent = (Component)components.get(0);
			}
		}

		return !lcError;
	}

	/**
	 * searches for an android event
	 *
	 * @return the android event
	 */
	public AndroidEvent getAndroidEvent(DavCalendar androidCalendar) throws RemoteException
	{
		boolean      lcError      = false;
		Uri          uriEvents    = Events.CONTENT_URI;
		Uri          uriAttendee  = Attendees.CONTENT_URI;
		Uri          uriReminder  = Reminders.CONTENT_URI;
		AndroidEvent androidEvent = null;

		String selection       = "(" + Events._SYNC_ID + " = ?)";
		String[] selectionArgs = new String[] {this.getUri().toString()};
		Cursor curEvent        = this.mProvider.query(uriEvents, null, selection, selectionArgs, null);

		Cursor curAttendee = null;
		Cursor curReminder = null;

		if (curEvent == null)
		{
			lcError = true;
		}
		if (!lcError)
		{
			if (curEvent.getCount() == 0)
			{
				lcError = true;
			}
		}
		if (!lcError)
		{
			curEvent.moveToNext();

			long EventID     = curEvent.getLong(curEvent.getColumnIndex(Events._ID));
			Uri  returnedUri = ContentUris.withAppendedId(uriEvents, EventID);

			androidEvent = new AndroidEvent(returnedUri, androidCalendar.getAndroidCalendarUri());
			androidEvent.readContentValues(curEvent);

			selection     = "(" + Attendees.EVENT_ID + " = ?)";
			selectionArgs = new String[] {String.valueOf(EventID)};
			curAttendee   = this.mProvider.query(uriAttendee, null, selection, selectionArgs, null);
			selection     = "(" + Reminders.EVENT_ID + " = ?)";
			selectionArgs = new String[] {String.valueOf(EventID)};
			curReminder   = this.mProvider.query(uriReminder, null, selection, selectionArgs, null);
			androidEvent.readAttendees(curAttendee);
			androidEvent.readReminder(curReminder);
			curAttendee.close();
			curReminder.close();
		}
		if (curEvent != null)
		{
			curEvent.close();
		}

		return androidEvent;
	}

	/**
	 * creates a new androidEvent from a given calendarEvent
	 */
	public boolean createAndroidEvent(DavCalendar androidCalendar)
	    throws ClientProtocolException, IOException, CaldavProtocolException, RemoteException, ParserException
	{
		boolean lcResult         = false;
		boolean lcBodyFetched    = this.fetchBody();
		int     lcCountAttendees = 0;
		int     lcCountReminders = 0;

		if (lcBodyFetched)
		{
			this.readContentValues();
			this.setAndroidCalendarId(ContentUris.parseId(androidCalendar.getAndroidCalendarUri()));

			Uri uri = this.mProvider.insert(asSyncAdapter(Events.CONTENT_URI, this.mAccount.name, this.mAccount.type), this.ContentValues);

			if (uri == null)
			{
				throw new ParserException((int)this.getAndroidCalendarId());
			}
			this.setAndroidEventUri(uri);

			Log.d(TAG, "Creating calendar event for " + uri.toString());

			// check the attendees
			ArrayList<ContentValues> AttendeeList = this.getAttandees();
			for (ContentValues Attendee : AttendeeList)
			{
				this.mProvider.insert(Attendees.CONTENT_URI, Attendee);
				lcCountAttendees += 1;
			}

			// check the reminders
			ArrayList<ContentValues> ReminderList = this.getReminders();
			for (ContentValues Reminder : ReminderList)
			{
				this.mProvider.insert(Reminders.CONTENT_URI, Reminder);
				lcCountReminders += 1;
			}

			if ((lcCountAttendees > 0) || (lcCountReminders > 0))
			{
				// the events gets dirty when attendees or reminders were added
				AndroidEvent androidEvent = this.getAndroidEvent(androidCalendar);

				androidEvent.ContentValues.put(Events.DIRTY, 0);
				int RowCount = this.mProvider.update(
				    asSyncAdapter(androidEvent.getUri(), this.mAccount.name, this.mAccount.type), androidEvent.ContentValues, null, null);
				lcResult = (RowCount == 1);
			}
			else
			{
				lcResult = true;
			}
		}
		return lcResult;
	}

	/**
	 * the android event is getting updated
	 */
	public boolean updateAndroidEvent(AndroidEvent androidEvent)
	    throws ClientProtocolException, IOException, CaldavProtocolException, RemoteException, ParserException
	{
		boolean BodyFetched = this.fetchBody();
		int     lcRows      = 0;

		if (BodyFetched)
		{
			this.readContentValues();
			this.setAndroidCalendarId(androidEvent.getAndroidCalendarId());
			this.setAndroidEventUri(androidEvent.getUri());

			Log.d(TAG, "AndroidEvent is dirty: " + androidEvent.ContentValues.getAsString(Events.DIRTY));

			if (androidEvent.checkEventValuesChanged(this.ContentValues))
			{
				// just set the raw data from server event into android event
				if (androidEvent.ContentValues.containsKey(Event.RAWDATA))
				{
					androidEvent.ContentValues.remove(Event.RAWDATA);
				}
				androidEvent.ContentValues.put(Event.RAWDATA, this.ContentValues.getAsString(Event.RAWDATA));

				// update the attendees and reminders
				this.updateAndroidAttendees();
				this.updateAndroidReminder();

				androidEvent.ContentValues.put(Events.DIRTY, 0); // the event is now in sync
				Log.d(TAG, "Update calendar event: for " + androidEvent.getUri());

				lcRows = mProvider.update(asSyncAdapter(androidEvent.getUri(), mAccount.name, mAccount.type), androidEvent.ContentValues, null, null);
				// Log.i(TAG, "Updated calendar event: rows effected " + Rows.toString());
			}
			else
			{
				Log.d(TAG, "Update calendar event not needed: for " + androidEvent.getUri());
			}
		}
		return (lcRows == 1);
	}

	/**
	 * updates the attendees from a calendarEvent to its androidEvent.
	 * the calendarEvent has to know its androidEvent via {@link CalendarEvent#setAndroidEventUri(Uri)}
	 */
	private boolean updateAndroidAttendees()
	{
		boolean lcResult = false;

		try
		{
			String mSelectionClause = "(" + Attendees.EVENT_ID + " = ?)";
			String[] mSelectionArgs = {Long.toString(ContentUris.parseId(this.getAndroidEventUri()))};
			int RowDelete;
			RowDelete = this.mProvider.delete(Attendees.CONTENT_URI, mSelectionClause, mSelectionArgs);
			Log.d(TAG, "Attendees Deleted:" + String.valueOf(RowDelete));

			java.util.ArrayList<ContentValues> AttendeeList = this.getAttandees();
			for (ContentValues Attendee : AttendeeList)
			{
				this.mProvider.insert(Attendees.CONTENT_URI, Attendee);
			}
			Log.d(TAG, "Attendees Inserted:" + String.valueOf(AttendeeList.size()));
			lcResult = true;
		}
		catch (RemoteException e)
		{
			Log.e(getETag(), e.getMessage());
		}

		return lcResult;
	}

	/**
	 * update the reminders from a calendarEvent to its androidEvent.
	 * the calendarEvent has to know its androidEvent via {@link CalendarEvent#setAndroidEventUri(Uri)}
	 */
	private boolean updateAndroidReminder()
	{
		boolean lcResult = false;

		try
		{
			String mSelectionClause = "(" + Reminders.EVENT_ID + " = ?)";
			String[] mSelectionArgs = {Long.toString(ContentUris.parseId(this.getAndroidEventUri()))};
			int lcRowDelete;
			lcRowDelete = this.mProvider.delete(Reminders.CONTENT_URI, mSelectionClause, mSelectionArgs);
			Log.d(TAG, "Reminders Deleted:" + String.valueOf(lcRowDelete));

			Uri                                lcReminderUri;
			java.util.ArrayList<ContentValues> ReminderList = this.getReminders();
			for (ContentValues Reminder : ReminderList)
			{
				lcReminderUri = this.mProvider.insert(Reminders.CONTENT_URI, Reminder);
				System.out.println(lcReminderUri);
			}
			Log.d(TAG, "Reminders Inserted:" + String.valueOf(ReminderList.size()));

			lcResult = true;
		}
		catch (RemoteException e)
		{
			Log.e(getETag(), e.getMessage());
		}

		return lcResult;
	}
}
