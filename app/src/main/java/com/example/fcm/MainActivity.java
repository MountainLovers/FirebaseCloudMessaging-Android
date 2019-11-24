package com.example.fcm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fcm.zxing.android.CaptureActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.asn1.nist.NISTNamedCurves;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static com.example.fcm.R.id.txt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	private static final String AUTH_KEY = "AAAAjGipi60:APA91bExp6Dw6I33EZ-noe8UgeL3I06m2dLyINsIS6C835DSfzS6R8dJroE0cL31JEyDeHncYsHI-tRkWykB0Ji7aZimiRfhxmj_J9jb8cGBQiiyCgiKDACfc750Ffzrz_tMlEzP6ife";
	private TextView mTextView;
	private String token;
	private static final String DECODED_CONTENT_KEY = "codedContent";
	private static final String DECODED_BITMAP_KEY = "codedBitmap";
	private static final int REQUEST_CODE_SCAN = 0x0000;

	private Button btn_scan;
	private TextView tv_scanResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTextView = findViewById(txt);

		tv_scanResult = (TextView) findViewById(R.id.txt);
		btn_scan = (Button) findViewById(R.id.btn_scan);
		btn_scan.setOnClickListener(this);

		ProtocolClass.curve = NISTNamedCurves.getByName("P-256").getCurve();
		ProtocolClass.k = ProtocolClass.genK();

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String tmp = "";
			for (String key : bundle.keySet()) {
				Object value = bundle.get(key);
				tmp += key + ": " + value + "\n\n";
			}
			mTextView.setText(tmp);
		}

		FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
			@Override
			public void onComplete(@NonNull Task<InstanceIdResult> task) {
				if (!task.isSuccessful()) {
					token = task.getException().getMessage();
					Log.w("FCM TOKEN Failed", task.getException());
				} else {
					token = task.getResult().getToken();
					ProtocolClass.deviceToken = token;
					Log.i("FCM TOKEN", token);
				}
			}
		});
	}

	public void showToken(View view) {
		mTextView.setText(token);
	}

	public void subscribe(View view) {
		FirebaseMessaging.getInstance().subscribeToTopic("news");
		mTextView.setText(R.string.subscribed);
	}

	public void unsubscribe(View view) {
		FirebaseMessaging.getInstance().unsubscribeFromTopic("news");
		mTextView.setText(R.string.unsubscribed);
	}

	public void sendToken(View view) {
		sendWithOtherThread("token");
	}

	public void sendTokens(View view) {
		sendWithOtherThread("tokens");
	}

	public void sendTopic(View view) {
		sendWithOtherThread("topic");
	}

	private void sendWithOtherThread(final String type) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				pushNotification(type);
			}
		}).start();
	}

	private void pushNotification(String type) {
		JSONObject jPayload = new JSONObject();
		JSONObject jNotification = new JSONObject();
		JSONObject jData = new JSONObject();
		try {
			jNotification.put("title", "Google I/O 2016");
			jNotification.put("body", "Firebase Cloud Messaging (App)");
			jNotification.put("sound", "default");
			jNotification.put("badge", "1");
			jNotification.put("click_action", "OPEN_ACTIVITY_1");
			jNotification.put("icon", "ic_notification");

			jData.put("picture", "https://miro.medium.com/max/1400/1*QyVPcBbT_jENl8TGblk52w.png");

			switch(type) {
				case "tokens":
					JSONArray ja = new JSONArray();
					ja.put("c5pBXXsuCN0:APA91bH8nLMt084KpzMrmSWRS2SnKZudyNjtFVxLRG7VFEFk_RgOm-Q5EQr_oOcLbVcCjFH6vIXIyWhST1jdhR8WMatujccY5uy1TE0hkppW_TSnSBiUsH_tRReutEgsmIMmq8fexTmL");
					ja.put(token);
					jPayload.put("registration_ids", ja);
					break;
				case "topic":
					jPayload.put("to", "/topics/news");
					break;
				case "condition":
					jPayload.put("condition", "'sport' in topics || 'news' in topics");
					break;
				default:
					jPayload.put("to", token);
			}

			jPayload.put("priority", "high");
			jPayload.put("notification", jNotification);
			jPayload.put("data", jData);

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

			Handler h = new Handler(Looper.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					mTextView.setText(resp);
				}
			});
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	// Writen By ZhangSummary referring to https://firebase.google.com/docs/cloud-messaging/android/upstream. Send message. XMPP seems wrong...
	public void sendUpstream() {
		final String SENDER_ID = getResources().getString(R.string.gcm_defaultSenderId);
		final int messageID = 0;

		// [START fcm_send_upstream]
		FirebaseMessaging fm = FirebaseMessaging.getInstance();
		fm.send(new RemoteMessage.Builder(SENDER_ID + "@fcm.googleapis.com")
				.setMessageId(Integer.toString(messageID))
				.addData("my_message", "Hello World")
				.addData("my_action", "SAY_HELLO")
				.build());

		// [END fcm_send_upstream]
	}

	private String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next().replace(",", ",\n") : "";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_scan:
				//动态权限申请
				if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
				} else {
					goScan();
				}
				break;
			default:
				break;
		}
	}

	/**
	 * 跳转到扫码界面扫码
	 */
	private void goScan(){
		Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
		startActivityForResult(intent, REQUEST_CODE_SCAN);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case 1:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					goScan();
				} else {
					Toast.makeText(this, "你拒绝了权限申请，可能无法打开相机扫码哟！", Toast.LENGTH_SHORT).show();
				}
				break;
			default:
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// 扫描二维码/条码回传
		if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
			if (data != null) {
				//返回的文本内容
				String content = data.getStringExtra(DECODED_CONTENT_KEY);
				//返回的BitMap图像
				Bitmap bitmap = data.getParcelableExtra(DECODED_BITMAP_KEY);

				tv_scanResult.setText("你扫描到的内容是：" + content);

				ProtocolClass.clientToken = content;

				new Thread(new Runnable() {
					@Override
					public void run() {
						JSONObject jTokenPayload = new JSONObject();
						try {
							assert ProtocolClass.deviceToken != null;
							jTokenPayload.put("token", ProtocolClass.deviceToken);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						Log.d("MSGTOCLIENT", jTokenPayload.toString());
						ProtocolClass.sendMsgToClient("token", jTokenPayload);
					}
				}).start();
			}
		}
	}
}