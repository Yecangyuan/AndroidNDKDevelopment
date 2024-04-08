package com.simley.lib_okhttp;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpUtils {

    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * GET 请求
     *
     * @param url
     * @param headerMap
     * @return
     * @throws Exception
     */
    public static String get(String url, Map<String, String> headerMap) throws Exception {
        Request.Builder requestBuilder = new Request.Builder().url(url);

        handleRequestHeader(requestBuilder, headerMap);
        return executeRequest(requestBuilder);
    }

    /**
     * POST JSON请求
     *
     * @param url
     * @param jsonStr
     * @param headerMap
     * @return
     * @throws Exception
     */
    public static String postJson(String url, String jsonStr, Map<String, String> headerMap) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=UTF-8");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, jsonStr));

        handleRequestHeader(requestBuilder, headerMap);
        return executeRequest(requestBuilder);
    }

    /**
     * 上传文件
     *
     * @param url
     * @param file
     * @param headerMap
     * @return
     * @throws Exception
     */
    public static String postFile(String url, File file, Map<String, String> headerMap) throws Exception {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        handleRequestHeader(requestBuilder, headerMap);
        return executeRequest(requestBuilder);
    }

    /**
     * PUT 方法
     *
     * @param url
     * @param jsonStr
     * @param headerMap
     * @return
     * @throws Exception
     */
    public static String putJson(String url, String jsonStr, Map<String, String> headerMap) throws Exception {
        MediaType mediaType = MediaType.parse("application/json; charset=UTF-8");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .put(RequestBody.create(mediaType, jsonStr));

        handleRequestHeader(requestBuilder, headerMap);
        return executeRequest(requestBuilder);
    }

    /**
     * DELETE 方法
     *
     * @param url
     * @param headerMap
     * @return
     * @throws Exception
     */
    public static String delete(String url, Map<String, String> headerMap) throws Exception {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete();

        handleRequestHeader(requestBuilder, headerMap);
        return executeRequest(requestBuilder);
    }


    /**
     * 真实执行请求
     *
     * @param requestBuilder
     * @return
     * @throws IOException
     */
    private static String executeRequest(Request.Builder requestBuilder) throws IOException {
        try (Response response = CLIENT.newCall(requestBuilder.build()).execute()) {
            if (null != response.body()) {
                return response.body().string();
            }
        }
        return null;
    }

    /**
     * 处理请求头
     *
     * @param requestBuilder
     * @param headerMap
     */
    private static void handleRequestHeader(Request.Builder requestBuilder, Map<String, String> headerMap) {
        if (null != headerMap && !headerMap.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                headerMap.forEach(requestBuilder::header);
            }
        }
    }

}
