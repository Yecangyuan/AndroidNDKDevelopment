package com.simley.ndk_day78.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.simley.ndk_day78.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tesseract 文字识别封装。
 * <p>
 * 训练数据（tessdata/eng.traineddata）放在<b>应用私有目录</b>：
 * 既不需要任何存储权限，也不受 Android 10+ 分区存储（scoped storage）限制。
 * Tesseract 要求 dataPath 指向 tessdata 的<b>父目录</b>，即
 * {@code <filesDir>/tesseract/tessdata/eng.traineddata}。
 */
public class TesseractOCR {

    private static final String TAG = "TesseractOCR";

    /** 训练数据文件名，对应 assets/eng.traineddata */
    private static final String TRAINED_DATA = "eng.traineddata";
    /** Tesseract 数据目录名 */
    private static final String DATA_DIR = "tesseract";
    /** tessdata 子目录名，Tesseract 约定名称，不可更改 */
    private static final String TESSDATA_DIR = "tessdata";
    /** 识别语言 */
    private static final String LANGUAGE = "eng";

    /**
     * 确保训练数据就绪，返回 Tesseract 的 dataPath（其下需存在 tessdata/eng.traineddata）。
     *
     * @return 数据目录；准备失败时返回 null
     */
    public static File prepareDataDir(Context context) {
        File dataDir = new File(context.getFilesDir(), DATA_DIR);
        File trainedData = new File(new File(dataDir, TESSDATA_DIR), TRAINED_DATA);
        if (trainedData.exists()) {
            return dataDir;
        }
        try {
            // 首次运行把 assets 中的训练数据拷贝到私有目录（约 32 MB，耗时较长，勿在主线程调用）
            FileUtil.copyAsset(context, TRAINED_DATA, trainedData);
        } catch (IOException e) {
            Log.e(TAG, "准备训练数据失败: " + trainedData.getAbsolutePath(), e);
            return null;
        }
        return trainedData.exists() ? dataDir : null;
    }

    /**
     * 识别图片中的文字。
     * <p>
     * 参数经真机实测选定（见 README）：<br>
     * - {@code OEM_LSTM_ONLY}：LSTM 引擎对实拍照片的识别效果明显优于 legacy；<br>
     * - {@code PSM_RAW_LINE}：调用方已裁剪出卡号所在的单行条带，此时不应再做版面分析。
     * 实测用 PSM_SINGLE_LINE 会把整条带判为无效而返回空。
     * <p>
     * 注意：{@link TessBaseAPI#init} 在数据目录不存在时是<b>抛 IllegalArgumentException</b>
     * 而不是返回 false，因此这里必须捕获，否则会直接崩溃。
     *
     * @return 识别结果；失败时返回可展示的错误描述
     */
    public String recognizeImage(Context context, Bitmap bitmap) {
        return recognize(context, toGray(bitmap), TessBaseAPI.OEM_LSTM_ONLY,
                TessBaseAPI.PageSegMode.PSM_RAW_LINE);
    }

    /**
     * 灰度化。真机矩阵实测：卡面为绿色渐变底、数字压在渐变与高光上，
     * 灰度化后识别率明显优于直接喂彩色图（16/19 位 vs 14/19 位）。
     */
    private static Bitmap toGray(Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                0.299f, 0.587f, 0.114f, 0, 0,
                0.299f, 0.587f, 0.114f, 0, 0,
                0.299f, 0.587f, 0.114f, 0, 0,
                0, 0, 0, 1, 0})));
        new Canvas(out).drawBitmap(src, 0, 0, paint);
        return out;
    }

    private String recognize(Context context, Bitmap bitmap, int oem, int pageSegMode) {
        File dataDir = prepareDataDir(context);
        if (dataDir == null) {
            return "识别失败：训练数据不可用";
        }

        TessBaseAPI tess = new TessBaseAPI();
        boolean initialized = false;
        try {
            Map<String, String> config = new HashMap<>();
            // 银行卡号只包含数字，限定字符集可降低误识别
            config.put(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
            if (!tess.init(dataDir.getAbsolutePath(), LANGUAGE, oem, config)) {
                return "识别失败：Tesseract 初始化失败";
            }
            initialized = true;
            tess.setPageSegMode(pageSegMode);
            tess.setImage(bitmap);
            String text = tess.getUTF8Text();
            return text == null ? "" : text.trim();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Tesseract 数据目录无效: " + dataDir.getAbsolutePath(), e);
            return "识别失败：" + e.getMessage();
        } finally {
            // 仅在初始化成功时回收，避免对未初始化的实例调用 native 方法
            if (initialized) {
                tess.recycle();
            }
        }
    }
}
