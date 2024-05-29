package com.example.mobl_pmd;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.InputStream;

public class PlayerActivity extends AppCompatActivity {
    public static final String MUSIC_FILE_PATH_PASS_CODE = "music_file_path";

    // Предполагается, что может существовать лишь одна копия этой активности.
    // @TODO: Паттерн Singleton
    private static int channelFragmentChNum = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // @TODO: иногда открытие активности сопровождается ужаааааасными лагами, в то время как музыка играет как надо...
        // Устанавливаем на активность различные фрагменты
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.play_btn_fragment_place, PlayButtonFragment.class, null)
                    .commit();

            channelFragmentChNum = 1;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm1_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 2;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm2_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 3;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm3_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 4;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm4_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 5;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm5_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 6;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm6_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 7;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm7_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
            channelFragmentChNum = 8;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fm8_fragment_frame, ChannelVisualFragment.class, null)
                    .commit();
        }

        Resources rs = getResources();
        if (!InstrumentBank.getIsLoaded())
            InstrumentBank.load(rs.openRawResource(R.raw.instruments));

        try {
            MidiPlayerFast.init();
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

    public static int getStaticChannelFragmentChNum() {
        return channelFragmentChNum;
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