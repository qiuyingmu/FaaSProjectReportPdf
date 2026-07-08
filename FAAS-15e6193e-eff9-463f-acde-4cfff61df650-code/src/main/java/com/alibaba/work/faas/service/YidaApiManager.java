package com.alibaba.work.faas.service;

import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkyida_2_0.Client;
import com.aliyun.dingtalkyida_2_0.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 钉钉新版 Yida API (dingtalkyida_2_0) Service
 *
 * <h3>设计模式：饿汉式单例 (Eager Singleton)</h3>
 * <pre>
 * 选择理由（针对 FaaS 连接器环境）：
 *
 *  1. 冷启动友好
 *     饿汉式在类加载阶段完成初始化，第一次请求到达时零额外延迟。
 *     FaaS 的冷启动是主要延迟瓶颈，懒加载反而会增加首次调用耗时。
 *
 *  2. 天然线程安全
 *     static final 实例由 JVM 保证初始化唯一性，无需 synchronized / volatile。
 *     FaaS 容器可能并发处理请求，饿汉式不会有竞争条件。
 *
 *  3. 构造轻量无副作用
 *     构造器仅做字符串赋值，不涉及网络 I/O 或文件读取，成本可忽略不计。
 *
 *  4. 代码简洁可靠
 *     无需双重检查锁定（DCL）或静态内部类等复杂机制，
 *     一行 static final 即可，可读性和维护性都更好。
 * </pre>
 *
 * <h3>凭证管理</h3>
 * <pre>
 * 敏感配置从 classpath:yida-secret.properties 加载（已加入 .gitignore）。
 * 如果文件不存在，则回退使用硬编码常量（DEFAULT_APP_KEY 等）。
 *
 * 新成员请复制 yida-secret.properties.template 为 yida-secret.properties 并填入真实值。
 * </pre>
 *
 * <h3>本地缓存策略（JVM 内存级）</h3>
 * <pre>
 * 采用 FaaS 容器本地缓存，原因：
 *
 *  1. FaaS 容器在无活动后会被冻结但不一定销毁，下次请求可能复用同一容器。
 *     本地缓存的 token 可以在容器生命周期内持续复用，减少 token 获取频次。
 *
 *  2. 不需要分布式缓存（Redis/DB）。
 *     钉钉 token 仅用于当前应用，FaaS 每个容器独立，不存在跨容器共享需求。
 *     如果有多个容器实例，各自独立缓存，最多增加一次 token 获取开销。
 *
 *  3. 缓存命中时零网络开销，7200秒有效期 + 300秒提前刷新，有效期内几乎每次都是缓存命中。
 * </pre>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *     // ===== 查询全部数据（自动翻页） =====
 *     List<...> allRecords =
 *         YidaApiManager.INSTANCE.searchAllFormData("FORM-XXX", null);
 *
 *     // ===== 自定义查询条件 =====
 *     SearchFormDatasRequest req = YidaApiManager.INSTANCE.newSearchRequest()
 *         .setFormUuid("FORM-XXX")
 *         .setSearchFieldJson("{\"textField_xxx\":\"value\"}");
 *     SearchFormDatasResponseBody result =
 *         YidaApiManager.INSTANCE.searchFormData(req);
 *
 *     // ===== 保存表单数据 =====
 *     SaveFormDataRequest saveReq = new SaveFormDataRequest()
 *         .setAppType(YidaApiManager.INSTANCE.productionSystemAppType)
 *         .setSystemToken(YidaApiManager.INSTANCE.productionSystemSystemToken)
 *         .setUserId(YidaApiManager.INSTANCE.defaultUserId)
 *         .setFormUuid("FORM-XXX")
 *         .setFormDataJson("{\"textField_xxx\":\"hello\"}");
 *     String instanceId = YidaApiManager.INSTANCE.saveFormData(saveReq);
 * }</pre>
 *
 * @author Senior Developer
 * 创建于 2026/07/03
 */
public class YidaApiManager {

    // ========================================
    //  饿汉式单例
    // ========================================

    /** 全局唯一实例，类加载时初始化，JVM 保证线程安全 */
    public static final YidaApiManager INSTANCE = new YidaApiManager();

