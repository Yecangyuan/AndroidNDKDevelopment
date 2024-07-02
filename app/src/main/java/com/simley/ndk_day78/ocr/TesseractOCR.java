package com.simley.ndk_day78.ocr;

import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TesseractOCR {

    public String recognizeImage(Bitmap bitmap) {
        TessBaseAPI tess = new TessBaseAPI();

        String dataPath = new File(Environment.getExternalStorageDirectory(), "tesseract").getAbsolutePath();
        Map<String, String> config = new HashMap<>();
        config.put(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        if (!tess.init(dataPath, "eng", TessBaseAPI.OEM_TESSERACT_ONLY, config)) {
            tess.recycle();
            return "";
        }
        tess.setImage(bitmap);
        String text = tess.getUTF8Text();
        tess.recycle();
        return text;
    }


}
