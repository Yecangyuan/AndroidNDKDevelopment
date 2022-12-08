package com.simley.ndk_day78.bandcard;

import android.graphics.Bitmap;

public class BankCardRecognition {

    /**
     * 银行卡卡号识别
     * @param bitmap
     */
    public native String cardOcr(Bitmap bitmap);

}
