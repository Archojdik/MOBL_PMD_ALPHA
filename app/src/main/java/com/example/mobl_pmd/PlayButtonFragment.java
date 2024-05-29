package com.example.mobl_pmd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import javax.sound.midi.InvalidMidiDataException;

public class PlayButtonFragment extends Fragment {
    private ImageView circleView;
    private ImageView iconView;

    private Bitmap[] circleImages;
    private Bitmap playImage;
    private Bitmap pauseImage;
    private int curCircleIndx = 1;

    private PlayButtonHandler handler;

    public PlayButtonFragment() {
        super(R.layout.fragment_play_button);

        handler = new PlayButtonHandler();
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

            playImage = BitmapFactory.decodeResource(getResources(), R.drawable.play);
            pauseImage = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
        }
        catch (Exception ex) {
            Log.e("Fragment init", "Failed to initialize PlayButtonFragment: " + ex.getMessage());
        }

        view.setOnClickListener(v -> {
            try {
                if (MidiPlayerFast.getIsPlaying())
                    MidiPlayerFast.pause();
                else
                    MidiPlayerFast.play();
            }
            catch (InvalidMidiDataException | NotInitializedException ignored) {}
        });

        MidiPlayerFast.VisualizingHandler.setPlayButtonHandler(handler);
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
    public void setPlaying() {
        iconView.setImageBitmap(pauseImage);
    }
    public void setPaused() {
        iconView.setImageBitmap(playImage);
    }



    public class PlayButtonHandler extends Handler {
        public static final int UPDATE_CIRCLE_ARG1 = 1;
        public static final int CIRCLE_ON_ARG1 = 2;
        public static final int CIRCLE_OFF_ARG1 = 3;
        public static final int SET_PLAYING_ARG1 = 4;
        public static final int SET_PAUSED_ARG1 = 5;

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.arg1) {
                case UPDATE_CIRCLE_ARG1:
                    updateCircle();
                    break;
                case CIRCLE_ON_ARG1:
                    setCircleActive(true);
                    break;
                case CIRCLE_OFF_ARG1:
                    setCircleActive(false);
                    break;
                case SET_PLAYING_ARG1:
                    setPlaying();
                    break;
                case SET_PAUSED_ARG1:
                    setPaused();
                    break;
            }
        }
    }
}