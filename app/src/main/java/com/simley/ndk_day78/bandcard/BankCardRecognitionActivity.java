package com.simley.ndk_day78.bandcard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityBankCardRecognitionBinding;

public class BankCardRecognitionActivity extends AppCompatActivity {

    private ActivityBankCardRecognitionBinding binding;
    private Bitmap mCardBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBankCardRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mCardBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.card_n);
        binding.cardIv.setImageBitmap(mCardBitmap);
    }

    public void cardOcr(View view) {
        BankCardRecognition bankCardRecognition = new BankCardRecognition();
        String bankNumber = bankCardRecognition.cardOcr(mCardBitmap);
        binding.cardNumberTv.setText(bankNumber);
    }
}