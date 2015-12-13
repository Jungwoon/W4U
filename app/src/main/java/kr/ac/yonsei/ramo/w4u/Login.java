package kr.ac.yonsei.ramo.w4u;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class Login extends ActionBarActivity {
    EditText id, pw;
    Button btn;
    String responseString;
    SharedPreferences pref;
    Handler mHandler;

    static final String TAG = "Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mHandler = new Handler(new resultHandler());

        id = (EditText)findViewById(R.id.userId);
        btn = (Button)findViewById(R.id.btnLogin);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //Login 관련해서 요청하는 쓰레드 부분
                LoginThread thread = new LoginThread();
                thread.start();
            }
        });
    }

    class LoginThread extends Thread{
        @Override
        public void run(){
            try {
                SharedPreferences prefLogtime = getSharedPreferences("PREF_SEETING", MODE_MULTI_PROCESS);
                SharedPreferences.Editor editLogtime = prefLogtime.edit();
                editLogtime.putString("USER_ID", id.getText().toString());
                Log.e(TAG, "id : " + id.toString());

                editLogtime.commit();

                //로그인이 되면 로그인이 되었다고 핸들러를 호출
                mHandler.sendEmptyMessage(1);

                //맨 처음엔 다운로드 한번 해오는 부분
                //ContentsDownloads temp = new ContentsDownloads(Login.this);
                //temp.downloadStart();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    };


    class resultHandler implements Handler.Callback{
        @Override
        public boolean handleMessage(Message msg){

            if(msg.what == 1){
                Toast.makeText(Login.this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show();

                pref = getSharedPreferences("PREF_SEETING", 0);
                SharedPreferences.Editor edit = pref.edit();
                edit.putBoolean("LOGIN", true);
                edit.commit();

                finish();
            }
            return true;
        }
    }

}
