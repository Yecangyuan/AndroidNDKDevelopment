package com.drake.net.cookie

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 持久化存储Cookie
 * @param dbName 数据库名称, 设置多个名称可以让不同的客户端共享不同的cookies
 */
class PersistentCookieJar(
    val context: Context,
    val dbName: String = "net_cookies",
) : CookieJar {

    private var sqlHelper: SQLiteOpenHelper = object : SQLiteOpenHelper(context, dbName, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cookies (url TEXT, name TEXT, cookie TEXT, PRIMARY KEY(url, name))")
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL("DROP TABLE IF EXISTS cookies")
        }
    }

    /**
     * 添加Cookie到指定域名下
     */
    fun addAll(url: HttpUrl, cookies: List<Cookie>) {
        val db = sqlHelper.writableDatabase
        cookies.forEach { cookie ->
            if (cookie.expiresAt > System.currentTimeMillis()) {
                db.replace("cookies", null, ContentValues().apply {
                    put("url", url.host)
                    put("name", cookie.name)
                    put("cookie", cookie.toString())
                })
            }
        }
    }

    /**
     * 获取指定域名下的所有Cookie
     */
    fun getAll(url: HttpUrl): List<Cookie> {
        val db = sqlHelper.writableDatabase
        db.rawQuery("SELECT * FROM cookies WHERE url = ?", arrayOf(url.host)).use { cursor ->
            val cookies = mutableListOf<Cookie>()
            while (cursor.moveToNext()) {
                val cookieColumn = cursor.getString(cursor.getColumnIndex("cookie"))
                val cookie = Cookie.parse(url, cookieColumn)
                if (cookie != null) {
                    if (cookie.expiresAt > System.currentTimeMillis()) {
                        cookies.add(cookie)
                    } else {
                        db.delete("cookies", "url = ? AND name = ?", arrayOf(url.host, cookie.name))
                    }
                }
            }
            return cookies
        }
    }

    /**
     * 删除指定域名的所有Cookie
     */
    fun remove(url: HttpUrl) {
        sqlHelper.writableDatabase.delete("cookies", "url = ?", arrayOf(url.host))
    }

    /**
     * 删除指定域名下的指定cookie
     */
    fun remove(url: HttpUrl, cookieName: String) {
        sqlHelper.writableDatabase.delete("cookies", "url = ? AND name = ?", arrayOf(url.host, cookieName))
    }

    /**
     * 清除应用所有Cookie
     */
    fun clear() {
        sqlHelper.writableDatabase.delete("cookies", null, null)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return getAll(url)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        addAll(url, cookies)
    }
}