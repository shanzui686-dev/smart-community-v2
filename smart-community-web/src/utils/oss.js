import request from '@/utils/request'

const signedCache = new Map()

// 公共读的 Bucket，这些 Bucket 的 URL 无需签名，直接访问
const PUBLIC_BUCKETS = ['smart-community-v1']

/**
 * 获取 OSS 签名 URL（仅对私有 Bucket 签名）
 * @param {string} rawUrl - 原始 OSS URL
 * @param {number} expire - 过期秒数，默认 3600
 * @returns {Promise<string>} 签名 URL 或原始 URL
 */
export async function getSignedUrl(rawUrl, expire = 3600) {
  if (!rawUrl) return ''
  // 非 OSS URL 直接返回
  if (!rawUrl.includes('aliyuncs.com')) return rawUrl

  // 公共读的 Bucket 不需要签名
  if (PUBLIC_BUCKETS.some(b => rawUrl.includes(b))) return rawUrl

  const cacheKey = rawUrl
  const cached = signedCache.get(cacheKey)
  if (cached && Date.now() - cached.time < 55 * 60 * 1000) {
    return cached.url
  }

  try {
    const { data } = await request.get('/common/signed-url', {
      params: { url: rawUrl, expire }
    })
    signedCache.set(cacheKey, { url: data, time: Date.now() })
    return data
  } catch {
    return rawUrl
  }
}

/** 清除签名缓存 */
export function clearSignedCache() {
  signedCache.clear()
}
