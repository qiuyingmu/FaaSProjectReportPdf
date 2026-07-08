package com.alibaba.work.faas.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 请谨慎修改此处代码 请求入参封装, 宜搭侧会将相关上下文参数带过来
 *
 * @author bufan
 */
public class FaasInputs{
    /**
     *宜搭侧相关的上下文
     */
    private Map<String, Object> yidaContext = new HashMap<String, Object>();
    /**
     *faas连接器入参
     */
    private Map<String, Object> inputs = new HashMap<String, Object>();

    public Map<String, Object> getYidaContext() {
        return yidaContext;
    }

    public void setYidaContext(Map<String, Object> yidaContext) {
        this.yidaContext = yidaContext;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }
}
