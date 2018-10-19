package com.hoc.weatherapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

import static com.hoc.weatherapp.utils.NotificationUtilKt.ACTION_CANCEL_NOTIFICATION;
import static com.hoc.weatherapp.utils.NotificationUtilKt.WEATHER_NOTIFICATION_ID;
import static com.hoc.weatherapp.utils.NotificationUtilKt.cancelNotificationById;

public class CancelNotificationReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (Objects.equals(intent.getAction(), ACTION_CANCEL_NOTIFICATION)) {
      cancelNotificationById(context, WEATHER_NOTIFICATION_ID);
    }
  }
}
