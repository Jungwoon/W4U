package kr.ac.yonsei.ramo.w4u;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * DB관련해서 Table생성해주는 부분
 */

public class DBHelper extends SQLiteOpenHelper{

    private static final String TAG = "DBHelper";

    //Test 때문에 /mnt/sdcard/를 붙임
	public DBHelper(Context context){
        //super(context, "/mnt/sdcard/" + DATABASE_NAME, null, DATABASE_VERSION);
        super(context, Common.DATABASE_NAME, null, Common.DATABASE_VERSION);
	}

    //DB가 없으면 onCreate를 생성
	public void onCreate(SQLiteDatabase db){
        Log.d(TAG, "onCreate()");
		db.execSQL("CREATE TABLE " + Common.TABLE_NAME +" (" +
                        "num varchar(10), " +
                        "user_id varchar(20), " +
                        "contents_name varchar(30), " +
                        "contents_genre varchar(20), " +
                        "contents_path varchar(128), " +
                        "hit integer default 0, " +
                        "hit_time varchar(20), " +
                        "check_net varchar(2) " +
                        ");"
        );
	}

    //DB를 업그레이드할 때 호출된다. 기존 테이블을 삭제하고 새로 만들거나 ALTER TABLE로 스키마를 수정한다.
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d(TAG, "onUpgrade()");
		db.execSQL("DROP TABLE IF EXISTS " + Common.TABLE_NAME);
		onCreate(db);
	}
}
