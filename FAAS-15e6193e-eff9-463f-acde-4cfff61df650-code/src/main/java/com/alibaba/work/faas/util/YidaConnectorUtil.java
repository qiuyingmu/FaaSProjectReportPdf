package com.alibaba.work.faas.util;


import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * 调用宜搭连接器
 * 调用宜搭平台接口
 * 更多钉钉官方连接器ID和动作ID及出入参请参照  https://www.yuque.com/yida/support/stbfik#Mv0dK (如果目标段落不存在, 请访问 https://www.yuque.com/yida/support/stbfik)
 *
 *@author bufan
 *@date 2022/2/11
 */
public class YidaConnectorUtil {
    public enum ConnectorTypeEnum {
        /**
         *自定义http连接器
         */
        HTTP("httpConnector"),
        /**
         *钉钉官方连接器
         */
        DING_INNER_CONNECTOR("innerConnector"),
        /**
         *钉钉三方连接器
         */
        DING_THIRD_CONNECTOR("thirdConnector");

        private String type;

        ConnectorTypeEnum(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class YidaResponse{
        private Object content;
        private String errorCode;
        private Object errorExtInfo;
        private String errorLevel;
        private String errorMsg;
        private boolean success;
        private String throwable;

        public Object getContent() {
            return content;
        }

        public void setContent(Object content) {
            this.content = content;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public Object getErrorExtInfo() {
            return errorExtInfo;
        }

        public void setErrorExtInfo(Object errorExtInfo) {
            this.errorExtInfo = errorExtInfo;
        }

        public String getErrorLevel() {
            return errorLevel;
        }

        public void setErrorLevel(String errorLevel) {
            this.errorLevel = errorLevel;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getThrowable() {
            return throwable;
        }

        public void setThrowable(String throwable) {
            this.throwable = throwable;
        }
    }
    /**
     *宜搭连接器消费码
     */
    private static String consumeCode;
    /**
     *宜搭域名, 部分企业可能是用的自定义域名
     */
    private static String yidaHost = "www.aliwork.com";

    private static final String SERVICE_PATH = "/query/publicService/invokeService.json";

    private static final String SERVICE_RETURN_VALUE = "serviceReturnValue";
    /**
     * 如果您的宜搭使用自定义域名, 则设置您的宜搭自定义域名
     *
     * @param host
     */
    public static void setCustomYidaHost(String host){
        yidaHost = host.trim();
    }

    /**
     * 设置宜搭服务消费码
     *
     * @param code
     */
    public static void setConsumeCode(String code){
        consumeCode = code;
    }

    public static String getConsumeCode(){
        return consumeCode;
    }

    private static String normalYidaHost(){
        if(Objects.isNull(yidaHost)){
            return null;
        }
        if (yidaHost.startsWith("http")){
            if (yidaHost.endsWith("/")){
                return yidaHost.substring(0,yidaHost.length()-1);
            }else {
                return yidaHost;
            }
        }else {
            if (yidaHost.endsWith("/")){
                return "https://"+yidaHost.substring(0,yidaHost.length()-1);
            }else {
                return "https://"+yidaHost;
            }
        }
    }

    /**
     * 调用宜搭连接器
     * 支持自定义http连接器(注意: Faas连接器实际上也可支持, 但为了避免您的误操作导致回环调用(A调B, B调A, 循环调), 因此枚举里没加Faas类型)
     * 支持钉钉官方连接器
     * 支持钉钉三方连接器
     *
     * @param connectorId
     * @param actionId
     * @param connectorTypeEnum
     * @param connection
     * @param inputs
     * @return
     */
    public static final YidaResponse invokeService(String connectorId,String actionId, ConnectorTypeEnum connectorTypeEnum, Long connection,JSONObject inputs){
        Map<String,String> headers = new HashMap<>(1,1);
        headers.put("Referer", "www.aliwork.com");
        Map<String,String> queries = new HashMap<>(1,1);
        queries.put("ConnectorConsumeCode",getConsumeCode());
        Map<String,String> bodies = new HashMap<>(2,1);
        bodies.put("inputs", JSON.toJSONString(inputs));

        JSONObject serviceInfo = new JSONObject();
        JSONObject connectorInfo = new JSONObject();
        serviceInfo.put("connectorInfo",connectorInfo);
        connectorInfo.put("connectorId",connectorId);
        connectorInfo.put("actionId",actionId);
        connectorInfo.put("type",connectorTypeEnum.getType());
        connectorInfo.put("connection",connection);

        bodies.put("serviceInfo", JSON.toJSONString(serviceInfo));
        try {
            HttpResponse response = HttpUtil.doPost(normalYidaHost(), SERVICE_PATH, headers,queries,bodies);
            if (response.getStatusLine().getStatusCode() != 200) {
                String message = "";
                try {
                    message =  EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                } catch (Exception e) {
                    message = message + e.getMessage();
                }
                throw new HttpException("status code not 200,but " + response.getStatusLine().getStatusCode() + message);
            } else {
                String resultJson = EntityUtils.toString(response.getEntity());
                return JSON.parseObject(resultJson,YidaResponse.class);
            }
        } catch (Exception e) {
            YidaResponse response = new YidaResponse();
            response.setSuccess(false);
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * post请求调用宜搭的宜搭平台接口
     * 宜搭平台接口参考 https://www.yuque.com/yida/support/aql605
     *
     * @param path 绝对路径, 以/开头, 以.json结尾, 例如/dingtalk/web/APP_XXXXXXXXYYYYYYY/query/compromiseDing/queryReleaseRecord.json
     * @param headers 请求头
     * @param queries 请求参数, 将会拼接到最终的请求url里
     * @param bodies 请求体键值对, 本方法采用的content-type为application/x-www-form-urlencoded
     * @return
     */
    public static final YidaResponse postYidaApi(String path,Map<String,String> headers, Map<String,String> queries, Map<String,String> bodies){
        try {
            if(Objects.isNull(queries)){
                queries = new HashMap<>(1,1);
            }
            if(Objects.isNull(bodies)){
                bodies = Collections.emptyMap();
            }  
            if(Objects.isNull(headers)){
                headers = Collections.emptyMap();
            }   
            //必须传, 否则无法访问宜搭.
            queries.put("ConnectorConsumeCode",getConsumeCode());

            HttpResponse response = HttpUtil.doPost(normalYidaHost(), path, headers,queries,bodies);
            if (response.getStatusLine().getStatusCode() != 200) {
                String message = "";
                try {
                    message =  EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                } catch (Exception e) {
                    message = message + e.getMessage();
                }
                throw new HttpException("status code not 200,but " + response.getStatusLine().getStatusCode() + message);
            } else {
                String resultJson = EntityUtils.toString(response.getEntity());
                return JSON.parseObject(resultJson,YidaResponse.class);
            }
        } catch (Exception e) {
            YidaResponse response = new YidaResponse();
            response.setSuccess(false);
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * get请求调用宜搭的宜搭平台接口
     * 宜搭平台接口参考: https://www.yuque.com/yida/support/aql605
     *
     * @param path 绝对路径, 以/开头, 以.json结尾, 例如/dingtalk/web/APP_XXXXXXXXYYYYYYY/query/compromiseDing/queryReleaseRecord.json
     * @param headers 请求头
     * @param queries 请求参数, 将会拼接到最终的请求url里
     * @return
     */
    public static final YidaResponse getYidaApi(String path,Map<String,String> headers, Map<String,String> queries){
        try {
            if(Objects.isNull(queries)){
                queries = new HashMap<>(1,1);
            }  
            if(Objects.isNull(headers)){
                headers = Collections.emptyMap();
            }  
            //必须传, 否则无法访问.
            queries.put("ConnectorConsumeCode",getConsumeCode());

            HttpResponse response = HttpUtil.doGet(normalYidaHost(), path, headers,queries);
            if (response.getStatusLine().getStatusCode() != 200) {
                String message = "";
                try {
                    message =  EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                } catch (Exception e) {
                    message = message + e.getMessage();
                }
                throw new HttpException("status code not 200,but " + response.getStatusLine().getStatusCode() + message);
            } else {
                String resultJson = EntityUtils.toString(response.getEntity());
                return JSON.parseObject(resultJson,YidaResponse.class);
            }
        } catch (Exception e) {
            YidaResponse response = new YidaResponse();
            response.setSuccess(false);
            response.setErrorMsg(e.getMessage());
            return response;
        }
    }

    /**
     * 从宜搭服务响应里获取宜搭连接器执行结果
     * 
     * @param yidaResponse 宜搭服务响应
     * @return
     * @throws Exception
     */
    public static Object extractYidaConnectorExecutionResult(YidaResponse yidaResponse) throws Exception {
        if (Objects.isNull(yidaResponse) || ! yidaResponse.isSuccess()){
            throw new Exception("yida response is null or fail");
        }
        if (yidaResponse.getContent() instanceof Map){
            return ((Map<String,Object>)yidaResponse.getContent()).get(SERVICE_RETURN_VALUE);
        }
        throw new Exception("content is not map");
    }
}
