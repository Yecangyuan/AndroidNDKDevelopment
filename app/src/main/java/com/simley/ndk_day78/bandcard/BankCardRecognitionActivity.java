package com.simley.ndk_day78.bandcard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

        // 检查并请求存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }

        File file = new File("tesseract/tessdata");
        if (!file.exists()) {
            file.mkdirs();
        }

        FileUtil.copyAssets2SDCard(BankCardRecognitionActivity.this,
                "eng.traineddata",
                "tesseract/tessdata/eng.traineddata");

        mCardBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.card_n);
        binding.cardIv.setImageBitmap(mCardBitmap);

        binding.btnCardOcr.setOnClickListener(this::cardOcr);

    }

    public void cardOcr(View view) {
        // 1. 自己手写的文本识别
//        BankCardRecognition bankCardRecognition = new BankCardRecognition();
//        String bankNumber = bankCardRecognition.cardOcr(mCardBitmap);
//        binding.cardNumberTv.setText(bankNumber);

        // 2. 使用别人的文字识别库
        TesseractOCR tesseractOCR = new TesseractOCR();

        // 将这个bitmap拷贝到/storage/emulated/0/tesseract目录下
        // FileUtil.copyRawFileToSDCard(mCardBitmap, "/tesseract", "card_n.jpg");

        String bankCardNumber = tesseractOCR.recognizeImage(mCardBitmap);
        binding.cardNumberTv.setText(bankCardNumber);
    }
}