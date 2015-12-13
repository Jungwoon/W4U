package kr.ac.yonsei.ramo.w4u;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;


public class ServiceChecker extends Service {
	public static boolean serviceFlag;
    private static final String TAG = "ServiceChecker";
    Context mContext;
    SharedPreferences pref;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {

        Log.d(TAG, "serviceChecker_onCreate()");
        mContext = this;
		serviceFlag = true;

		new Thread(new Runnable() {
			public void run() {
                while (serviceFlag) {
                    try {
                        //전송하는 부분을 호출
                        ContentsDownloads temp = new ContentsDownloads(ServiceChecker.this);
                        Log.d(TAG, "run run run~~");
                        temp.downloadStart();

                        //20분 주기로 전송
                        Thread.sleep(Common.TRANSPORT_TIME);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
		}).start();

	}
}
