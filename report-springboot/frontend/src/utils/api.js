/**
 * 统一 API 请求工具。
 *
 * 封装 fetch()，自动携带认证凭据，统一处理 401 Session 过期，
 * 避免每个组件都重复写「无法连接」的 fallback。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */

const BASE_URL = import.meta.env.BASE_URL

/**
 * 从 Cookie 中读取 CSRF token（XSRF-TOKEN）。
 * Spring Security CookieCsrfTokenRepository 默认 cookie 名为 "XSRF-TOKEN"。
 */
function getCsrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : ''
}

/**
 * 是否需要 CSRF token（只有非 GET 请求需要）。
 */
function needsCsrf(method) {
  return method && method.toUpperCase() !== 'GET'
}

/**
 * 将相对路径转换为带部署前缀的绝对路径。
 * 例如：/api/admin/logs → /report/api/admin/logs（生产环境）
 */
function resolveUrl(url) {
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  const normalized = url.startsWith('/') ? url.slice(1) : url
  return BASE_URL + normalized
}

/**
 * 发起 API 请求。
 *
 * @param {string} url - 请求路径（如 /api/admin/logs）
 * @param {object} [options] - 透传给 fetch 的选项
 * @param {boolean} [options.noRedirect] - 为 true 时不自动跳转登录页
 * @returns {Promise<Response>} fetch Response 对象
 * @throws {Error} 网络错误或 Session 过期时抛出
 */
export async function apiFetch(url, options = {}) {
  const { noRedirect, method, ...fetchOptions } = options

  const headers = {
    ...(fetchOptions.body
      ? { 'Content-Type': 'application/json' }
      : {})
  }

  // 非 GET 请求自动附加 CSRF token
  if (needsCsrf(method)) {
    const token = getCsrfToken()
    if (token) {
      headers['X-XSRF-TOKEN'] = token
    }
  }

  // 合并用户自定义 headers
  if (fetchOptions.headers) {
    Object.assign(headers, fetchOptions.headers)
  }

  // 构建请求
  const controller = new AbortController()
  const timeoutMs = 180000  // 180s 超时（月报大查询）
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  const res = await fetch(resolveUrl(url), {
    credentials: 'include',
    method,
    signal: controller.signal,
    ...fetchOptions,
    headers
  })
  clearTimeout(timeoutId)

  // 401 → Session 过期，自动跳转登录
  if (res.status === 401 && !noRedirect) {
    window.location.href = resolveUrl('/login')
    throw new Error('SESSION_EXPIRED')
  }

  // 403 + CSRF 失败 → 尝试刷新页面重试（token 可能过期）
  if (res.status === 403 && needsCsrf(method)) {
    console.warn('[API] CSRF token 可能已过期，刷新页面重试')
  }

  return res
}

/**
 * 封装 JSON 响应的便捷方法（GET）。
 *
 * @param {string} url
 * @param {object} [options]
 * @returns {Promise<object>} 解析后的 JSON
 */
export async function apiGet(url, options = {}) {
  const res = await apiFetch(url, options)
  return res.json()
}

/**
 * 封装 JSON 响应的便捷方法（POST）。
 *
 * @param {string} url
 * @param {object} [body] - 请求体（会自动 JSON.stringify）
 * @param {object} [options]
 * @returns {Promise<object>} 解析后的 JSON
 */
export async function apiPost(url, body, options = {}) {
  const res = await apiFetch(url, {
    method: 'POST',
    body: typeof body === 'string' ? body : JSON.stringify(body),
    ...options
  })
  return res.json()
}

/**
 * 封装 JSON 响应的便捷方法（PUT）。
 */
export async function apiPut(url, body, options = {}) {
  const res = await apiFetch(url, {
    method: 'PUT',
    body: JSON.stringify(body),
    ...options
  })
  return res.json()
}

/**
 * 封装 JSON 响应的便捷方法（DELETE）。
 */
export async function apiDelete(url, options = {}) {
  const res = await apiFetch(url, {
    method: 'DELETE',
    ...options
  })
  return res.json()
}
