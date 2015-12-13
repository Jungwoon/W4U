package kr.ac.yonsei.ramo.w4u;

/**
 * Created by JW on 15. 5. 10..
 */
public class ListItem {

    private String[] mData;

    public ListItem(String[] data ){
        mData = data;
    }

    public ListItem(String num, String user_id, String contents_name, String contents_genre, String contents_path, String hit, String hit_time){

        mData = new String[7];
        mData[0] = num;
        mData[1] = user_id;
        mData[2] = contents_name;
        mData[3] = contents_genre;
        mData[4] = contents_path;
        mData[5] = hit;
        mData[6] = hit_time;
    }

    public String[] getData(){
        return mData;
    }

    public String getData(int index){
        return mData[index];
    }

    public void setData(String[] data){
        mData = data;
    }

}
