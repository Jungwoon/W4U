package kr.ac.yonsei.ramo.w4u;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by JW on 15. 5. 14..
 */
public class MyListAdapter extends BaseAdapter {
    Context maincon;
    LayoutInflater Inflater;
    ArrayList<MyItem> arSrc;
    int layout;

    public MyListAdapter(Context context, int alayout, ArrayList<MyItem> aarSrc){
        maincon = context;
        Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        arSrc = aarSrc;
        layout = alayout;
    }

    public int getCount(){
        return arSrc.size();
    }

    public String getItem(int position){
        return arSrc.get(position).Name;
    }

    public long getItemId(int position){
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent){

        final int pos = position;

        if(convertView == null){
            convertView = Inflater.inflate(layout, parent, false);
        }

        ImageView imgThumbs = (ImageView)convertView.findViewById(R.id.thumbImg);
        imgThumbs.setImageBitmap(arSrc.get(position).Thumbs);

        TextView txtName = (TextView)convertView.findViewById(R.id.txtTitle);
        txtName.setText(arSrc.get(position).Name);

        TextView txtGenre = (TextView)convertView.findViewById(R.id.txtGenre);
        txtGenre.setText(arSrc.get(position).Genre);

        return convertView;
    }
}
