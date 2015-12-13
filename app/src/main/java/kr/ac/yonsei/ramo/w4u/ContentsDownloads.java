package kr.ac.yonsei.ramo.w4u;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.util.ByteArrayBuffer;
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
import java.util.ArrayList;

/**
 * Created by JW on 15. 6. 7..
 */
public class ContentsDownloads {
    public final String TAG = "ContentsDownloads";
    fileGenerate thread;
    ArrayList<ListItem> listItem= new ArrayList<ListItem>();
    String[][] fileArray = new String[10000][5];
    String[][] bFileArray = new String[10000][7];
    public int userNum;
    int lastContentsNumber;
    int cnt;
    private static DBHelper mHelper;
    static String loginId;
    private static Context mContext;
    phpDown task;

    public ContentsDownloads(Context context) {
        mContext = context;
    }

    public void downloadStart(){
        try{
            if(getNetworkStatus(mContext) == "wifi"){
                Log.d(TAG, "wifi");

                SharedPreferences pref = mContext.getSharedPreferences("PREF_SEETING", mContext.MODE_MULTI_PROCESS);

                //일단은 Common에서 받아오는 방식으로 한다.
                loginId = pref.getString("USER_ID", "");

                String param = "?user_id="+loginId;
                task = new phpDown();
                task.execute(Common.SERVER_IP+param);
            }else if(getNetworkStatus(mContext) == "cell"){
                Log.d(TAG, "cell");
            }
        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG, "Network Error");
        }
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
                SharedPreferences pref = mContext.getSharedPreferences("PREF_SEETING", mContext.MODE_MULTI_PROCESS);
                lastContentsNumber = pref.getInt("LAST_CONTENTS_NUMBER", 0);
                loginId = pref.getString("USER_ID", "");

                Log.e("Initialize Cnt : ", cnt+"");
                //선언은 위에서 해주지만 생성은 여기서 가변적으로 해준다.
                mHelper = new DBHelper(mContext);

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
                }

                //다 썼으니 닫아줌
                mHelper.close();

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
}
