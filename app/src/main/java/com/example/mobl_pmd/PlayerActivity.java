package com.example.mobl_pmd;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;

public class PlayerActivity extends AppCompatActivity {
    public static final String MUSIC_FILE_PATH_PASS_CODE = "music_file_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.play_btn_fragment_place, PlayButtonFragment.class, null)
                    .commit();
        }

        Resources rs = getResources();
        if (!InstrumentBank.getIsLoaded())
            InstrumentBank.load(rs.openRawResource(R.raw.instruments));

        try {
            MusicVisualizer musicVisualizer = new MusicVisualizer();
            MidiPlayerFast.init(musicVisualizer);
        }
        catch (Exception ex) {
            Log.e("Initialization failed", ex.getMessage(), ex);
        }

        //createNotification();


        if (getIntent().hasExtra(MUSIC_FILE_PATH_PASS_CODE)) {
            String musicFilePath = getIntent().getStringExtra(MUSIC_FILE_PATH_PASS_CODE);
            try {
                InputStream midiFile = new FileInputStream(musicFilePath);

                try {
                    if (MidiPlayerFast.getIsLoaded())
                        MidiPlayerFast.stop();
                    MidiPlayerFast.load(midiFile);
                    MidiPlayerFast.play();
                } catch (NotInitializedException ex) {
                    Log.e("Init error", "MidiPlayerFast is not initialized for some reason.");
                }

                midiFile.close();
            } catch (Exception ex) {
                Toast.makeText(this, "Exception: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onPlayClicked(View view) {
        try {
            MidiPlayerFast.play();
        } catch (NotInitializedException ex) {
            Log.e("Init error", "MidiPlayerFast is not initialized for some reason.");
        } catch (InvalidMidiDataException ex) {
            Log.e("Midi not loaded", "Midi file is not loaded. There is nothing to play.");
        }
    }
    public void onPauseClicked(View view) {
        try {
            MidiPlayerFast.pause();
        } catch (NotInitializedException ex) {
            Log.e("Init error", "MidiPlayerFast is not initialized for some reason.");
        } catch (InvalidMidiDataException ex) {
            Log.e("Midi not loaded", "Midi file is not loaded. There is nothing to pause.");
        }
    }
    public void onStopClicked(View view) {
        try {
            MidiPlayerFast.stop();
        } catch (NotInitializedException ex) {
            Log.e("Init error", "MidiPlayerFast is not initialized for some reason.");
        } catch (InvalidMidiDataException ex) {
            Log.e("Midi not loaded", "Midi file is not loaded. There is nothing to stop.");
        }
    }

    private void createNotification() {
        /*NotificationCompat.Builder musicNotBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.sak)
                .setContentTitle("MOBL_PMD")
                .setContentText("Вопроизведение...");

        Notification musicNot = musicNotBuilder.build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(52, musicNot);*/
        //NotificationChannel channel = new NotificationChannel("mobl_pmd_nch", "Mobile PMD notification channel", NotificationManager.IMPORTANCE_DEFAULT);
    }
}