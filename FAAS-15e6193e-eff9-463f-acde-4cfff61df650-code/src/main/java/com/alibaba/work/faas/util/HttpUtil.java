package com.alibaba.work.faas.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 *@author bufan
 *@date 2022/2/7
 */
public class HttpUtil {

    /**
     * get
     *
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @return
     * @throws Exception
     */
    public static HttpResponse doGet(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpGet request = new HttpGet(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    /**
     * post form
     *
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @param bodies
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries,
        Map<String, String> bodies)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpPost request = new HttpPost(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodies != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodies.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodies.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            request.setEntity(formEntity);
        }

        return httpClient.execute(request);
    }

    /**
     * Post String
     *
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries,
        String body)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpPost request = new HttpPost(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Post stream
     *
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries,
        byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpPost request = new HttpPost(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Put String
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries,
        String body)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpPut request = new HttpPut(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Put stream
     * @param schemaAndHost
     * @param path
     * @param method
     * @param headers
     * @param queries
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String schemaAndHost, String path, String method,
        Map<String, String> headers,
        Map<String, String> queries,
        byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpPut request = new HttpPut(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Delete
     *
     * @param schemaAndHost
     * @param path
     * @param headers
     * @param queries
     * @return
     * @throws Exception
     */
    public static HttpResponse doDelete(String schemaAndHost, String path,
        Map<String, String> headers,
        Map<String, String> queries)
        throws Exception {
        HttpClient httpClient = wrapClient(schemaAndHost);

        HttpDelete request = new HttpDelete(buildUrl(schemaAndHost, path, queries));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    private static String buildUrl(String schemaAndHost, String path, Map<String, String> queries) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(schemaAndHost);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != queries) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : queries.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    private static HttpClient wrapClient(String schemaAndHost) {
        HttpClient httpClient = new DefaultHttpClient();
        if (schemaAndHost.startsWith("https://")) {
            sslClient(httpClient);
        }

        return httpClient;
    }

    private static void sslClient(HttpClient httpClient) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }
                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
