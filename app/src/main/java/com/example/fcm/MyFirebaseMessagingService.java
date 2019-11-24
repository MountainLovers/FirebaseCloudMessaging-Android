package com.example.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Scanner;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
	private static final String TAG = "FMS";
	public static final String FCM_PARAM = "picture";
	private static final String CHANNEL_NAME = "FCM";
	private static final String CHANNEL_DESC = "Firebase Cloud Messaging";
	private int numMessages = 0;

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);
		Log.d("FROM", remoteMessage.getFrom());

		// 如果是通知消息，就log并弹窗
		if (remoteMessage.getNotification() != null) {
			Log.d(TAG, "Message Notification Body: "+remoteMessage.getNotification().getBody());
			RemoteMessage.Notification notification = remoteMessage.getNotification();
			Map<String, String> data = remoteMessage.getData();
			sendNotification(notification, data);
		}

		// 如果是数据消息，就log并处理
		if (remoteMessage.getData().size() > 0) {
            Map<String, String> msg = remoteMessage.getData();
            Log.d(TAG, "Message Data: "+msg);
            String type = msg.get("type");
            Log.d(TAG, "Message Data Type: "+type);

            boolean handled = false;
            assert type != null;
            // 应该没啥用，理论上不可能收到token类别的消息。
            if (type.equals("token")) {
                String token = msg.get("token");
                Log.d(TAG, "Message Data Recv Token: "+token);
                ProtocolClass.clientToken = token;
                handled = true;
            }
            if (type.equals("ecpoint")) {
				JSONObject data = null;
				String x = null;
				String y = null;
				String description = null;
				try {
					data = new JSONObject(Objects.requireNonNull(msg.get("data")));
					x = (String) data.get("x");
					y = (String) data.get("y");
					description = (String) data.get("description");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				assert x != null;
				assert y != null;
				assert description != null;
				Log.d(TAG, "Message Data Recv ECPoint: x:"+x+" y: "+y+" desp: "+description);
				if (description.equals("alpha")) {
					ProtocolClass.alpha = ProtocolClass.retrivePoint(x, y);
					assert ProtocolClass.alpha != null;
					Log.d("DEBUG", "k: "+ProtocolClass.k.toString());
					ProtocolClass.beta = ProtocolClass.computeBeta(ProtocolClass.alpha, ProtocolClass.k).normalize();
					Log.d("DEBUG", "beta_x: "+ProtocolClass.beta.getAffineXCoord().toString());
					new Thread(new Runnable() {
						@Override
						public void run() {
							JSONObject jECPointPayload = new JSONObject();
							try {
								jECPointPayload.put("x", ProtocolClass.beta.getAffineXCoord().toString());
								jECPointPayload.put("y", ProtocolClass.beta.getAffineYCoord().toString());
								jECPointPayload.put("description", "beta");
							} catch (JSONException e) {
								e.printStackTrace();
							}
							Log.d("MSGTOCLIENT", "jECPointPayload"+jECPointPayload.toString());
							ProtocolClass.sendMsgToClient("ecpoint", jECPointPayload);
						}
					}).start();
				} else {
					throw new RuntimeException("Unknown desp in ecpoint type");
				}
				handled = true;
			}
            if (type.equals("requestK")) {
            	if (ProtocolClass.k == null) {
            		ProtocolClass.k = ProtocolClass.genK();
				}
            	Log.d(TAG, ProtocolClass.k.toString(16));
				new Thread(new Runnable() {
					@Override
					public void run() {
						Log.d("DEBUG", "In Here");
						JSONObject jKPayload = new JSONObject();
						try {
							jKPayload.put("k", ProtocolClass.k.toString(16));
						} catch (JSONException e) {
							e.printStackTrace();
						}
						Log.d("MSGTOCLIENT", "jKPayload"+jKPayload.toString());
						ProtocolClass.sendMsgToClient("responseK", jKPayload);
					}
				}).start();
            	handled = true;
			}
			if (!handled) {
				Log.d(TAG, "Message Data Type Error");
			}
		}
//		Log.d(TAG, "From: "+remoteMessage.getFrom());
		// Check if message contains a data payload.
//		if (remoteMessage.getData().size() > 0) {
//			Log.d(TAG, "Message data payload: "+remoteMessage.getData());
//		} else {
//			Log.d(TAG, "Message do not contain data payload");
//		}

	}

	private void sendNotification(RemoteMessage.Notification notification, Map<String, String> data) {
		Bundle bundle = new Bundle();
		bundle.putString(FCM_PARAM, data.get(FCM_PARAM));

		Intent intent = new Intent(this, SecondActivity.class);
		intent.putExtras(bundle);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
				.setContentTitle(notification.getTitle())
				.setContentText(notification.getBody())
				.setAutoCancel(true)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				//.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.win))
				.setContentIntent(pendingIntent)
				.setContentInfo("Hello")
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
				.setColor(getColor(R.color.colorAccent))
				.setLights(Color.RED, 1000, 300)
				.setDefaults(Notification.DEFAULT_VIBRATE)
				.setNumber(++numMessages)
				.setSmallIcon(R.drawable.ic_notification);

		try {
			String picture = data.get(FCM_PARAM);
			if (picture != null && !"".equals(picture)) {
				URL url = new URL(picture);
				Bitmap bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());
				notificationBuilder.setStyle(
						new NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setSummaryText(notification.getBody())
				);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					getString(R.string.notification_channel_id), CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
			);
			channel.setDescription(CHANNEL_DESC);
			channel.setShowBadge(true);
			channel.canShowBadge();
			channel.enableLights(true);
			channel.setLightColor(Color.RED);
			channel.enableVibration(true);
			channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

			assert notificationManager != null;
			notificationManager.createNotificationChannel(channel);
		}

		assert notificationManager != null;
		notificationManager.notify(0, notificationBuilder.build());
	}
}