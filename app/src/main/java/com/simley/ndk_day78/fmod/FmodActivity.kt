package com.simley.ndk_day78.fmod

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simley.ndk_day78.R
import org.fmod.FMOD

class FmodActivity : AppCompatActivity() {
    private val path = "file:///android_asset/derry.mp3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fmod)
        // 初始化fmod
        FMOD.init(this)
        if (FMOD.checkInit()) {
            Log.e("TAG", "fmod 初始化成功")
        } else {
            Log.e("TAG", "fmod 初始化失败")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // 销毁fmod
        FMOD.close()
    }

    fun onFix(view: View) {
        when (view.id) {
            R.id.btn_normal -> voiceChange(MODE_NORMAL, path) // 真实开发中，必须子线程  JNI线程（很多坑）
            R.id.btn_luoli -> voiceChange(MODE_LUOLI, path)
            R.id.btn_dashu -> voiceChange(MODE_DASHU, path)
            R.id.btn_jingsong -> voiceChange(MODE_JINGSONG, path)
            R.id.btn_gaoguai -> voiceChange(MODE_GAOGUAI, path)
            R.id.btn_kongling -> voiceChange(MODE_KONGLING, path)
        }
    }

    // 给C++调用的函数
    // JNI 调用 Java函数的时候，忽略掉 私有、公开 等
    private fun playerEnd(msg: String) {
        Toast.makeText(this, "" + msg, Toast.LENGTH_SHORT).show()
    }

    private fun voiceChange(modeNormal: Int, path: String) {
        Thread { voiceChangeNative(modeNormal, path) }.start()
    }

    private external fun voiceChangeNative(modeNormal: Int, path: String)

    companion object {
        private const val MODE_NORMAL = 0 // 正常
        private const val MODE_LUOLI = 1 // 萝莉音色
        private const val MODE_DASHU = 2 // 大叔音色
        private const val MODE_JINGSONG = 3 // 惊悚音色
        private const val MODE_GAOGUAI = 4 //   搞怪音色
        private const val MODE_KONGLING = 5 //  空灵音色
    }
}