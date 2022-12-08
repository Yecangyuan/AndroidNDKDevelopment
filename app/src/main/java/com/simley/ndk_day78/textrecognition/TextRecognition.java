package com.simley.ndk_day78.textrecognition;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import org.opencv.core.Mat;

public class TextRecognition {

    public native Bitmap recognize(Bitmap input);
}
