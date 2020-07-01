package org.gege.caldavsyncadapter.syncadapter.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import de.we.acaldav.R;

public class NotificationsHelper
{

	public static void signalSyncErrors(Context context, String title, String text)
	{
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.icon).setContentTitle(title).setContentText(text);

		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		int mId = 0;
		mNotificationManager.notify(mId, mBuilder.build());
	}
}