    private YidaApiManager() {
        // 优先从 classpath:yida-secret.properties 加载敏感配置
        Properties props = new Properties();
        boolean loaded = false;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("yida-secret.properties")) {
            if (is != null) {
                props.load(is);
                loaded = true;
            }
        } catch (IOException e) {
            System.out.println("[YidaApiManager] 读取 yida-secret.properties 失败: " + e.getMessage());
        }

        if (loaded) {
            this.customAppKey = props.getProperty("dingtalk.app.key", DEFAULT_APP_KEY);
            this.customAppSecret = props.getProperty("dingtalk.app.secret", DEFAULT_APP_SECRET);
            this.productionSystemAppType = props.getProperty(
                    "yida.production.app.type", PRODUCTION_SYSTEM_APP_TYPE);
            this.productionSystemSystemToken = props.getProperty(
                    "yida.production.system.token", PRODUCTION_SYSTEM_SYSTEM_TOKEN);
            this.defaultUserId = props.getProperty(
                    "yida.production.user.id", DEFAULT_USER_ID);
            System.out.println("[YidaApiManager] 从 yida-secret.properties 加载配置成功");
        } else {
            this.customAppKey = DEFAULT_APP_KEY;
            this.customAppSecret = DEFAULT_APP_SECRET;
            this.productionSystemAppType = PRODUCTION_SYSTEM_APP_TYPE;
            this.productionSystemSystemToken = PRODUCTION_SYSTEM_SYSTEM_TOKEN;
            this.defaultUserId = DEFAULT_USER_ID;
            System.out.println("[YidaApiManager] 未找到 yida-secret.properties，使用硬编码默认值");
        }
    }


    // ========================================
    //  写死的钉钉企业内部应用凭证
    // ========================================

    /** ⚠️ 请通过 .env 配置 dingtalk.app.key */
    private static final String DEFAULT_APP_KEY = "your-app-key-here";

    /** ⚠️ 请通过 .env 配置 dingtalk.app.secret */
    private static final String DEFAULT_APP_SECRET = "your-app-secret-here";


    // ========================================
    //  生产系统宜搭应用常量（默认值）
    // ========================================

    /** ⚠️ 请通过 .env 配置 yida.production.app.type */
    private static final String PRODUCTION_SYSTEM_APP_TYPE = "your-app-type";

    /** ⚠️ 请通过 .env 配置 yida.production.system.token */
    private static final String PRODUCTION_SYSTEM_SYSTEM_TOKEN = "your-system-token";

    /** ⚠️ 请通过 .env 配置 yida.production.user.id */
    private static final String DEFAULT_USER_ID = "your-user-id";

    /** 宜搭应用编码（可在运行时通过 {@link #setProductionSystemAppType} 覆盖） */
    private String productionSystemAppType = PRODUCTION_SYSTEM_APP_TYPE;

    /** 宜搭应用密钥（可在运行时通过 {@link #setProductionSystemSystemToken} 覆盖） */
    private String productionSystemSystemToken = PRODUCTION_SYSTEM_SYSTEM_TOKEN;

    /** 操作人钉钉 userId（可在运行时通过 {@link #setDefaultUserId} 覆盖） */
    private String defaultUserId = DEFAULT_USER_ID;


    // ========================================
    //  Token 管理（本地缓存 JVM 内存级）
    // ========================================

    /** 钉钉自定义应用的 AppKey */
    private String customAppKey;

    /** 钉钉自定义应用的 AppSecret */
    private String customAppSecret;

    /** 本地缓存的自定义 token（JVM 内存级，FaaS 容器复用期间持续有效） */
    private String cachedToken = "";

    /** 本地缓存 token 的过期时间戳（毫秒），AtomicLong 保证原子读写 */
    private final AtomicLong cachedTokenExpireAt = new AtomicLong(0L);

    /** 缓存过期时间：7200 秒（钉钉官方有效期） */
    private static final long TOKEN_EXPIRY_SECONDS = 7200;

    /** 提前刷新缓冲：300 秒（5分钟），避免边缘续期问题 */
    private static final long REFRESH_BUFFER_SECONDS = 300;


    /**
     * 在运行时覆盖默认的钉钉应用凭证。
     *
     * <p>如果传入的 appKey/appSecret 与当前值相同，则直接跳过（无操作）。
     * 仅在凭证变更时生效，并自动清空旧 token 缓存。
     */
    public void setCustomAppKeySecret(String appKey, String appSecret) {
        if (StringUtils.equals(this.customAppKey, appKey)
                && StringUtils.equals(this.customAppSecret, appSecret)) {
            return;  // 凭证未变，跳过
        }
        this.customAppKey = appKey;
        this.customAppSecret = appSecret;
        // 凭证变了 → 旧 token 作废
        this.cachedToken = "";
        this.cachedTokenExpireAt.set(0L);
    }

    /**
     * 在运行时覆盖默认的生产系统宜搭应用编码。
     */
    public void setProductionSystemAppType(String appType) {
        this.productionSystemAppType = appType;
    }

    /**
     * 在运行时覆盖默认的生产系统宜搭应用密钥。
     */
    public void setProductionSystemSystemToken(String systemToken) {
        this.productionSystemSystemToken = systemToken;
    }

    /**
     * 在运行时覆盖默认的操作人钉钉 userId。
     */
    public void setDefaultUserId(String userId) {
        this.defaultUserId = userId;
    }

    /**
     * 获取当前设置的宜搭应用编码。
     */
    public String getAppType() {
        return productionSystemAppType;
    }

    /**
     * 获取当前设置的宜搭应用 systemToken。
     */
    public String getSystemToken() {
        return productionSystemSystemToken;
    }

    /**
     * 获取当前设置的默认操作人 userId。
     */
    public String getDefaultUserId() {
        return defaultUserId;
    }


    // ---------- 核心 token 获取 ----------

    /**
     * 获取当前有效的 accessToken。
     *
     * <p><b>决策逻辑：</b>
     * <pre>
     * 本地缓存 cachedToken 是否有效？
     *   ├─ 有效 → 直接返回 ✅（0 次网络请求）
     *   └─ 无效/过期/冷启动 → 自动请求钉钉获取 → 写入缓存 → 返回
     * </pre>
     *
     * @return 有效的 accessToken
     * @throws Exception token 获取/刷新失败时抛出
     */
    public String getAccessToken() throws Exception {
        // 子场景 A：本地缓存命中且未过期 → 直接返回
        if (StringUtils.isNotBlank(cachedToken) && !isCachedTokenExpired()) {
            return cachedToken;
        }

        // 子场景 B：本地缓存过期 / 冷启动无缓存 → 刷新
        return refreshAndCacheToken();
    }


    // ---------- token 刷新（私有） ----------

    /**
     * 请求钉钉接口获取新 token，写入本地缓存并返回。
     */
    private String refreshAndCacheToken() throws Exception {
        System.out.println("[YidaApiManager] 本地缓存未命中/已过期，请求钉钉获取新 token...");

        com.aliyun.dingtalkoauth2_1_0.Client client = new com.aliyun.dingtalkoauth2_1_0.Client(
                new Config().setProtocol("https").setRegionId("central")
        );
        GetAccessTokenRequest request = new GetAccessTokenRequest()
                .setAppKey(customAppKey)
                .setAppSecret(customAppSecret);

        try {
            GetAccessTokenResponse response = client.getAccessToken(request);
            if (Objects.isNull(response) || Objects.isNull(response.getBody())) {
                throw new Exception("获取 accessToken 返回空响应");
            }

            String newToken = response.getBody().getAccessToken();
            long expireInSeconds = response.getBody().getExpireIn();

            // 写入本地缓存
            this.cachedToken = newToken;
            long effectiveExpiry = (expireInSeconds > 0) ? expireInSeconds : TOKEN_EXPIRY_SECONDS;
            long newExpireAt = System.currentTimeMillis() + (effectiveExpiry - REFRESH_BUFFER_SECONDS) * 1000L;
            this.cachedTokenExpireAt.set(newExpireAt);

            System.out.println("[YidaApiManager] token 获取成功，已写入本地缓存，有效至: " + newExpireAt);
            return newToken;

        } catch (Exception e) {
            System.out.println("[YidaApiManager] 获取 token 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 检查本地缓存的 token 是否已过期。
     *
     * @return true=已过期, false=有效
     */
    private boolean isCachedTokenExpired() {
        if (StringUtils.isBlank(cachedToken)) {
            return true;
        }
        long expireAt = cachedTokenExpireAt.get();
        return expireAt <= 0 || System.currentTimeMillis() >= expireAt;
    }


    // ========================================
    //  内部工具方法
    // ========================================

    /**
     * 创建 Yida V2 Client。
     * 每次调用创建新实例，避免状态共享问题。
     */
    private Client createClient() throws Exception {
        return new Client(
                new Config().setProtocol("https").setRegionId("central")
        );
    }


    // ========================================
    //  API：表单数据保存/创建
    // ========================================

    /**
     * 保存（创建）单条无审批流程的宜搭表单实例。
     *
     * @param request 保存请求（需自行填充 appType/systemToken/userId/formUuid/formDataJson）
     * @return 新建的表单实例 ID
     * @throws Exception 保存失败时抛出
     */
    public String saveFormData(SaveFormDataRequest request) throws Exception {
        Client client = createClient();
        SaveFormDataHeaders headers = new SaveFormDataHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            SaveFormDataResponse response = client.saveFormDataWithOptions(request, headers, new RuntimeOptions());
            return response.getBody().getResult();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] saveFormData 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 新增或更新表单实例（支持条件筛选）。
     * <p>如果匹配到已有数据则更新，否则新增。</p>
     *
     * @param request 新增或更新请求
     * @return 创建/更新的表单实例 ID 列表
     * @throws Exception 操作失败时抛出
     */
    public List<String> createOrUpdateFormData(CreateOrUpdateFormDataRequest request) throws Exception {
        Client client = createClient();
        CreateOrUpdateFormDataHeaders headers = new CreateOrUpdateFormDataHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            CreateOrUpdateFormDataResponse response = client.createOrUpdateFormDataWithOptions(
                    request, headers, new RuntimeOptions());
            return response.getBody().getResult();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] createOrUpdateFormData 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 更新表单数据。
     *
     * @param request 更新请求（需包含 formInstanceId 等必要字段）
     * @throws Exception 更新失败时抛出
     */
    public void updateFormData(UpdateFormDataRequest request) throws Exception {
        Client client = createClient();
        UpdateFormDataHeaders headers = new UpdateFormDataHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            client.updateFormDataWithOptions(request, headers, new RuntimeOptions());
        } catch (Exception e) {
            System.out.println("[YidaApiManager] updateFormData 失败: " + e.getMessage());
            throw e;
        }
    }

    // ========================================
    //  API：表单数据查询
    // ========================================

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 5;

    /** 指数退避初始等待时间（毫秒） */
    private static final long INITIAL_BACKOFF_MS = 1000L;

    /** 相邻 API 调用的最小间隔（毫秒），用于调用方主动限速 */
    private static final long API_THROTTLE_INTERVAL_MS = 150L;

    /** 上一次 API 调用的时间戳，用于控制调用频率 */
    private long lastApiCallTime = 0L;

    /**
     * 查询表单实例数据（分页），带自动限流重试。
     *
     * <p>当遇到钉钉限流（code: 400 / 请求过于频繁）时，自动实施指数退避重试：</p>
     * <pre>
     *   重试 1：等待 1s
     *   重试 2：等待 2s
     *   重试 3：等待 4s
     *   重试 4：等待 8s
     *   重试 5：等待 16s （最大 5 次）
     * </pre>
     *
     * @param request 查询请求
     * @return 分页结果，含 data（当前页数据列表）、totalCount（总数）、currentPage
     * @throws Exception 超出最大重试次数或非限流类错误
     */
    public SearchFormDatasResponseBody searchFormData(SearchFormDatasRequest request) throws Exception {
        // ---- 主动限速：确保相邻调用间隔至少 API_THROTTLE_INTERVAL_MS ----
        synchronized (this) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastApiCallTime;
            if (elapsed < API_THROTTLE_INTERVAL_MS) {
                Thread.sleep(API_THROTTLE_INTERVAL_MS - elapsed);
            }
            lastApiCallTime = System.currentTimeMillis();
        }

        Client client = createClient();
        SearchFormDatasHeaders headers = new SearchFormDatasHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();

        Exception lastException = null;
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                SearchFormDatasResponse response = client.searchFormDatasWithOptions(
                        request, headers, new RuntimeOptions());
                // 成功则返回
                return Objects.isNull(response) ? null : response.getBody();
            } catch (TeaException e) {
                // 仅对限流错误进行重试
                if (isRateLimitError(e)) {
                    lastException = e;
                    System.out.println("[YidaApiManager] 请求被限流，"
                            + attempt + "/" + MAX_RETRIES + " 次重试，等待 " + backoff + "ms ...");
                    Thread.sleep(backoff);
                    backoff = Math.min(backoff * 2, 16000L);  // 指数退避，上限 16s
                } else {
                    // 非限流错误直接抛出
                    System.out.println("[YidaApiManager] searchFormData 失败: " + e.getMessage());
                    throw e;
                }
            } catch (Exception e) {
                System.out.println("[YidaApiManager] searchFormData 失败: " + e.getMessage());
                throw e;
            }
        }

        // 所有重试均失败
        System.out.println("[YidaApiManager] searchFormData 重试 " + MAX_RETRIES + " 次后仍然失败");
        throw lastException;
    }

    /**
     * 判断是否限流错误。
     * <p>钉钉限流特征：code=400，消息包含"请求过于频繁"。</p>
     */
    private static boolean isRateLimitError(TeaException e) {
        return "400".equals(e.getCode()) && e.getMessage() != null
                && e.getMessage().contains("请求过于频繁");
    }

    /**
     * 创建一个预填默认 appType / systemToken / userId 的查询请求。
     * <p>只需额外设置 formUuid 和可选的 searchFieldJson 即可使用。</p>
     */
    public SearchFormDatasRequest newSearchRequest() {
        return new SearchFormDatasRequest()
                .setAppType(productionSystemAppType)
                .setSystemToken(productionSystemSystemToken)
                .setUserId(defaultUserId)
                .setLanguage("zh_CN");
    }

    /**
     * 分页获取全部表单实例数据（自动处理翻页）。
     *
     * <p>钉钉查询接口单次最大返回 100 条（pageSize=100），
     * 当符合条件的数据超过 100 条时，此方法会自动遍历所有页面并合并结果。</p>
     *
     * <p><b>注意：</b>此方法会在 request 上强制设置 pageSize=100，
     * 并从 page=1 开始递增直至取完所有数据。</p>
     *
     * @param request 查询请求（appType/systemToken/userId 需已设置，不设则使用 DEFAULT 值）
     * @return 合并后的完整数据列表
     * @throws Exception 任何一次分页查询失败时抛出
     */
    public List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> searchAllFormData(
            SearchFormDatasRequest request) throws Exception {

        // 如果调用方没设这三个字段，自动补默认值
        if (StringUtils.isBlank(request.getAppType())) {
            request.setAppType(productionSystemAppType);
        }
        if (StringUtils.isBlank(request.getSystemToken())) {
            request.setSystemToken(productionSystemSystemToken);
        }
        if (StringUtils.isBlank(request.getUserId())) {
            request.setUserId(defaultUserId);
        }

        // 强制最大页大小
        request.setPageSize(100);
        request.setCurrentPage(1);

        // 第一页
        SearchFormDatasResponseBody firstPage = searchFormData(request);
        if (firstPage == null || firstPage.getData() == null || firstPage.getData().isEmpty()) {
            return Collections.emptyList();
        }

        long totalCount = firstPage.getTotalCount();
        List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> allData =
                new ArrayList<>(firstPage.getData());

        // 计算剩余页数
        int pageSize = 100;
        long totalPages = (totalCount + pageSize - 1) / pageSize;  // 向上取整

        // 从第 2 页开始取
        for (int page = 2; page <= totalPages; page++) {
            request.setCurrentPage(page);
            SearchFormDatasResponseBody pageData = searchFormData(request);
            if (pageData != null && pageData.getData() != null) {
                allData.addAll(pageData.getData());
            }
        }

        System.out.println("[YidaApiManager] searchAllFormData 完成，共 " + totalCount + " 条数据，"
                + totalPages + " 页");
        return allData;
    }

    /**
     * 便捷版：直接用 formUuid 查询全部数据，searchFieldJson 可选。
     */
    public List<SearchFormDatasResponseBody.SearchFormDatasResponseBodyData> searchAllFormData(
            String formUuid, String searchFieldJson) throws Exception {
        SearchFormDatasRequest request = newSearchRequest()
                .setFormUuid(formUuid);
        if (StringUtils.isNotBlank(searchFieldJson)) {
            request.setSearchFieldJson(searchFieldJson);
        }
        return searchAllFormData(request);
    }

    /**
     * 通过表单实例 ID 查询表单数据详情。
     *
     * @param formInstanceId 表单实例 ID（路径参数）
     * @param request        查询请求（含 appType/systemToken/userId/formUuid）
     * @return 表单数据详情，含所有组件字段值
     * @throws Exception 查询失败时抛出
     */
    public GetFormDataByIDResponseBody getFormDataByID(String formInstanceId, GetFormDataByIDRequest request)
            throws Exception {
        Client client = createClient();
        GetFormDataByIDHeaders headers = new GetFormDataByIDHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            GetFormDataByIDResponse response = client.getFormDataByIDWithOptions(
                    formInstanceId, request, headers, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] getFormDataByID 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 获取表单实例 ID 列表（分页）。
     * <p>相比 {@link #searchFormData}，此接口只返回 ID 列表，数据量更小，适合仅需 ID 的场景。</p>
     *
     * @param appType  应用编码
     * @param formUuid 表单 ID
     * @param request  查询请求（含分页参数 currentPage / pageSize）
     * @return 当前页的表单实例 ID 列表
     * @throws Exception 查询失败时抛出
     */
    public List<String> searchFormDataIdList(String appType, String formUuid, SearchFormDataIdListRequest request)
            throws Exception {
        Client client = createClient();
        SearchFormDataIdListHeaders headers = new SearchFormDataIdListHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            SearchFormDataIdListResponse response = client.searchFormDataIdListWithOptions(
                    appType, formUuid, request, headers, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody().getData();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] searchFormDataIdList 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 通过高级查询条件获取表单实例数据（<b>含子表单组件数据</b>）。
     * <p>{@link #searchFormData} 不含子表单数据，如需子表单内容请调用此方法。</p>
     *
     * @param request 高级查询请求
     * @return 表单实例数据，每行含子表数据（SearchFormDataSecondGenerationResponseBodyData 中包含 subForm 字段）
     * @throws Exception 查询失败时抛出
     */
    public SearchFormDataSecondGenerationResponseBody searchFormDataSecondGeneration(
            SearchFormDataSecondGenerationRequest request) throws Exception {
        Client client = createClient();
        SearchFormDataSecondGenerationHeaders headers = new SearchFormDataSecondGenerationHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            SearchFormDataSecondGenerationResponse response = client.searchFormDataSecondGenerationWithOptions(
                    request, headers, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] searchFormDataSecondGeneration 失败: " + e.getMessage());
            throw e;
        }
    }

    // ========================================
    //  API：审批流程
    // ========================================

    /**
     * 发起宜搭审批流程。
     *
     * @param request 发起流程请求（含 appType/systemToken/userId/formUuid/formDataJson/processCode）
     * @return 流程实例 ID
     * @throws Exception 发起失败时抛出
     */
    public String startProcessInstance(StartInstanceRequest request) throws Exception {
        Client client = createClient();
        StartInstanceHeaders headers = new StartInstanceHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            StartInstanceResponse response = client.startInstanceWithOptions(
                    request, headers, new RuntimeOptions());
            return response.getBody().getResult();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] startInstance 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 批量获取流程实例列表。
     *
     * @param request 查询请求（可按流程编码、创建时间等条件过滤）
     * @return 流程实例列表（含各节点的审批人、状态等信息）
     * @throws Exception 查询失败时抛出
     */
    public GetInstancesResponseBody getInstances(GetInstancesRequest request) throws Exception {
        Client client = createClient();
        GetInstancesHeaders headers = new GetInstancesHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            GetInstancesResponse response = client.getInstancesWithOptions(
                    request, headers, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] getInstances 失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 根据流程实例 ID 获取单条流程实例详情。
     *
     * @param instanceId 流程实例 ID
     * @param request    查询请求（含 appType/systemToken/userId）
     * @return 流程实例详情，含表单数据、审批记录、当前节点等
     * @throws Exception 查询失败时抛出
     */
    public GetInstanceByIdResponseBody getInstanceById(String instanceId, GetInstanceByIdRequest request)
            throws Exception {
        Client client = createClient();
        GetInstanceByIdHeaders headers = new GetInstanceByIdHeaders();
        headers.xAcsDingtalkAccessToken = getAccessToken();
        try {
            GetInstanceByIdResponse response = client.getInstanceByIdWithOptions(
                    instanceId, request, headers, new RuntimeOptions());
            return Objects.isNull(response) ? null : response.getBody();
        } catch (Exception e) {
            System.out.println("[YidaApiManager] getInstanceById 失败: " + e.getMessage());
            throw e;
        }
    }
}
