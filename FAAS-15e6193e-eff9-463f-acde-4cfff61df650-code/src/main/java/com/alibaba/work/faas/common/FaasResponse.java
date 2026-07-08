package com.alibaba.work.faas.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 请谨慎修改此处代码 响应内容，参考：https://help.aliyun.com/document_detail/54788.html
 *
 * @author ningzhong.wyl
 */
public class FaasResponse {
    private int statusCode;
    private Map<String, Object> headers = new HashMap<String, Object>();
    private Object body;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
