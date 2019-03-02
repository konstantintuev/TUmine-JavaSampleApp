package tuev.co.mineinbkg;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import tuev.co.tumine.NotificationGetter;

public class ProvideNotification extends NotificationGetter {
    @Override
    public Notification returnNotification(Context context) {
        String channelId = "miner";
        String channelName = "Mining Info";
        int channelImportance = NotificationManager.IMPORTANCE_LOW;

        //init Notification.Builder
        Notification.Builder notificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(context, channelId);
        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(context, 4521, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);

        //create the desired Notification
        Notification notification = notificationBuilder
                .setContentTitle("Mining XMR")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("This mines Monero without background kill protection.")
                .setContentIntent(contentIntent)
                .build();

        //there needs to be a 'Notification Channel' generate, else the app crashes on newer Androids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotifyManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannelRetr;
            notificationChannelRetr = mNotifyManager.getNotificationChannel(channelId);
            if (notificationChannelRetr == null) {
                NotificationChannel notificationChannel;


                notificationChannel = new NotificationChannel(channelId, channelName, channelImportance);
                notificationChannel.enableLights(false);
                notificationChannel.enableVibration(false);
                notificationChannel.setShowBadge(false);
                mNotifyManager.createNotificationChannel(notificationChannel);
            }
        }
        return notification;
    }
}
