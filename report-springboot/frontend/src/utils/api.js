/**
 * 统一 API 请求工具。
 *
 * 封装 fetch()，自动携带认证凭据，统一处理 401 Session 过期，
 * 避免每个组件都重复写「无法连接」的 fallback。
 *
 * @author Senior Developer
 * 创建于 2026/07/08
 */

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
  const { noRedirect, ...fetchOptions } = options

  const res = await fetch(url, {
    credentials: 'include',
    ...fetchOptions,
    headers: {
      ...(fetchOptions.body
        ? { 'Content-Type': 'application/json' }
        : {}),
      ...fetchOptions.headers
    }
  })

  // 401 → Session 过期，自动跳转登录
  if (res.status === 401 && !noRedirect) {
    window.location.href = '/login'
    throw new Error('SESSION_EXPIRED')
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
