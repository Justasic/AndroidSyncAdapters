package org.gege.caldavsyncadapter.caldav.entities;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import java.net.URI;
import java.util.ArrayList;
import org.gege.caldavsyncadapter.caldav.entities.DavCalendar.CalendarSource;
import org.gege.caldavsyncadapter.syncadapter.notifications.NotificationsHelper;

// import org.gege.caldavsyncadapter.CalendarColors;
// import android.content.ContentUris;
// import android.content.ContentValues;
// import android.util.Log;

public class CalendarList
{
	//	private static final String TAG = "CalendarList";

	public CalendarSource Source = CalendarSource.undefined;

	public String ServerUrl = "";

	private java.util.ArrayList<DavCalendar> mList = new java.util.ArrayList<DavCalendar>();

	private Account mAccount = null;

	private ContentProviderClient mProvider = null;

	public CalendarList(Account account, ContentProviderClient provider, CalendarSource source, String serverUrl)
	{
		this.mAccount  = account;
		this.mProvider = provider;
		this.Source    = source;
		this.ServerUrl = serverUrl;
	}

	public DavCalendar getCalendarByURI(URI calendarURI)
	{
		DavCalendar lcResult = null;

		for (DavCalendar Item : mList)
		{
			if (Item.getURI().equals(calendarURI))
			{
				lcResult = Item;
			}
		}

		return lcResult;
	}

	public DavCalendar getCalendarByAndroidUri(Uri androidCalendarUri)
	{
		DavCalendar lcResult = null;

		for (DavCalendar lcDavCalendars : mList)
		{
			if (lcDavCalendars.getAndroidCalendarUri().equals(androidCalendarUri))
			{
				lcResult = lcDavCalendars;
			}
		}

		return lcResult;
	}

	/**
	 * function to get all calendars from client side android
	 */
	public boolean readCalendarFromClient()
	{
		boolean lcResult = false;
		Cursor  cur      = null;

		Uri uri = Calendars.CONTENT_URI;

		/* COMPAT: in the past, the serverurl was not stored within a calendar. (see #98)
		 * so there was no chance to see which calendars belongs to a named account.
		 * username + serverurl have to be unique
		 * ((DavCalendar.SERVERURL = ?) OR (DavCalendar.SERVERURL IS NULL))
		 */
		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND "
		    + "(" + Calendars.ACCOUNT_TYPE + " = ?))";

		String[] selectionArgs = new String[] {mAccount.name, mAccount.type};

		String[] projection = new String[] {Calendars._ID, Calendars.NAME, Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE};

		// Submit the query and get a Cursor object back.
		try
		{
			cur = mProvider.query(uri, null, selection, selectionArgs, Calendars._ID + " ASC");
		}
		catch (RemoteException e)
		{
			Log.e(this.getClass().getCanonicalName(), e.getMessage());
		}
		if (cur != null)
		{
			while (cur.moveToNext())
			{
				mList.add(new DavCalendar(mAccount, mProvider, cur, this.Source, this.ServerUrl));
			}
			cur.close();
			lcResult = true;
		}

		return lcResult;
	}

	public boolean deleteCalendarOnClientSideOnly(android.content.Context context)
	{
		boolean lcResult = false;

		for (DavCalendar androidCalendar : this.mList)
		{
			if (!androidCalendar.foundServerSide)
			{
				NotificationsHelper.signalSyncErrors(context, "CalDAV Sync Adapter", "calendar deleted: " + androidCalendar.getCalendarDisplayName());
				androidCalendar.deleteAndroidCalendar();
			}
		}

		return lcResult;
	}

	public void addCalendar(DavCalendar item)
	{
		item.setAccount(this.mAccount);
		item.setProvider(this.mProvider);
		item.ServerUrl = this.ServerUrl;
		this.mList.add(item);
	}

	public ArrayList<DavCalendar> getCalendarList()
	{
		return this.mList;
	}

	public void setAccount(Account account)
	{
		this.mAccount = account;
	}

	public void setProvider(ContentProviderClient provider)
	{
		this.mProvider = provider;
	}

	public ArrayList<Uri> getNotifyList()
	{
		ArrayList<Uri> lcResult = new ArrayList<Uri>();

		for (DavCalendar cal : this.mList)
		{
			lcResult.addAll(cal.getNotifyList());
		}

		return lcResult;
	}
}
