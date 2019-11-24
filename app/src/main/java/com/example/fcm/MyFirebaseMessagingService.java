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
import java.util.PriorityQueue;
import java.util.Scanner;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
	private static final String TAG = "FMS";
	public static final String FCM_PARAM = "picture";
	private static final String AUTH_KEY = "AAAAjGipi60:APA91bExp6Dw6I33EZ-noe8UgeL3I06m2dLyINsIS6C835DSfzS6R8dJroE0cL31JEyDeHncYsHI-tRkWykB0Ji7aZimiRfhxmj_J9jb8cGBQiiyCgiKDACfc750Ffzrz_tMlEzP6ife";
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
            if (type.equals("token")) {
                String token = msg.get("token");
                Log.d(TAG, "Message Data Recv Token: "+token);
                ProtocolClass.clientToken = token;
                handled = true;
            }
            if (type.equals("ecpoint")) {
            	String x = msg.get("x");
            	String y = msg.get("y");
            	String description = msg.get("description");
				Log.d(TAG, "Message Data Recv ECPoint: x:"+x+" y: "+y+" desp: "+description);
				if (description.equals("alpha")) {
					ProtocolClass.alpha = ProtocolClass.retrivePoint(x, y);
					assert ProtocolClass.alpha != null;
					ProtocolClass.beta = ProtocolClass.computeBeta(ProtocolClass.alpha, ProtocolClass.k);
					sendMsgToClient("ecpoint", x, y, "beta");
				} else {
					throw new RuntimeException("Unknown desp in ecpoint type");
				}
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

	private void sendMsgToClient(String type, String x, String y, String desp) {
		assert ProtocolClass.clientToken != null;
		JSONObject jPayload = new JSONObject();
		JSONObject jData = new JSONObject();
		try {
			switch(type) {
				case "tokens":
					assert ProtocolClass.deviceToken != null;
					jData.put("type", "token");
					jData.put("token", ProtocolClass.deviceToken);
					break;
				case "ecpoint":
					jData.put("type", "ecpoint");
					jData.put("x", x);
					jData.put("y", y);
					jData.put("description", desp);
					break;
				default:
					jData.put("type", "unknown");
			}

			jPayload.put("priority", "high");
			jPayload.put("data", jData);
			jPayload.put("to", ProtocolClass.clientToken);

			URL url = new URL("https://fcm.googleapis.com/fcm/send");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", AUTH_KEY);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			// Send FCM message content.
			OutputStream outputStream = conn.getOutputStream();
			outputStream.write(jPayload.toString().getBytes());

			// Read FCM response.
			InputStream inputStream = conn.getInputStream();
			final String resp = convertStreamToString(inputStream);

			Log.d(TAG, "send msg to client return value: "+resp);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	private String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next().replace(",", ",\n") : "";
	}
}