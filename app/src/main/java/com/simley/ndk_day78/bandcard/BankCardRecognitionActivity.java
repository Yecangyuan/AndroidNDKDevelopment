package com.simley.ndk_day78.bandcard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityBankCardRecognitionBinding;
import com.simley.ndk_day78.ocr.TesseractOCR;
import com.simley.ndk_day78.utils.FileUtil;

import java.io.File;

public class BankCardRecognitionActivity extends AppCompatActivity {

    private ActivityBankCardRecognitionBinding binding;
    private Bitmap mCardBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBankCardRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File("/sdcard/tesseract/tessdata");
                if (!file.exists()) {
                    file.mkdirs();
                }
                FileUtil.copyAssets2SDCard(BankCardRecognitionActivity.this,
                        "eng.traineddata",
                        "/sdcard/tesseract/tessdata/eng.traineddata");
            }
        }).start();

        mCardBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.card_n);
        binding.cardIv.setImageBitmap(mCardBitmap);
    }

    public void cardOcr(View view) {
        // 1. 自己手写的文本识别
//        BankCardRecognition bankCardRecognition = new BankCardRecognition();
//        String bankNumber = bankCardRecognition.cardOcr(mCardBitmap);
//        binding.cardNumberTv.setText(bankNumber);

        // 2. 使用别人的文字识别库
        TesseractOCR tesseractOCR = new TesseractOCR();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.card_n);
        String bankCardNumber = tesseractOCR.recognizeImage(bitmap);
        binding.cardNumberTv.setText(bankCardNumber);
    }
}