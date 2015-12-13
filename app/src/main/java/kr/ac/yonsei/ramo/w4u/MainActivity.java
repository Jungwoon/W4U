package kr.ac.yonsei.ramo.w4u;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;



/**
 * 'wifi'인지 확인 - done
 * 'wifi'이면 서버에 연결, 서버의 리스트를 가져옴
 * 서버에서 가져온 리스트와 내 리스트를 비교
 * 내 리스트에 없는 부분은 다운로드
 * 내 리스트 갱신(SQLite 이용)
 * 내가 가지고 있는 리스트를 이용해서 ListView에 썸네일과함께 표시
 * 봤는지 보지 않았는지 확인(봤으면 flag = 1 / 아니면 flag = 0)
 * 봤는지 보지 않았는지는 Click Event 발생시 flag = 1로 바꿔서 서버에 업데이트
 */

public class MainActivity extends ActionBarActivity {
    public static final String TAG = "MainActivity";
    ActivityManager activityManager;

    ArrayList<MyItem> arItem;
    MyItem mi;
    ListView MyList;

    private static DBHelper mHelper;
    static String loginId;

    public static boolean checkNet;

    ArrayList<ListItem> listItem= new ArrayList<ListItem>();
    String[][] fileArray = new String[10000][5];
    String[][] bFileArray = new String[10000][7];
    int lastContentsNumber;
    public int userNum;
    int cnt;
    fileGenerate thread;
    phpDown task;

    //Progress Dialog
    int mValue;
    boolean mQuit;
    ProgressDialog mProgress;


