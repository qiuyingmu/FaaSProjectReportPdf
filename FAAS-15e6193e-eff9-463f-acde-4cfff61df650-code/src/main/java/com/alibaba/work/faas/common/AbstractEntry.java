package com.alibaba.work.faas.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @Description 请谨慎修改此处代码
 * @Date 2023/09/21 11:16 AM
 * @Version 1.0
 * @Author bufan
 **/
public abstract class AbstractEntry {
    /**
     * (non-Javadoc)
     *
     * @see com.aliyun.fc.runtime.PojoRequestHandler#handleRequest(Object, com.aliyun.fc.runtime.Context)
     */

    public FaasResponse handleRequest(String inputs) {
        FaasInputs faasInputs = JSON.parseObject(inputs,FaasInputs.class);
        /**
         * 构建服务返回结果
         */
        JSONObject result = execute(faasInputs);

        // String responseBody = JSON.toJSONString(result,SerializerFeature.DisableCircularReferenceDetect);
    
        FaasResponse response = new FaasResponse();
        response.setBody(result);
        return response;
    }

    public abstract JSONObject execute(FaasInputs faasInputs);
}
