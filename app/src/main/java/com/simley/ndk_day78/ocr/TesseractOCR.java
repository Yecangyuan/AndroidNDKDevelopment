package com.simley.ndk_day78.ocr;

import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class TesseractOCR {

    public String recognizeImage(Bitmap bitmap) {
        TessBaseAPI tess = new TessBaseAPI();

        String dataPath = new File(Environment.getExternalStorageDirectory(), "tesseract").getAbsolutePath();
        if (!tess.init(dataPath, "eng")) {
            tess.recycle();
            return "";
        }
        tess.setImage(bitmap);
        String text = tess.getUTF8Text();
        tess.recycle();
        return text;
    }


}
