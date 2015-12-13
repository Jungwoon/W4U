package kr.ac.yonsei.ramo.w4u;

/**
 * Created by JW on 15. 5. 04..
 * 어플리케이션 전체적으로 사용하는 변수 및 값들을 모아놓은 곳
 */
public final class Common {

    //전송부분
    public final static String UPDATE_SERVER_IP = "http://byjungwoon.cafe24.com/update.php"; //real
    //public final static String SERVER_IP = "http://byjungwoon.cafe24.com/appdata.php"; //real
    public final static String SERVER_IP = "http://byjungwoon.cafe24.com/appdata_new.php"; //test

    public final static int PORT = 1088; //Test Server Port

    //SD카드 경로
    public static final String SDCARDPATH = android.os.Environment.getExternalStorageDirectory().toString();
    public static final String SDCARDSAVEDIR = SDCARDPATH + "/";

    //DB 부분
    public static final String DATABASE_NAME = "w4u.db";
    public static final String TABLE_NAME = "tb_contents_list";
    public static final int DATABASE_VERSION = 1;

    //시간(타이머)
    public static final long TRANSPORT_TIME = 1000*60*30; //정보 수집 간격 - 30분

}
