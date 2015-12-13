package kr.ac.yonsei.ramo.w4u;

import android.graphics.Bitmap;

/**
 * Created by JW on 15. 5. 14..
 */
public class MyItem {
    Bitmap Thumbs;
    String Name;
    String Genre;
    String Path;
    String Num;

    MyItem(Bitmap aThumbs, String aName, String aGenre, String aPath, String aNum){
        Thumbs = aThumbs;
        Name = aName;
        Genre = aGenre;
        Path = aPath;
        Num = aNum;
    }

}
