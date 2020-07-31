package com.example.jetpackcourse.util;

import android.content.Context;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.jetpackcourse.R;
import butterknife.BindView;

public class Util {
    public static void loadImage(ImageView imageView, String url, CircularProgressDrawable progressDrawable){
        RequestOptions options = new RequestOptions()
                .placeholder(progressDrawable)
                .error(R.drawable.dog);
        Glide.with(imageView.getContext())
                .setDefaultRequestOptions(options)
                .load(url)
                .into(imageView);
    }
    public static CircularProgressDrawable getProgressDrawable(Context context){
        CircularProgressDrawable cpd = new CircularProgressDrawable(context);
        cpd.setStrokeWidth(10f);
        cpd.setCenterRadius(50f);
        cpd.start();
        return cpd;
    }

    @BindingAdapter("android:imageURL")
    public static void loadImage(ImageView view,String url){
        loadImage(view , url, getProgressDrawable(view.getContext()));
    }

}
