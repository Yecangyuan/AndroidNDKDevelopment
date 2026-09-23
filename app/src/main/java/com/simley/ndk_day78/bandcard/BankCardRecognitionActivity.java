package com.simley.ndk_day78.bandcard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityBankCardRecognitionBinding;
import com.simley.ndk_day78.ocr.TesseractOCR;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankCardRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "BankCardRecognition";
    private static final String TEXT_RECOGNIZING = "识别中…";
    private static final String TEXT_NO_RESULT = "未识别到卡号";

    private ActivityBankCardRecognitionBinding binding;
    private Bitmap mCardBitmap;
    private TesseractOCR tesseractOCR;

    /** 训练数据的拷贝与识别都较重（首次约 32 MB），必须放到工作线程，否则会触发 ANR */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBankCardRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 训练数据位于应用私有目录，无需申请任何存储权限
        tesseractOCR = new TesseractOCR();

        mCardBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.card_n);
        binding.cardIv.setImageBitmap(mCardBitmap);

        binding.btnCardOcr.setOnClickListener(this::cardOcr);
    }

    public void cardOcr(View view) {
        // 1. 自己手写的文本识别：BankCardRecognition.cardOcr 目前是未实现的 native 方法
        //    （cpp 侧无 JNI 导出，调用会抛 UnsatisfiedLinkError），故暂不使用。

        // 2. 使用别人的文字识别库
        binding.btnCardOcr.setEnabled(false);
        binding.cardNumberTv.setText(TEXT_RECOGNIZING);

        executor.execute(() -> {
            final String recognized = tesseractOCR.recognizeImage(getApplicationContext(),
                    cropCardNumberArea(mCardBitmap));
            Log.d(TAG, "raw ocr result: " + recognized);
            // 卡号只保留数字：OCR 结果会夹带空格与换行
            final String result = recognized == null ? "" : recognized.replaceAll("[^0-9]", "");
            mainHandler.post(() -> {
                if (binding == null) {
                    return;
                }
                binding.cardNumberTv.setText(result.isEmpty() ? TEXT_NO_RESULT : result);
                binding.btnCardOcr.setEnabled(true);
            });
        });
    }

    /**
     * 裁剪出卡号所在的条带区域。
     * <p>
     * 采用与 native 侧 {@code co1::findCardNumberArea}（cardocr.cpp）相同的比例启发式：
     * x = cols/12、y = rows/2、w = cols*5/6、h = rows/4。
     * 整张卡面含行名、芯片、闪付与银联 Logo 等大量干扰，先裁剪再识别能显著提升准确率。
     */
    private static Bitmap cropCardNumberArea(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int x = Math.max(0, width / 12);
        int y = Math.max(0, height / 2);
        int w = Math.min(width * 5 / 6, width - x);
        int h = Math.min(height / 4, height - y);
        if (w <= 0 || h <= 0) {
            return src;
        }
        return Bitmap.createBitmap(src, x, y, w, h);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        binding = null;
    }
}
