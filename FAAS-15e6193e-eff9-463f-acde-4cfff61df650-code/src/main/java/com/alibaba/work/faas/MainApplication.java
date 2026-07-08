
package com.alibaba.work.faas;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.work.faas.common.FaasResponse;
import com.alibaba.work.faas.common.AbstractEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
/**
 * 请不要修改此类的代码!!!, 修改后可能导致Faas连接器将无法工作, 业务逻辑请写到FaasEntry.java
 * 
 * @Date 2023/09/10 11:16 AM
 * @Description 宜搭Faas连接器函数入口，FC handler：com.alibaba.work.faas.FaasEntry::handleRequest
 * @Version 1.0
 * @Author dankun
 * 
 **/
@SpringBootApplication
@RestController
public class MainApplication {
    /**
     *请不要删除!!!, 删除后Faas连接器将无法工作
     */
    private static final AbstractEntry FAAS_ENTRY = new FaasEntry();

    /**
     *请不要删除!!!, 删除后Faas连接器将无法工作
     */
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

    /**
     *请不要删除!!!, 删除后Faas连接器将无法工作
     */
	@PostMapping("/invoke")
	public String invoke(@RequestHeader Map<String, String> headers, @RequestBody(required = false) String payload) {
        //请不要修改faas连接器模板代码
        if (Objects.isNull(payload)) {
			return "payload is null";
		}
		JSONObject jsonObject = JSONObject.parseObject(payload);
		String action = jsonObject.getString("action");
		String triggerName = jsonObject.getString("triggerName");
		// 勿删！用作定时预热，解决Java冷启动问题
		if("heartbeat".equals(action) || "timer".equals(triggerName)) {
            FaasResponse res = new FaasResponse();
			JSONObject result = new JSONObject();
			result.put("success",true);
			System.out.println("heartbeat!");
			res.setBody(JSON.toJSONString(result));
			return JSON.toJSONString(res,SerializerFeature.DisableCircularReferenceDetect);
		}
        
        String requestId = headers.get("x-fc-request-id");
		System.out.printf("FC Invoke Start RequestId: %s%n", requestId);
		System.out.println(payload);
        /**
         * 真正执行用户的业务逻辑
         */
		FaasResponse response = FAAS_ENTRY.handleRequest(payload);
		response.getHeaders().put("responseHeader", "testValue");
		// Notice: 修改完代码后需要 new Terminal, 并执行./builder.sh自动构建和打包, 然后再点击部署
        System.out.printf("FC Invoke End RequestId: %s%n", requestId);
		return JSON.toJSONString(response,SerializerFeature.DisableCircularReferenceDetect);
	}
}
