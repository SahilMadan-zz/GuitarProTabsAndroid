package com.sahil_madan.guitarprotabs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.sahil_madan.guitarprotabs.GPTab;
import com.sahil_madan.guitarprotabs.R;

import java.util.ArrayList;

/**
 * Created by sahil on 23/03/2015.
 */
public class GPTabAdapter extends ArrayAdapter<GPTab> {
    Context context;
    public GPTabAdapter(Context context, ArrayList<GPTab> tabs)
    {
        super(context, 0, tabs);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        GPTab tab = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gptab, parent, false);
        }

        TextView artist = (TextView) convertView.findViewById(R.id.gptabitem_artist);
        TextView song = (TextView) convertView.findViewById(R.id.gptabitem_song);
        TextView rating = (TextView) convertView.findViewById(R.id.gptabitem_rating);

        if (tab.rating == -1) {
            rating.setText(context.getString(R.string.tab_item_norating));
        } else {
            rating.setText("Average Rating: " + Integer.toString(tab.rating) + " (" + Integer.toString(tab.votes) + " Votes)");
        }
        Button download = (Button) convertView.findViewById(R.id.gptabitem_download);

        artist.setText(tab.artist);
        song.setText(tab.song);
        download.setTag(position);

        return convertView;
    }
}
