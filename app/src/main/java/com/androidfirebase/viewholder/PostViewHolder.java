package com.androidfirebase.viewholder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.androidfirebase.Post;
import com.androidfirebase.R;
import com.androidfirebase.WelcomeActivity;

import java.io.InputStream;
import java.net.URL;

public class PostViewHolder extends RecyclerView.ViewHolder {
    public ImageView starView;
    private TextView authorView;
    private TextView bodyView;
    private TextView numStarsView;
    private TextView titleView;
    private ImageView user_image;

    public PostViewHolder(View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.post_title);
        authorView = itemView.findViewById(R.id.post_author);
        starView = itemView.findViewById(R.id.star);
        numStarsView = itemView.findViewById(R.id.post_num_stars);
        bodyView = itemView.findViewById(R.id.post_body);
        user_image = itemView.findViewById(R.id.user_image);
    }

    public void bindToPost(Post post, View.OnClickListener starClickListener) {
        titleView.setText(post.title);
        authorView.setText(post.author);
        numStarsView.setText(String.valueOf(post.starCount));
        bodyView.setText(post.body);
        if (post.userImage != null) {
            new DownloadImageTask().execute(post.userImage);
        }

        starView.setOnClickListener(starClickListener);
    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap mIcon = null;
            try {
                InputStream in = new URL(urls[0]).openStream();
                mIcon = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
              //  user_image.getLayoutParams().width = (getResources().getDisplayMetrics().widthPixels / 100) * 24;
                user_image.setImageBitmap(result);
            }
        }
    }
}