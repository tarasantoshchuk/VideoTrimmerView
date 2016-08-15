package com.tarasantoshchuk.videotrimmerview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    Trimmer mTrimmer;
    TextView mStart;
    TextView mFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        File file = new File("/storage/emulated/0/video2.mp4");

        if(!file.exists()) {
            InputStream fileStream = getResources().openRawResource(R.raw.video);

            try {
                FileOutputStream fileOutput = new FileOutputStream(file);
                copyStream(fileStream, fileOutput);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTrimmer = (Trimmer) findViewById(R.id.trimmer);
        mStart = (TextView) findViewById(R.id.start);
        mFinish = (TextView) findViewById(R.id.finish);

        mTrimmer.setOnTrimChangedListener(new Trimmer.OnTrimChangedListener() {
            @Override
            public void onTrimChanged(float startTime, float endTime) {
                mStart.setText(String.valueOf(startTime));
                mFinish.setText(String.valueOf(endTime));
            }
        });


    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[512];
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}