    @Override
    protected void onResume() {
        super.onResume();

        checkNet = false;

        //프로그램이 시작될때 마지막으로 가져왔던 테이블 넘버를 가져와서 lastContentsNumber에 넣어준다.
        SharedPreferences pref = getSharedPreferences("PREF_SEETING", MODE_MULTI_PROCESS);
        Boolean login = pref.getBoolean("LOGIN", false);
        Boolean isFirstTime = pref.getBoolean("FIRST", false); //맨처음 실행했을때만 다운로드 받게끔(Wifi이면)

        //일단은 Common에서 받아오는 방식으로 한다.
        loginId = pref.getString("USER_ID", "");
        Log.e(TAG, "Login ID : " + loginId);

        if(login == false){
            //아직 로그인을 한번도 않은 상태
            //ID를 입력할 수 있게끔 만든다
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(MainActivity.this, Login.class);
                    startActivity(i);
                }
            }, 250);
        }
        else{
            //로그인을 이미 한 상태
            //네트워크 문제로 hit데이터를 전송못한게 있는지 확인 하는 부분
            Toast.makeText(this, getNetworkStatus(this), Toast.LENGTH_LONG).show();
            checkNetwork(); //hit 기록이 있는지 확인하는 부분
            //setList();
            Log.e("This is Here", "You have to stay in here");

            try{
                if(getNetworkStatus(this) == "wifi"){
                    //네트워크 문제로 hit데이터를 전송못한게 있는지 확인 하는 부분
                    checkNetwork(); //hit 기록이 있는지 확인하는 부분

                    //Toast.makeText(this, "Wifi", Toast.LENGTH_LONG).show();

                    if(isFirstTime == false){
                        String param = "?user_id="+loginId;
                        //PHP서버로부터 데이터베이스를 불러오는 부분
                        task = new phpDown();
                        task.execute(Common.SERVER_IP+param);

                        SharedPreferences.Editor edit = pref.edit();
                        edit.putBoolean("FIRST", true);
                        edit.commit();
                    }else{
                        setList();
                    }

                }else{
                    //Toast.makeText(this, "Cell", Toast.LENGTH_LONG).show();
                    setList();
                }
            }catch(Exception e){
                e.printStackTrace();
                Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show();
                setList();
            }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arItem = new ArrayList<MyItem>();
        MyList = (ListView)findViewById(R.id.list);
        MyList.setOnItemClickListener(mItemClickListener);

        //시스템 내부 액티비티 상태파악을 위한 부분
        activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);

        startService(new Intent(getBaseContext(), ServiceChecker.class));

        Log.d(TAG, "Service Start");

        //isServiceRunning 내부적으로 "activityManager.getRunningsServices"를
        //이용하여 현재 실행중인 프로세스 리스트를 가져와 ServiceChecker가 실행되는지 확인
        if (isServiceRunning("kr.ac.yonsei.ramo.w4u", "kr.ac.yonsei.ramo.w4u.ServiceChecker") == true) {
            Log.d(TAG, "isServiceRunning is true");
        } else {
            Log.d(TAG, "isServiceRunning is false");
        }

    }

    //리스트를 눌렀을때 처리하는 부분
    AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener(){
        public void onItemClick(AdapterView parent, View view, int position, long id){
            String videoPath;
            String num;
            num = arItem.get(position).Num + "";
            videoPath = arItem.get(position).Path + "";

            mVideoDialog(videoPath);

            Log.e(TAG, "checkNet : " + checkNet);

            if(checkNet){
                //만약 네트워크가 연결이 안될 경우 클릭했던 리스트 번호를 저장하는 부분
                mHelper = new DBHelper(MainActivity.this);
                SQLiteDatabase db = mHelper.getWritableDatabase();
                Cursor cursor;


                //hit가 되어있는지 판단하기 위해서 해당 num에 해당하는 hit의 값을 가져오기 위한 쿼리
                String query = String.format("select hit from %s where num = '%s'", Common.TABLE_NAME, num);

                //쿼리를 실행하고 거기에 대한 결과를 cursor에 넣음
                cursor = db.rawQuery(query, null);

                while(cursor.moveToNext()){
                    Integer hit = cursor.getInt(0);

                    Log.e(TAG, "Click hit : " + hit);

                    //아직 hit가 0이면 아직 한번도 누르지 않은 상태
                    if(hit == 0){
                        Log.e(TAG, "in hit == 0");

                        //인터넷 연결이 안 된 상태에서 한 번도 hit 되지않은 아이템을 누르면 그 상태의 hit_time을 저장해두고, 나중에 다시 전송을 위해 check_net을 1로 표기해둔다.
                        //나중에 인터넷이 연결이 되면 check_net이 1인 아이템들의 시간을 가져와서 서버로 갱신해주기위해서
                        String updateQuery = String.format("update tb_contents_list set check_net = '1', hit_time = '%s' where num = '%s'", getTime(), num);
                        //만들어진 Query가 정상적인지 확인하는 부분
                        Log.e(TAG, "Unconnected_Check-UpdateQuery : " + updateQuery);
                        //쿼리 실행
                        db.execSQL(updateQuery);
                    }
                }

                //mHelper 닫기
                cursor.close();
                mHelper.close();

            } else {
                //인터넷이 연결이 잘 되어있을 경우
                mHelper = new DBHelper(MainActivity.this);
                SQLiteDatabase db = mHelper.getWritableDatabase();

                Cursor cursor;

                String query = String.format("select hit from %s where num = '%s'", Common.TABLE_NAME, num);

                //쿼리를 실행하고 거기에 대한 결과를 cursor에 넣음
                cursor = db.rawQuery(query, null);


                while(cursor.moveToNext()){

                    int hit = cursor.getInt(0);
                    Log.e(TAG, "hit : " + hit);

                    //한번 hit가 된 아이템은 다시 업데이트 하지 않기 위해서
                    if(hit == 0){
                        //hit를 잘 한 경우에도 내부 DB에도 hit='1'와 hit_time을 갱신해줌
                        String updateQuery = String.format("update tb_contents_list set hit = '1' where num = '%s'", num);

                        //만들어진 updateQuery가 정상적인지 확인하는 부분
                        Log.e(TAG, "Connected_Check_UpdateQuery : " + updateQuery);

                        //쿼리 실행
                        db.execSQL(updateQuery);

                        SendPost a = new SendPost();
                        a.execute(num, getTime());
                    }
                }

                cursor.close();
                mHelper.close();
            }

        }
    };

    public void setList(){
        Log.e("setList()", "setList start");

        //일단 기존의 리스트를 비운다.
        arItem.clear();
        mHelper = new DBHelper(MainActivity.this);

        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor;

        String query = "select num, contents_name, contents_genre, contents_path " +
                "from " + Common.TABLE_NAME;

        Log.e("setList", "getQuery() : " + query);

        //쿼리를 실행하고 거기에 대한 결과를 cursor에 넣음
        cursor = db.rawQuery(query, null);

        //데이터베이스로부터 읽어온 부분을 커스텀 리스트 뷰에 넣어줌
        while(cursor.moveToNext()) {

            String num = cursor.getString(0);
            String title = cursor.getString(1);
            String genre = cursor.getString(2);
            String path = cursor.getString(3);

            Bitmap img = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);

            mi = new MyItem(img, title, genre, path, num); arItem.add(mi);
            Log.e("setList", "Path : " + path);

        }
        //다쓴 cursor와 DBHelper는 닫아준다.
        cursor.close();
        mHelper.close();

        //List에 미리 만들어놓은 Adapter를 세팅한다.
        MyListAdapter MyAdapter = new MyListAdapter(MainActivity.this, R.layout.contentsmenu, arItem);
        MyList.setAdapter(MyAdapter);
    }

    //대화상자를 띄워서 동영상을 재생하는 부분
    public void mVideoDialog(String path){
        //다이얼로그에 사용할 layout을 인플레이팅 하는 부분
        RelativeLayout dialog = (RelativeLayout)View.inflate(MainActivity.this, R.layout.dialog, null);

        //해당 레이아웃의 다이얼로그로부터 아이디 찾아오기
        VideoView video = (VideoView)dialog.findViewById(R.id.videoplayer);
        video.setZOrderOnTop(true);

        Log.e("mVideoDialog", path);

        video.setVideoPath(path);
        video.start();


        new AlertDialog.Builder(this)
                .setView(dialog)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })
                .show();
    }

    //서버쪽 데이터베이스에 업데이트를 해주는 부분
    private class SendPost extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... str) {
            String content = executeClient(str[0], str[1]);
            return content;
        }

        protected void onPostExecute(String result) {
            // 모두 작업을 마치고 실행할 일 (메소드 등등)
        }

        // 실제 전송하는 부분
        public String executeClient(String num, String time) {
            String logTime = time;

            ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
            post.add(new BasicNameValuePair("num", num));
            post.add(new BasicNameValuePair("time", logTime));

            Log.e(TAG, "num : " + num);
            Log.e(TAG, "time : " + logTime);

            // 연결 HttpClient 객체 생성
            HttpClient client = new DefaultHttpClient();

            // 객체 연결 설정 부분, 연결 최대시간 등등
            HttpParams params = client.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 5000);

            // Post객체 생성
            HttpPost httpPost = new HttpPost(Common.UPDATE_SERVER_IP);

            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
                httpPost.setEntity(entity);
                client.execute(httpPost);
                return EntityUtils.getContentCharSet(entity);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    String getTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        String logTime = formatter.format(currentTime);

        return logTime;
    }

    void checkNetwork(){
        Log.e(TAG, "checkNetwork()");

        boolean chk = false;
        mHelper = new DBHelper(MainActivity.this);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor cursor;

        String query = String.format("select num, hit_time from %s where check_net = '1'", Common.TABLE_NAME);

        Log.e(TAG, "checkNetwork() query : " + query);

        //쿼리를 실행하고 거기에 대한 결과를 cursor에 넣음
        cursor = db.rawQuery(query, null);

        Log.e(TAG, "Cursor Count : " + cursor.getCount());

        while(cursor.moveToNext()){
            Log.e(TAG, "checkNetwork, in while()");
            chk = true;
            String num = cursor.getString(0);
            String hit_time = cursor.getString(1);

            String updateQuery = String.format("update tb_contents_list set check_net = '0', hit = '1' where num = '%s'", num);
            //만들어진 Query가 정상적인지 확인하는 부분e
            Log.e(TAG, "checkNetwork()_updateQuery : " + updateQuery);
            //쿼리 실행
            db.execSQL(updateQuery);

            SendPost a = new SendPost();
            a.execute(num, hit_time);

        }

        //mHelper 닫기
        cursor.close();
        mHelper.close();

        if(chk==true){
            Log.e(TAG, "checkNetwork() : " + "Exist");
        }else{
            Log.e(TAG, "checkNetwork() : " + "No Problem");
        }

    }

    //내부적으로 ServiceCheck Class가 돌아가는지 확인하는 부분
    private boolean isServiceRunning(String packageName, String className) {
        //현재 실행중인 Application Process List를 받아옴
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        boolean isServiceFound = false;

        for(int i = 0; i < services.size(); i++) {
            //List에 있는 packageName과 서비스 패키지 이름이 같으면 실행
            if(packageName.equals(services.get(i).service.getPackageName())) {
                //List에 있는 클래스 네임과 내부클래스 네임이 같으면 true
                if(className.equals(services.get(i).service.getClassName())) {
                    isServiceFound = true;
                }
            }
        }
        //현재 실행중인 Application Process List에서 패키지 이름과 클래스 이름이 같으면 true 아니면 false
        return isServiceFound;
    }

    //php문서 받아오는 부분
    private class phpDown extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.e(TAG, "doInBackground");

            StringBuilder jsonHtml = new StringBuilder();

            try{
                // 연결 url 설정
                URL url = new URL(urls[0]);

                // 커넥션 객체 생성
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                // 연결되었으면.
                if(conn != null){
                    conn.setConnectTimeout(10000);
                    conn.setUseCaches(false);

                    // 연결되었음 코드가 리턴되면.
                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                        for(;;){
                            // 웹상에 보여지는 텍스트를 라인단위로 읽어 저장.
                            String line = br.readLine();

                            if(line == null){
                                break;
                            }

                            // 저장된 텍스트 라인을 jsonHtml에 붙여넣음
                            jsonHtml.append(line + "\n");
                        }

                        br.close();
                    }
                    else{
                        Log.e("status", "connect to fail");
                    }
                    conn.disconnect();
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }

            return jsonHtml.toString();
        }

        protected void onPostExecute(String str){
            String num;
            String user_id;
            String contents_name;
            String contents_genre;
            String contents_path;
            String hit;
            String hit_time;

            cnt = 0;

            try{
                JSONObject root = new JSONObject(str);
                JSONArray ja = root.getJSONArray("results");
                cnt = ja.length(); //읽어온 데이터의 길이

                //여기서 JSON을 이용해서 파싱해준다.
                for(int i=0; i<ja.length(); i++){
                    JSONObject jo = ja.getJSONObject(i);
                    num = jo.getString("num");
                    user_id = jo.getString("user_id");
                    contents_name = jo.getString("contents_name");
                    contents_genre = jo.getString("contents_genre");
                    contents_path = jo.getString("contents_path");
                    hit = jo.getString("hit");
                    hit_time = jo.getString("hit_time");

                    listItem.add(new ListItem(num, user_id, contents_name, contents_genre, contents_path, hit, hit_time));

                    Log.d(TAG, "-------------------------");
                    Log.e("t_num : ", num);
                    Log.e("t_user_id : ", user_id);
                    Log.e("t_contents_name : ", contents_name);
                    Log.e("t_contents_genre : ", contents_genre);
                    Log.e("t_contents_path : ", contents_path);
                    Log.e("t_hit : ", hit);
                    Log.e("t_hit_time : ", hit_time);
                    Log.d(TAG, "-------------------------");

                    bFileArray[i][0] = num;
                    bFileArray[i][1] = user_id;
                    bFileArray[i][2] = contents_name;
                    bFileArray[i][3] = contents_genre;
                    bFileArray[i][4] = contents_path;
                    bFileArray[i][5] = hit;
                    bFileArray[i][6] = hit_time;
                }

            }catch(JSONException e){
                e.printStackTrace();
            }

            try{
                SharedPreferences pref = getSharedPreferences("PREF_SEETING", MODE_MULTI_PROCESS);
                lastContentsNumber = pref.getInt("LAST_CONTENTS_NUMBER", 0);
                loginId = pref.getString("USER_ID", "");

                Log.e("Initialize Cnt : ", cnt+"");
                //선언은 위에서 해주지만 생성은 여기서 가변적으로 해준다.
                mHelper = new DBHelper(MainActivity.this);

                Log.d(TAG, "-------------------------");
                Log.e(TAG, "cnt : " + cnt);
                Log.e(TAG, "loginId : " + loginId);
                Log.e(TAG, "lastContentsNumber : " + lastContentsNumber);
                Log.d(TAG, "-------------------------");
                userNum = 0;

                int checkLastNumber = lastContentsNumber;

                //DB에서 받아온 수를 가져온다.
                for(int i=0; i<cnt; i++){
                    //테스트용 로그를 찍기 위해서 String을 만들어주는 부분
                    Log.d(TAG, "i : "+ i);
                    Log.d(TAG, "num : "+ bFileArray[i][0]);
                    Log.d(TAG, "user_id : "+ bFileArray[i][1]);
                    Log.d(TAG, "contents_name : "+ bFileArray[i][2]);
                    Log.d(TAG, "contents_genre : "+ bFileArray[i][3]);
                    Log.d(TAG, "contents_path : "+ bFileArray[i][4]);
                    Log.d(TAG, "hit : "+ bFileArray[i][5]);
                    Log.d(TAG, "hit_time : "+ bFileArray[i][6]);
                    Log.e(TAG, "Outer");

                    Log.e(TAG, "before");
                    //해당 유저에 해당하는 자료만 가져오는 부분
                    //비교를 해서 가져온 데이터가 해당 유저의 것인지 비교하는 부분
                    int currentNum = Integer.parseInt(bFileArray[i][0]);

                    Log.d(TAG, "-------------------------");
                    Log.e("currentNum : ", currentNum+"");
                    Log.e("lastContentsNumber : ", lastContentsNumber+"");
                    Log.d(TAG, "-------------------------");

                    if(bFileArray[i][1].equals(loginId) && currentNum > lastContentsNumber) {
                        Log.e(TAG, "Inner");
                        //파일을 저장해주기 위해서 저장해주는 부분
                        fileArray[userNum][0] = bFileArray[i][0]; //num
                        fileArray[userNum][1] = bFileArray[i][1]; //user_id
                        fileArray[userNum][2] = bFileArray[i][2]; //contents_name
                        fileArray[userNum][3] = bFileArray[i][3]; //contents_genre
                        fileArray[userNum][4] = bFileArray[i][4]; //contents_path

                        Log.d(TAG, "-------------------------");
                        Log.e(TAG, "num : "+ bFileArray[i][0]);
                        Log.e(TAG, "user_id : "+ bFileArray[i][1]);
                        Log.e(TAG, "contents_name : "+ bFileArray[i][2]);
                        Log.e(TAG, "contents_genre : "+ bFileArray[i][3]);
                        Log.e(TAG, "contents_path : "+ bFileArray[i][4]);
                        Log.e(TAG, "hit : "+ bFileArray[i][5]);
                        Log.e(TAG, "hit_time : "+ bFileArray[i][6]);
                        Log.e(TAG, "TESTTEST");
                        Log.d(TAG, "-------------------------");

                        //수정가능한 부분
                        if(Integer.parseInt(bFileArray[i][0]) > checkLastNumber){
                            checkLastNumber = Integer.parseInt(bFileArray[i][0]);
                            Log.e("CheckLastNumber() : ", checkLastNumber+"");
                        }

                        //값을 넣어줬으니 마지막에 증가시켜준다.
                        userNum++;
                        Log.e("userNum : ", userNum+"");
                    }
                }

                //마지막 num을 저장해 놓는다.
                SharedPreferences.Editor editLogtime = pref.edit();
                editLogtime.putInt("LAST_CONTENTS_NUMBER", checkLastNumber);
                editLogtime.commit();

                Log.e(TAG, "LAST_CONTENTS_NUMBER : " + checkLastNumber);


                //해당 데이터베이스에 대해 업데이트 된 자료가 있으면 다운로드
                //없으면 그냥 바로 List를 만들어준다.
                if(userNum>0){
                    //새로운 업데이트 자료가 있으면 업데이트 새로운 스레드를 만들어서 다운받아줌
                    thread = null;
                    thread = new fileGenerate();
                    thread.setDaemon(true);
                    thread.start();

                }
                else{
                    //MainActivity.setList();
                    //Log.e(TAG, "setList()");
                }

            } catch (Exception e){
                e.printStackTrace();
                //setList();
                //Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_LONG).show();
            }
        }
    }

    //버퍼를 이용해서 다운로드 받기
    private class fileGenerate extends Thread {
        @Override
        public void run() {
            try{
                Log.e("FileGenerate.Thread", "Generate Start");
                kHandler.sendEmptyMessage(0);
                int downloadedNum=0;

                //중간에 W4U라는 디렉토리에 다 모아버리기 위해서 디렉토리부터 생성하는 부분
                File dirs = new File(android.os.Environment.getExternalStorageDirectory().toString()+"/", "W4U");
                if(dirs.mkdirs()) Log.d(TAG, "Directory Created");

                for(int i=0; i<userNum; i++){
                    Log.d("for Test : ", "i : " + i);

                    //파일의 확장자를 추출해내는 부분
                    String extention = fileArray[i][4].substring(fileArray[i][4].length()-3, fileArray[i][4].length());

                    //이름과 확장자를 이용해서 저장할 파일 이름을 만드는 부분
                    String strFileName = fileArray[i][2] + "." + extention;

                    //파일을 만들어 주는 부분
                    File file = new File(dirs.getAbsolutePath(), strFileName);
                    URL rUrl = new URL(fileArray[i][4]);

                    //URLConnection을 이용해서 URL로부터 읽은 데이터를 위에서 만든 파일에 쓰는 부분
                    URLConnection ucon = rUrl.openConnection();
                    InputStream is = ucon.getInputStream();

                    BufferedInputStream bis = new BufferedInputStream(is);

                    ByteArrayBuffer baf = new ByteArrayBuffer(50);
                    int current = 0;

                    while((current = bis.read()) != -1){
                        baf.append((byte)current);
                    }

                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baf.toByteArray());
                    fos.close();

                    //저장할 파일 이름을 가져와야 하는 부분
                    String filePath = dirs.getAbsolutePath()+"/"+strFileName;

                    //여기서 데이터베이스에 저장함
                    SQLiteDatabase db = mHelper.getWritableDatabase();

                    //동영상과 이름을 같게 .png파일로 thumbs 파일을 만들어야 함
                    String query = String.format("INSERT INTO %s (num, user_id, contents_name, contents_genre, contents_path)" +
                                    " VALUES('%s', '%s', '%s', '%s', '%s');",
                            Common.TABLE_NAME, fileArray[i][0], fileArray[i][1], fileArray[i][2], fileArray[i][3], filePath);

                    //만들어진 Query가 정상적인지 확인하는 부분
                    Log.e(TAG, "Real Query : " + query);

                    //쿼리 실행
                    db.execSQL(query);

                    //다이얼로그에 보여주기 위한 부분 파일하나 다운로드 완료될때마다
                    //mValue를 바꿔서 진행사황을 보여준다.
                    downloadedNum++;
                    Log.e("downloadedNum : ", downloadedNum+"");

                    //백분율로 해서 처리
                    mValue = (downloadedNum * 100) / userNum;
                    Log.e("mValue : ", mValue+"");
                    Message msg = pHandler.obtainMessage();
                    msg.arg1 = mValue;
                    pHandler.sendMessage(msg);
                }

                //다 썼으니 닫아줌
                mHelper.close();
                mHandler.sendEmptyMessage(0);
                mQuit=true;
                Log.e("Download", "Complete");

            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    //Network 상태 받아오는 부분(wifi or cell)
    public String getNetworkStatus(Context context){
        Log.d(TAG, "getNetworkStatus()");
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo= cm.getActiveNetworkInfo();

        if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            return "wifi";
        }
        else if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
            return "cell";
        }
        else {
            return "unknown";
        }
    }

    Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            if(msg.what == 0){
                Toast.makeText(MainActivity.this, "Download Complete", Toast.LENGTH_LONG).show();

                //다운로드까지 한 다음 해당 자료를 가지고 출력할 리스트를 만든다.
                setList();
            }
        }
    };

    //************************************************************************************************
    //Progress Dialog 부분
    //************************************************************************************************

    //Progress Dialog를 띄워주기 위한 핸들러
    Handler kHandler = new Handler(){
        public void handleMessage(Message msg){
            if(msg.what == 0){
                mValue = 0;
                showDialog(0);
                mQuit = false;
            }
        }
    };

    protected Dialog onCreateDialog(int id){
        switch(id){
            case 0 :
                mProgress = new ProgressDialog(this);
                mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgress.setTitle("Downloading");
                mProgress.setMessage("Wait...");
                mProgress.setCancelable(false);

                return mProgress;
        }

        return null;
    }

    Handler pHandler = new Handler(){
        public void handleMessage(Message msg){
            mValue = msg.arg1;

            if(mValue < 100){
                mProgress.setProgress(mValue);
            }else{
                mQuit=true;
                dismissDialog(0);
            }

            //if(mQuit==true) dismissDialog(0);

        }
    };

}
