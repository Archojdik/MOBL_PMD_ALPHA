package com.example.mobl_pmd;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSION = "Random Bullshit Go!!".hashCode();
    private final String[] MIDI_EXTENSIONS = new String[] {".mid"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.loadLibrary("fmgen");
    }

    private boolean requestPermissions() {
        // Добрый человек дал ответ. Проверить на живучесть.
        // https://stackoverflow.com/questions/32431723/read-external-storage-permission-for-android

        // Открываем настройки, если надо.
        // (У новых android есть новые разрешения для файлов)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)
                return true;
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                return true;
        }

        requestPermissionSettings();
        return false;
    }
    private void requestPermissionSettings() {
        // Открываем настройки разрешений приложения, чтобы пользователь сам нажал "разрешить"
        // (На моём телефоне по умолчанию наглухо запрещается использовать хранилище)
        Toast.makeText(this, "РАЗРЕШИ МНЕ ЧИТАТЬ ТВОЁ ХРАНИЛИЩЕ!!", Toast.LENGTH_SHORT).show(); // @TODO: Человеческую просьбу отдать телефон в наши руки
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
        ContextCompat.startActivity(this, intent, null);
    }

    public void onOpenMidiClicked(View view) {
        if (requestPermissions()) {
            Intent intent = new Intent(this, FileExploreActivity.class);
            intent.putExtra(FileExploreActivity.FILE_EXTENSION_PASS_CODE, MIDI_EXTENSIONS);
            startActivity(intent);
        }
    }

    public void onOpenPmdClicked(View view) {
        if (requestPermissions()) {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }
    }

    public void onOpenMmlClicked(View view) {

    }

    public void onOpenSettingsClicked(View view) {

    }

    public void onOpenAboutClicked(View view) {

    }
}