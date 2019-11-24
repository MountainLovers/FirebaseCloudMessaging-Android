package com.example.fcm;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Scanner;

public class ProtocolClass {

    private static final String TAG = "ProtocolClass";

    public static String deviceToken = null;
    public static String clientToken = null;

    public static ECCurve curve;

    public static ECPoint alpha = null;
    public static ECPoint beta = null;
    public static BigInteger k = null;

    private static final String AUTH_KEY = "key=AAAAjGipi60:APA91bExp6Dw6I33EZ-noe8UgeL3I06m2dLyINsIS6C835DSfzS6R8dJroE0cL31JEyDeHncYsHI-tRkWykB0Ji7aZimiRfhxmj_J9jb8cGBQiiyCgiKDACfc750Ffzrz_tMlEzP6ife";


    public static ECPoint retrivePoint(String x, String y) {
        assert curve != null;
        ECPoint point = curve.decodePoint(Hex.decode("04"+ x + y));
        Log.d(TAG, "RetrivePoint: "+point.toString());
        if (point.isValid()) {
            Log.d(TAG, "RetrivePoint: Valid Point!");
            return point;
        } else {
            Log.d(TAG, "RetrivePoint: Invalid Point!");
            return null;
        }
    }

    public static ECPoint computeBeta(ECPoint alpha, BigInteger k) {
        assert alpha != null;
        assert k != null;
        ECPoint beta = alpha.multiply(k);
        Log.d("DEBUG", alpha.toString());
        Log.d("DEBUG", beta.toString());
        return beta;
    }

    public static BigInteger genK() {
        BigInteger randk;
        SecureRandom random = new SecureRandom();
        BigInteger n = curve.getOrder();
        int nBitLength = n.bitLength();
        int watchDog = 1000;
        do {
            randk = new BigInteger(nBitLength, random);
            if (--watchDog == 0) {
                throw new RuntimeException("Can not generate k in 1000 times rounds");
            }
        } while (randk.equals(BigInteger.ZERO) || randk.compareTo(n) >= 0);
        Log.d(TAG, "Generate k:" + randk.toString(16));
        return randk;
    }

    public static void sendMsgToClient(String type, JSONObject payload) {
        assert ProtocolClass.clientToken != null;
        JSONObject jPayload = new JSONObject();
        JSONObject jData = new JSONObject();
        try {
            jData.put("type", type);
            jData.put("payload", payload);
//            switch(type) {
//                case "token":
//                    assert ProtocolClass.deviceToken != null;
//                    jData.put("type", "token");
//                    jData.put("token", ProtocolClass.deviceToken);
//                    break;
//                case "ecpoint":
//                    jData.put("type", "ecpoint");
//                    jData.put("x", x);
//                    jData.put("y", y);
//                    jData.put("description", desp);
//                    break;
//                default:
//                    jData.put("type", "unknown");
//            }

//            jPayload.put("priority", "high");
            jPayload.put("data", jData);
            jPayload.put("to", ProtocolClass.clientToken);

            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", AUTH_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            conn.connect();
            Log.d("NETWORK", jPayload.toString());

            // Send FCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jPayload.toString().getBytes());

            if (conn.getResponseCode() == 200) {
                Log.d("NETWORK", "POST SUCC: "+conn.getResponseMessage());
            } else {
                Log.d("NETWORK", "POST FAIL: "+conn.getResponseCode());
            }

            // Read FCM response.
//            InputStream inputStream = conn.getInputStream();
//            final String resp = convertStreamToString(inputStream);
//
//            Log.d(TAG, "send msg to client return value: "+resp);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next().replace(",", ",\n") : "";
    }
}
