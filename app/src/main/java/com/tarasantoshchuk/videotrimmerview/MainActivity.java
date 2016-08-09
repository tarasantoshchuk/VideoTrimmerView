package com.tarasantoshchuk.videotrimmerview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Trimmer mTrimmer;
    TextView mStart;
    TextView mFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
}
