package com.in4java.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * http工具类，依赖okhttp3
 * 废弃util中其他的httpUtil，统一到这个类来
 * @author kaka
 * @date 2022-8-4
 */
public class OkHttpUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpUtil.class);

    private static final int CONNECT_TIMEOUT = 5 * 1000;

    private static final int READ_TIMEOUT = 10 * 1000;

    private static final int WRITE_TIMEOUT = 10 * 1000;

    /**
     * okhttp连接池中整体的空闲连接的最大数量
     */
    private static final int MAX_IDL_CONNECTIONS = 10;
    /**
     * 最大连接空闲时时间,秒
     */
    private static final long KEEP_ALIVE_DURATION = 60L;

    public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

    public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";
    /**
     * 单服务共享一个client实例
     */
    private static final OkHttpClient CLIENT;

    static {
        CLIENT = okHttpConfigClient();
    }

    private static OkHttpClient okHttpConfigClient() {
        return new OkHttpClient().newBuilder()
                .connectionPool(pool())
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                //支持HTTPS请求，跳过证书验证
                .sslSocketFactory(createSSLSocketFactory(), (X509TrustManager) getTrustManagers()[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();
    }

    private static ConnectionPool pool() {
        return new ConnectionPool(MAX_IDL_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.SECONDS);
    }

    /**
     * 生成安全套接字工厂，用于https请求的证书跳过
     *
     * @return 返回安全套接字工厂
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, getTrustManagers(), new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            LOG.error("createSSLSocketFactory error", e);
        }
        return ssfFactory;
    }

    private static TrustManager[] getTrustManagers() {
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }



    /**
     * get请求（同步）
     * @param url 请求地址,不能为空
     * @return 返回get请求response，请求失败则返回null
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * get请求（同步）
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @return 返回get请求response，请求失败则返回null
     */
    public static String get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }

    /**
     * get请求（同步）
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @return 返回get请求response，请求失败则返回null
     */
    public static String get(String url, Map<String, Object> params, Map<String, String> header) {
        Request request = constructGetRequest(url, params, header);
        // 请求发送
        return doSyncRequest(request);
    }

    /**
     * get请求（同步）, 注意！！！返回的Response使用完后记得close
     * 返回{@link okhttp3.Response}
     * @param url 请求地址,不能为空
     * @return 返回get请求{@link okhttp3.Response}，请求失败则返回null
     */
    public static Response getRetResponse(String url) {
        return getRetResponse(url, null);
    }

    /**
     * get请求（同步）, 注意！！！返回的Response使用完后记得close
     * 返回{@link okhttp3.Response}
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @return 返回get请求{@link okhttp3.Response}，请求失败则返回null
     */
    public static Response getRetResponse(String url,Map<String, Object> params) {
        Request request = constructGetRequest(url, params, null);
        // 请求发送
        try {
            return CLIENT.newCall(request).execute();
        } catch (IOException e) {
            LOG.error("OkHttpUtil post error", e);
            return null;
        }
    }

    /**
     * get请求（同步）,获取文件/数据bytes
     * @param url 请求地址,不能为空
     * @return 返回get请求response（byte[]），请求失败则返回null
     */
    public static byte[] getBytes(String url) {
        return getBytes(url, null);
    }




    /**
     * get请求（同步）,获取文件/数据bytes
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @return 返回get请求response（byte[]），请求失败则返回null
     */
    public static byte[] getBytes(String url, Map<String, Object> params) {
        Request request = constructGetRequest(url, params, null);
        // 请求发送
        try (Response response = CLIENT.newCall(request).execute()){
            if (response.body() != null) {
                return response.body().bytes();
            }
        } catch (Exception e) {
            LOG.error("OkHttpUtil post error", e);
        }
        return null;
    }




    /**
     * post请求（同步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param params 请求参数，置于request body，不能为空
     * @param contentType 不可为空
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String postCommon(String url, Map<String, Object> params, String contentType, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        if (params == null) {
            throw new IllegalArgumentException("params不能为空");
        }
        JSONObject paramJson = new JSONObject();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            paramJson.put(key, params.get(key).toString());
        }
        return post(url, paramJson.toJSONString(), contentType, headers);
    }

    /**
     * form请求（同步）
     * MediaType 为 application/x-www-form-urlencoded
     * @param url 请求地址，不能为空
     * @param params 请求参数，置于request body，不能为空
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String form(String url, Map<String, Object> params, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        if (params == null) {
            throw new IllegalArgumentException("params不能为空");
        }
        FormBody.Builder builder = new FormBody.Builder();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            builder.add(key, params.get(key).toString());
        }
        RequestBody body = builder.build();

        Request.Builder requestBuilder = new Request.Builder();
        // 请求头
        if (headers!= null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        requestBuilder.addHeader("Content-Type", APPLICATION_FORM_URLENCODED_VALUE);
        Request request = requestBuilder
                .url(url)
                .post(body)
                .build();
        // 请求发送
        try (Response response = CLIENT.newCall(request).execute()){
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            LOG.error("OkHttpUtil post error", e);
        }
        return null;
    }

    /**
     * post请求（同步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param jsonData 请求体，放置在request body内，不能为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String post(String url, String jsonData) {
        return post(url, jsonData, null);
    }

    /**
     * post请求（同步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param jsonData 请求体，放置在request body内，不能为空; 格式为object, 常见一般为Map或者JSONObject
     * @return 返回post请求response，格式为JSONObject, 请求失败则返回null
     */
    public static JSONObject post(String url, Object jsonData) {
        return post(url, jsonData, null);
    }

    /**
     * post请求（同步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param jsonData 请求体，放置在request body内，不能为空; 格式为object, 常见一般为Map或者JSONObject
     * @param headers 请求头，可以为空
     * @return 返回post请求response，格式为JSONObject, 请求失败则返回null
     */
    public static JSONObject post(String url, Object jsonData, Map<String, String> headers) {
        return JSON.parseObject(post(url, JSON.toJSONString(jsonData), headers));
    }

    /**
     * post请求（同步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param jsonData 请求体，放置在request body内，不能为空
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String post(String url, String jsonData, Map<String, String> headers) {
        return post(url, jsonData, APPLICATION_JSON_UTF8_VALUE, headers);
    }

    /**
     * post请求（同步）
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String post(String url, String strData, String contentType, Map<String, String> headers) {
        Request request = constructPostRequest(url, strData, contentType, headers);
        // 请求发送
        return doSyncRequest(request);
    }

    /**
     * post请求（同步）
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param byteData 请求体，字节数组
     * @param contentType content-type 默认 application/octet-stream
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String post(String url, byte[] byteData, String contentType, Map<String, String> headers) {
        Request request = constructPostRequest(url, byteData, contentType, headers);
        // 请求发送
        return doSyncRequest(request);
    }

    /**
     * post请求（同步）, 注意！！！返回的Response使用完后记得close
     * MediaType 为 {@value APPLICATION_JSON_UTF8_VALUE}, 返回{@link okhttp3.Response}
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, application/json类型 不能为空
     * @return 返回post请求{@link okhttp3.Response}，请求失败则返回null
     */
    public static Response postRetResponse(String url, String strData) {
        return postRetResponse(url, strData, APPLICATION_JSON_UTF8_VALUE, null);
    }

    /**
     * post请求（同步）, 注意！！！返回的Response使用完后记得close
     * MediaType 为 contentType, 返回{@link okhttp3.Response}
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @return 返回post请求{@link okhttp3.Response}，请求失败则返回null
     */
    public static Response postRetResponse(String url, String strData, String contentType, Map<String, String> headers) {
        Request request = constructPostRequest(url, strData, contentType, headers);
        try {
            return CLIENT.newCall(request).execute();
        } catch (IOException e) {
            LOG.error("OkHttpUtil post error", e);
            return null;
        }
    }

    /**
     * post上传文件请求（同步）
     * MediaType 为 multipart/form-data
     * @param url 请求地址，不能为空
     * @param fileFormPart 文件上传formPart
     * @return 返回post请求response，请求失败则返回null
     */
    public static String postMultiPart(String url, FileFormPart fileFormPart) {
        return postMultiPart(url, fileFormPart, null);
    }

    /**
     * post上传文件请求（同步）
     * MediaType 为 multipart/form-data
     * @param url 请求地址，不能为空
     * @param fileFormPart 文件上传formPart
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static String postMultiPart(String url, FileFormPart fileFormPart, Map<String, String> headers) {
        Request request = constructMultiPartPostRequest(url, fileFormPart, headers);
        // 请求发送
        return doSyncRequest(request);
    }

    /**
     * get请求（异步）
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @param netCall 自定义网络回调实现
     */
    public static void getAsync(String url, Map<String, Object> params, final NetCall netCall) {
        // 构建get请求实体
        Request request = constructGetRequest(url, params, null);
        // 进行异步调用
        doAsyncRequest(request, netCall);
    }

    /**
     * post请求（异步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param netCall 自定义网络回调实现
     */
    public static void postAsync(String url, String strData, final NetCall netCall) {
        postAsync(url, strData,null, netCall);
    }

    /**
     * post请求（异步）
     * MediaType 为 application/json
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param headers 请求头，可以为空
     * @param netCall 自定义网络回调实现
     */
    public static void postAsync(String url, String strData, Map<String, String> headers, final NetCall netCall) {
        postAsync(url, strData, APPLICATION_JSON_UTF8_VALUE, headers, netCall);
    }

    /**
     * post请求（异步）
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @param netCall 自定义网络回调实现
     */
    public static void postAsync(String url, String strData, String contentType, Map<String, String> headers, final NetCall netCall) {
        // 构建请求
        Request request = constructPostRequest(url, strData, contentType, headers);
        // 进行异步调用
        doAsyncRequest(request, netCall);
    }

    /**
     * post请求（异步）
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param byteData 请求体，字节数组
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @return 返回post请求response，请求失败则返回null
     */
    public static void postAsync(String url, byte[] byteData, String contentType, Map<String, String> headers, final NetCall netCall) {
        Request request = constructPostRequest(url, byteData, contentType, headers);
        // 进行异步调用
        doAsyncRequest(request, netCall);
    }

    /**
     * post上传文件请求（异步）
     * MediaType 为 multipart/form-data
     * @param url 请求地址，不能为空
     * @param fileFormPart 文件上传formPart
     * @param netCall 自定义网络回调实现
     */
    public static void postMultiPartAsync(String url, FileFormPart fileFormPart, final NetCall netCall) {
        postMultiPartAsync(url, fileFormPart, null, netCall);
    }

    /**
     * post上传文件请求（异步）
     * MediaType 为 multipart/form-data
     * @param url 请求地址，不能为空
     * @param fileFormPart 文件上传formPart
     * @param headers 请求头，可以为空
     * @param netCall 自定义网络回调实现
     */
    public static void postMultiPartAsync(String url, FileFormPart fileFormPart, Map<String, String> headers, final NetCall netCall) {
        Request request = constructMultiPartPostRequest(url, fileFormPart, headers);
        // 进行异步调用
        doAsyncRequest(request, netCall);
    }

    /**
     * put请求（异步）
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @param netCall 自定义网络回调实现
     */
    public static void putAsync(String url, String strData, String contentType, Map<String, String> headers, final NetCall netCall) {
        // 构建请求
        Request request = constructPutRequest(url, strData, contentType, headers);
        // 进行异步调用
        doAsyncRequest(request, netCall);
    }


    /**
     * 构建get请求Request实体
     * @param url 请求地址,不能为空
     * @param params 请求参数，置于url后，可以为空
     * @param headersMap 请求头，可以为空
     * @return 返回构建好的get Request实体
     */
    private static Request constructGetRequest(String url, Map<String, Object> params, Map<String, String> headersMap) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        return buildGetRequest(url, params, headersMap);
    }

    /**
     * 通用构建post Request方法
     * @param url 请求地址，不能为空
     * @param params 请参数，可以为空
     * @param headersMap 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request buildGetRequest(String url, Map<String, Object> params, Map<String, String> headersMap) {
        // 请求构建
        if (params != null && !params.isEmpty()) {
            url = url + mapToUrl(params);
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();
        if (headersMap!= null && !headersMap.isEmpty()) {
            Headers headers = Headers.of(headersMap);
            requestBuilder.headers(headers);
        }
        return requestBuilder.build();
    }

    /**
     * 构建post请求Request实体
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request constructPostRequest(String url, String strData, String contentType, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        // 请求体
        RequestBody requestBody = constructRequestBody(strData, contentType);
        // 请求构建
        return buildPostRequest(url, requestBody, headers);
    }

    /**
     * 构建post请求Request实体
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param byteData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/octet-stream
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request constructPostRequest(String url, byte[] byteData, String contentType, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        // 请求体
        RequestBody requestBody = constructRequestBody(byteData, contentType);
        // 请求构建
        return buildPostRequest(url, requestBody, headers);
    }

    /**
     * 构建post请求Request实体
     * MediaType 为 multipart/form-data
     * @param url 请求地址，不能为空
     * @param fileFormPart 文件上传formPart
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request constructMultiPartPostRequest(String url, FileFormPart fileFormPart, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        // 构建multipartBody
        RequestBody multipartBody = constructMultipartBody(fileFormPart);
        return buildPostRequest(url, multipartBody, headers);
    }

    /**
     * 通用构建post Request方法
     * @param url 请求地址，不能为空
     * @param requestBody 请求体，放置于request body, 不能为空
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request buildPostRequest(String url, RequestBody requestBody, Map<String, String> headers) {
        // 请求构建
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);
        // 请求头
        if (headers!= null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder.build();
    }

    /**
     * 构建Multipart RequestBody
     * @param fileFormPart 文件上传formPart
     * @return 返回构建的RequestBody
     */
    private static RequestBody constructMultipartBody(FileFormPart fileFormPart) {
        if (fileFormPart == null || fileFormPart.getRequestBody() == null) {
            throw new IllegalArgumentException("fileFormPart不能为空");
        }
        return new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fileFormPart.getName(), fileFormPart.getFilename(), fileFormPart.getRequestBody())
                .build();
    }

    /**
     * 构建请求体，String输入
     * content-type 为 contentType
     * @param strData 请求体内容
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @return 返回构建的RequestBody
     */
    private static RequestBody constructRequestBody(String strData, String contentType) {
        if (strData == null) {
            throw new IllegalArgumentException("strData不能为空");
        }
        if (StringUtil.isEmpty(contentType)) {
            contentType = APPLICATION_JSON_UTF8_VALUE;
        }
        return RequestBody.create(MediaType.parse(contentType), strData);
    }

    /**
     * 构建请求体，String输入
     * content-type 为 contentType
     * @param byteData 请求体内容
     * @param contentType content-type 默认 application/octet-stream
     * @return 返回构建的RequestBody
     */
    private static RequestBody constructRequestBody(byte[] byteData, String contentType) {
        if (byteData == null) {
            throw new IllegalArgumentException("byteData不能为空");
        }
        if (StringUtil.isEmpty(contentType)) {
            contentType = APPLICATION_OCTET_STREAM_VALUE;
        }
        return RequestBody.create(MediaType.parse(contentType), byteData);
    }

    /**
     * 构建put请求Request实体
     * MediaType 为 contentType
     * @param url 请求地址，不能为空
     * @param strData 请求体，放置于request body, 不能为空
     * @param contentType content-type 默认 application/json;charset=UTF-8
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request constructPutRequest(String url, String strData, String contentType, Map<String, String> headers) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("url不能为空");
        }
        // 请求体
        RequestBody requestBody = constructRequestBody(strData, contentType);
        // 请求构建
        return buildPutRequest(url, requestBody, headers);
    }

    /**
     * 通用构建put Request方法
     * @param url 请求地址，不能为空
     * @param requestBody 请求体，放置于request body, 不能为空
     * @param headers 请求头，可以为空
     * @return 返回构建好的post Request实体
     */
    private static Request buildPutRequest(String url, RequestBody requestBody, Map<String, String> headers) {
        // 请求构建
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .put(requestBody);
        // 请求头
        if (headers!= null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder.build();
    }

    /**
     * 进行同步请求，并返回结果(String)
     * @param request 同步请求体
     * @return 返回请求response，请求失败则返回null
     */
    private static String doSyncRequest(Request request) {
        // 请求发送
        try (Response response = CLIENT.newCall(request).execute()){
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            LOG.error("OkHttpUtil post error", e);
        }
        return null;
    }

    /**
     * 进行异步请求，并触发回调逻辑
     * @param request 请求实体
     * @param netCall 自定义网络回调实现
     */
    private static void doAsyncRequest(Request request, final NetCall netCall) {
        // 将request封装为call
        Call call = CLIENT.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                netCall.failed(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                netCall.success(call, response);
            }
        });
    }

    /**
     * 自定义网络回调接口
     */
    public interface NetCall {
        /**
         * 异步请求成功逻辑
         * @param call Call实体
         * @param response 响应实体Response
         * @throws IOException 异常
         */
        void success(Call call, Response response) throws IOException;

        /**
         * 异步请求失败逻辑
         * @param call Call实体
         * @param e 异常
         */
        void failed(Call call, IOException e);
    }

    /**
     * 文件上传formPart,用于文件上传场景
     * 使用方式为new FileFormPart(name, filename, content).init()
     * 然后传入到具体的post方法使用
     * requestBody的默认mediaType为 {@value APPLICATION_OCTET_STREAM_VALUE}
     * @author kaka
     */
    @Getter
    public static class FileFormPart {

        private final String name;

        private final String filename;

        private final byte[] content;

        private MediaType mediaType;

        private RequestBody requestBody;

        public FileFormPart(String name, String filename, byte[] content, MediaType mediaType) {
            this(name, filename, content);
            this.mediaType = mediaType;
        }

        public FileFormPart(String name, String filename, byte[] content) {
            this.name = name;
            this.filename = filename;
            this.content = content;
        }

        public FileFormPart init() {
            if (mediaType == null) {
                mediaType = MediaType.parse(APPLICATION_OCTET_STREAM_VALUE);
            }
            requestBody = RequestBody.create(mediaType, content);
            return this;
        }
    }


    public static String mapToUrl(Map<String, Object> mapStr) {
        String url = "?1";
        String paramStr = mapAppendToUrl(mapStr);
        return url + paramStr;
    }
    public static String mapAppendToUrl(Map<String, Object> mapStr) {
        StringBuilder paramStr = new StringBuilder();
        if (mapStr == null) {
            return paramStr.toString();
        }
        for (Map.Entry<String, Object> entry : mapStr.entrySet()) {
            Object value = entry.getValue();
            if (value != null && !"".equals(value)) {
                paramStr.append("&").append(entry.getKey()).append("=").append(value);
            }
        }
        return paramStr.toString();
    }
}
