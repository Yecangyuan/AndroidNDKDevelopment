package com.simley.ndk_day78.idcard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.simley.ndk_day78.R;

public class IDCardRecognitionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idcard_recognition);
        IDCardRecognition idCardRecognition = new IDCardRecognition();
        idCardRecognition.recognize();
    }
}