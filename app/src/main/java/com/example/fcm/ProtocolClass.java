package com.example.fcm;

import android.util.Log;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;

public class ProtocolClass {

    private static final String TAG = "ProtocolClass";

    public static String deviceToken = null;
    public static String clientToken = null;

    public static ECCurve curve;

    public static ECPoint alpha = null;
    public static ECPoint beta = null;
    public static BigInteger k;


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
        return alpha.multiply(k);
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
}
