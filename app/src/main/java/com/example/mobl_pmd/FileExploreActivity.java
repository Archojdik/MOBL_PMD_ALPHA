package com.example.mobl_pmd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileExploreActivity extends AppCompatActivity {
    public static final String FILE_EXTENSION_PASS_CODE = "file_extension";

    private List<String> allowedExtensions;

    private ArrayList<File> subdirs = new ArrayList<>();
    private ArrayList<File> files = new ArrayList<>();

    private File parentDir = null;
    private ArrayList<File> fileObjects = new ArrayList<>();

    private ListView filesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explore);

        filesListView = findViewById(R.id.file_list_view);
        filesListView.setOnItemClickListener((parent, view, position, id) -> {
            // Прочитайте описание адаптера
            File fileObject;
            if (position == 0)
                if (parentDir != null)
                    fileObject = parentDir;
                else
                    return;
            else
                fileObject = fileObjects.get(position - 1);

            if (fileObject.isDirectory()) {
                listFiles(fileObject);
            } else {
                Intent startPlayerIntent = new Intent(this, PlayerActivity.class);
                startPlayerIntent.putExtra(PlayerActivity.MUSIC_FILE_PATH_PASS_CODE, fileObject.getAbsolutePath());
                startActivity(startPlayerIntent);
            }
        });


        if (getIntent().hasExtra(FILE_EXTENSION_PASS_CODE)) {
            String[] extensions = getIntent().getStringArrayExtra(FILE_EXTENSION_PASS_CODE);
            allowedExtensions = Arrays.asList(extensions);
        }


        try {
            File externalDir = Environment.getExternalStorageDirectory();
            listFiles(externalDir);
        } catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    // Разбивает подпапки и файлы по полям subdirs и files
    private void separateDirsAndFiles(File directory) {
        subdirs.clear();
        files.clear();

        for (File f : directory.listFiles()) {
            if (f.isFile()) {
                // Добавляем только файлы с нужными расширениями
                String name = f.getName();
                int dot = name.lastIndexOf('.');
                String extension = "";
                if (dot != -1)
                    extension = name.substring(dot);
                if (allowedExtensions.contains(extension.toLowerCase()))
                    files.add(f);
            }
            else
                subdirs.add(f);
        }

        parentDir = directory.getParentFile();
        fileObjects.clear();
        fileObjects.addAll(subdirs);
        fileObjects.addAll(files);
    }
    private void listFiles(File directory) {
        if (!directory.canRead()) {
            Toast.makeText(this, "Can not read this directory.", Toast.LENGTH_SHORT).show();
            return;
        }

        separateDirsAndFiles(directory);

        FileAdapter adapter = new FileAdapter(fileObjects, this);
        filesListView.setAdapter(adapter);
    }



    /// В этом адаптаре элементом 0 всегда будет выступать директория "/.."
    private class FileAdapter extends BaseAdapter {
        private Context ctx;
        private ArrayList<File> fileObjects;
        private LayoutInflater layoutInflater;

        public FileAdapter(ArrayList<File> data, Context context) {
            fileObjects = data;
            ctx = context;
            layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public int getCount() {
            return fileObjects.size() + 1; // +"/.."
        }

        @Override
        public Object getItem(int position) {
            if (position == 0)
                return null;
            return fileObjects.get(position - 1); // -"/.."
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null)
                view = layoutInflater.inflate(R.layout.file_element, parent, false);

            // Под элементом 0 пусть всегда будет выступать "/.."
            if (position == 0)
            {
                view.findViewById(R.id.directory_block).setVisibility(View.VISIBLE);
                view.findViewById(R.id.file_block).setVisibility(View.GONE);

                ((TextView)view.findViewById(R.id.dir_name)).setText("/ ..");
                return view;
            }

            File file = fileObjects.get(position - 1);

            if (file.isDirectory()) {
                view.findViewById(R.id.directory_block).setVisibility(View.VISIBLE);
                view.findViewById(R.id.file_block).setVisibility(View.GONE);

                ((TextView)view.findViewById(R.id.dir_name)).setText("/ " + file.getName());
            } else {
                view.findViewById(R.id.directory_block).setVisibility(View.GONE);
                view.findViewById(R.id.file_block).setVisibility(View.VISIBLE);

                ((TextView)view.findViewById(R.id.file_name)).setText(file.getName());
            }

            return view;
        }
    }
}