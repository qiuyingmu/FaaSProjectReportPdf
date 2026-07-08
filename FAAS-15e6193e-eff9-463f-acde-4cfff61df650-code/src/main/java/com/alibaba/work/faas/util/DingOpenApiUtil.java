package com.alibaba.work.faas.util;

import java.util.List;
import java.util.Objects;

import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkyida_1_0.models.BatchSaveFormDataHeaders;
import com.aliyun.dingtalkyida_1_0.models.BatchSaveFormDataRequest;
import com.aliyun.dingtalkyida_1_0.models.BatchSaveFormDataResponse;
import com.aliyun.dingtalkyida_1_0.models.GetOpenUrlHeaders;
import com.aliyun.dingtalkyida_1_0.models.GetOpenUrlRequest;
import com.aliyun.dingtalkyida_1_0.models.GetOpenUrlResponse;
import com.aliyun.dingtalkyida_1_0.models.SaveFormDataHeaders;
import com.aliyun.dingtalkyida_1_0.models.SaveFormDataRequest;
import com.aliyun.dingtalkyida_1_0.models.SaveFormDataResponse;
import com.aliyun.dingtalkyida_1_0.models.SearchFormDatasHeaders;
import com.aliyun.dingtalkyida_1_0.models.SearchFormDatasRequest;
import com.aliyun.dingtalkyida_1_0.models.SearchFormDatasResponse;
import com.aliyun.dingtalkyida_1_0.models.SearchFormDatasResponseBody;
import com.aliyun.dingtalkyida_1_0.models.StartInstanceHeaders;
import com.aliyun.dingtalkyida_1_0.models.StartInstanceRequest;
import com.aliyun.dingtalkyida_1_0.models.StartInstanceResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.apache.commons.lang3.StringUtils;

/**
 * 钉钉开放平台OpenAPI工具类
 *
 * @author bufan
 * @date 2022/2/11
 */
public class DingOpenApiUtil {
    private static String accessToken = "";
    private static String customAccessToken = "";

    public static final void setAccessToken(String token) {
        accessToken = token;
    }

    public static final String getAccessToken() {
        return accessToken;
    }

    public static final String getCustomAccessToken() {
        return customAccessToken;
    }

    private static final String innerGetAccessToken() {
        return StringUtils.isNotBlank(getCustomAccessToken()) ? getCustomAccessToken() : getAccessToken();
    }

