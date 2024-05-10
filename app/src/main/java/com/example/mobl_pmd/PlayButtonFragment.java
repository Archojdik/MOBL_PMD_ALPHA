package com.example.mobl_pmd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class PlayButtonFragment extends Fragment {
    public static final int UPDATE_CIRCLE_ARG1 = 1;
    public static final int CIRCLE_ON_ARG1 = 2;
    public static final int CIRCLE_OFF_ARG1 = 3;


    private ImageView circleView;
    private ImageView iconView;

    private Bitmap[] circleImages;
    private int curCircleIndx = 1;

    private static ArrayList<PlayButtonFragment> instances = new ArrayList<>();

    public PlayButtonFragment() {
        super(R.layout.fragment_play_button);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        circleView = view.findViewById(R.id.circle_image);
        iconView = view.findViewById(R.id.play_button_image);

        try {
            circleImages = new Bitmap[9];
            circleImages[0] = BitmapFactory.decodeResource(getResources(), R.drawable.circle0);
            circleImages[1] = BitmapFactory.decodeResource(getResources(), R.drawable.circle1);
            circleImages[2] = BitmapFactory.decodeResource(getResources(), R.drawable.circle2);
            circleImages[3] = BitmapFactory.decodeResource(getResources(), R.drawable.circle3);
            circleImages[4] = BitmapFactory.decodeResource(getResources(), R.drawable.circle4);
            circleImages[5] = BitmapFactory.decodeResource(getResources(), R.drawable.circle5);
            circleImages[6] = BitmapFactory.decodeResource(getResources(), R.drawable.circle6);
            circleImages[7] = BitmapFactory.decodeResource(getResources(), R.drawable.circle7);
            circleImages[8] = BitmapFactory.decodeResource(getResources(), R.drawable.circle8);
        }
        catch (Exception ex) {
            Log.e("Fragment init", "Failed to initialize PlayButtonFragment: " + ex.getMessage());
        }

        view.setOnClickListener(v -> {
            updateCircle();
        });

        instances.add(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instances.remove(this);
    }

    public static void BroadcastMessage(Message msg) {
        for (PlayButtonFragment pbf : instances) {
            switch (msg.arg1) {
                case UPDATE_CIRCLE_ARG1:
                    pbf.updateCircle();
                    break;
                case CIRCLE_ON_ARG1:
                    pbf.setCircleActive(true);
                    break;
                case CIRCLE_OFF_ARG1:
                    pbf.setCircleActive(false);
                    break;
            }
        }
    }

    public void setCircleActive(boolean isActive) {
        if (isActive)
            circleView.setImageBitmap(circleImages[curCircleIndx]);
        else
            circleView.setImageBitmap(circleImages[0]);
    }
    public void updateCircle() {
        curCircleIndx++;
        if (curCircleIndx >= circleImages.length)
            curCircleIndx = 1;

        circleView.setImageBitmap(circleImages[curCircleIndx]);
    }
}