    private static com.aliyun.dingtalkoauth2_1_0.Client accessTokenClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }

    public static String getCustomAccessTokenThenCache(String appKey, String appSecret) throws Exception {
        com.aliyun.dingtalkoauth2_1_0.Client client = accessTokenClient();
        GetAccessTokenRequest getAccessTokenRequest = new GetAccessTokenRequest().setAppKey(appKey)
                .setAppSecret(appSecret);
        try {
            GetAccessTokenResponse response = client.getAccessToken(getAccessTokenRequest);
            if (Objects.nonNull(response)) {
                String token = response.getBody().getAccessToken();
                /**
                 * 记录下来
                 */
                customAccessToken = token;
                return token;
            }
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw err;
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw _err;
        }
        return null;
    }

    /**
     * @return Client
     * @throws Exception
     */
    private static com.aliyun.dingtalkyida_1_0.Client createClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkyida_1_0.Client(config);
    }

    /**
     * 获取宜搭附件临时访问地址
     *
     * @param userId      用户的钉钉userId
     * @param appType     宜搭应用编码
     * @param systemToken 宜搭应用的秘钥
     * @param timeout     超时时间, 单位毫秒
     * @param url         宜搭附件地址, 形如:
     *                    /ossFileHandle?appType=APP_YT6S7YG54JVHDRZUDWSY&fileName=APP_YT6S7YG54JVHDRZUDWSY_MTczMjU1NjYyMzg3MzI0NF9VTzk2NkE3MUlJVVhZNEk4WjVNWjYyRTFLWUI1MktFQlBUSFpLMA$$.jpg&instId=&type=download
     * @return 超时时间为timeout毫秒的临时免登url
     * @throws Exception
     */
    public static String getOpenUrl(String userId, String appType, String systemToken, Long timeout, String url)
            throws Exception {
        com.aliyun.dingtalkyida_1_0.Client client = createClient();
        GetOpenUrlHeaders getOpenUrlHeaders = new GetOpenUrlHeaders();
        getOpenUrlHeaders.xAcsDingtalkAccessToken = innerGetAccessToken();
        GetOpenUrlRequest getOpenUrlRequest = new GetOpenUrlRequest();
        getOpenUrlRequest.setFileUrl(url);
        getOpenUrlRequest.setUserId(userId);
        getOpenUrlRequest.setSystemToken(systemToken);
        getOpenUrlRequest.setTimeout(timeout);
        try {
            GetOpenUrlResponse res = client.getOpenUrlWithOptions(appType, getOpenUrlRequest, getOpenUrlHeaders,
                    new RuntimeOptions());
            return res.getBody().getResult();
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw err;
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw _err;
        }
    }

    /**
     * 发起宜搭审批流程
     * https://open.dingtalk.com/document/orgapp-server/initiate-the-approval-process
     *
     * @param appType      宜搭应用编码
     * @param systemToken  宜搭应用秘钥
     * @param userId       钉钉用户id
     * @param language     语言
     * @param formUuid     表单唯一编码
     * @param formDataJson 表单实例数据json
     * @param processCode  流程编码
     * @param departmentId 部门id
     * @return
     * @throws Exception
     */
    public static String startProcessInstance(String appType, String systemToken, String userId, String language,
            String formUuid, String formDataJson, String processCode, String departmentId) throws Exception {
        com.aliyun.dingtalkyida_1_0.Client client = createClient();
        StartInstanceHeaders startInstanceHeaders = new StartInstanceHeaders();
        startInstanceHeaders.xAcsDingtalkAccessToken = innerGetAccessToken();
        StartInstanceRequest startInstanceRequest = new StartInstanceRequest().setAppType(appType)
                .setSystemToken(systemToken).setUserId(userId).setLanguage(language).setFormUuid(formUuid)
                .setFormDataJson(formDataJson).setProcessCode(processCode).setDepartmentId(departmentId);
        try {
            StartInstanceResponse response = client.startInstanceWithOptions(startInstanceRequest, startInstanceHeaders,
                    new RuntimeOptions());
            return response.getBody().getResult();
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw err;
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw _err;
        }
    }

    /**
     * 查询表单实例数据
     * https://open.dingtalk.com/document/orgapp-server/querying-form-instance-data
     *
     * @param searchFormDatasRequest 请求包装对象
     * @return 表单实例数据分页结果
     * @throws Exception
     */
    public static SearchFormDatasResponseBody searchFormData(SearchFormDatasRequest searchFormDatasRequest)
            throws Exception {
        com.aliyun.dingtalkyida_1_0.Client client = createClient();
        SearchFormDatasHeaders searchFormDatasHeaders = new SearchFormDatasHeaders();
        searchFormDatasHeaders.xAcsDingtalkAccessToken = innerGetAccessToken();
        try {
            SearchFormDatasResponse response = client.searchFormDatasWithOptions(searchFormDatasRequest,
                    searchFormDatasHeaders, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody();
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw err;
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw _err;
        }
    }

    /**
     * 保存表单数据 https://open.dingtalk.com/document/orgapp-server/save-form-data
     *
     * @param saveFormDataRequest
     * @return 表单实例ID
     * @throws Exception
     */
    public static String saveFormData(SaveFormDataRequest saveFormDataRequest) throws Exception {
        com.aliyun.dingtalkyida_1_0.Client client = createClient();
        SaveFormDataHeaders saveFormDataHeaders = new SaveFormDataHeaders();
        saveFormDataHeaders.xAcsDingtalkAccessToken = innerGetAccessToken();
        try {
            SaveFormDataResponse response = client.saveFormDataWithOptions(saveFormDataRequest, saveFormDataHeaders,
                    new RuntimeOptions());
            return response.getBody().getResult();
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw err;
        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                // err 中含有 code 和 message 属性，可帮助开发定位问题
            }
            throw _err;
        }
    }

    /**
     * 批量创建表单实例
     * 
     * @param request
     * @return 创建的多个表单实例的表单实例ID
     * @throws Exception
     */
    public static List<String> batchSaveFormData(BatchSaveFormDataRequest request) throws Exception {
        BatchSaveFormDataHeaders header = new BatchSaveFormDataHeaders();
        header.xAcsDingtalkAccessToken = innerGetAccessToken();
        return createClient().batchSaveFormDataWithOptions(request, header, new RuntimeOptions()).getBody().getResult();
    }
}
