package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.LxThread
import com.aazhou.mtforum.data.parser.LxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object LxRepository {
    // 注入你提供的完整 Cookie
    private const val COOKIE = "HMACCOUNT=F3FD580504BD7E15; Hm_lpvt_0233b908b2bf29bfd788abd7ee3556da=1757035032; 55188_passport=XQbXwQnsMTPVjz0f289tEqA1yBENwwP0LKUwEJPVcP2K%2Fh9czvN0Odaeil2mq0q%2B%2FoDOUdMjXbkJq5%2F72eybrVGWt2J0dE8nBItzlqUoFKju%2BeUADFNhFhZvatV2V0G6N5e0J8uHNn2h7bnLeE6aC92dTCbtOvPl5QsVq8ygxrg%3D; passport2bbs=gXfzvUFx5Yx567ZhgndUtE89%2F81R6s5TCwZUBHERqfjVS%2FHg1lbEh%2FKGNJI7x1dK; cdb2_auth=jiqsgmbJXdBFKY8Asxbfh26nwQIv%2BJpbCF%2F01k%2BELaRiGSsEFr%2BxBzq%2BVeirpKA0; vOVx_56cc_saltkey=NN6A55Gs; vOVx_56cc_lastvisit=1792912771; vOVx_56cc_auth=031dQCd%2FCf1lezfzIt37Cr6SNufUPY99nXC1aHDYwbBy4Wr4vfCb4hcoHMT4fo7MuRbUjY3%2FiXlpSPm19SyBB5YWYls; vOVx_56cc_ulastactivity=9c91VEYN82V2Y9GJ5aM10iJ9Ct3zLWZz3GCnH8jisy0avIRUXTmF; vOVx_56cc_sid=EDh4Q1; vOVx_56cc_lip=49.118.82.123%2C1792916374; vOVx_56cc_atarget=1; vOVx_56cc_visitedfid=68; vOVx_56cc_home_diymode=1; vOVx_56cc_st_t=784998%7C1792925605%7C44054d47715c210d61bec28229d9defb; vOVx_56cc_forum_lastvisit=D_68_1792925605; vOVx_56cc_lastact=1792925653%09forum.php%09image"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Cookie", COOKIE)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Referer", "https://www.55188.com/")
                .build()
            chain.proceed(request)
        }
        .build()

    // 内存缓存，避免重复请求
    private var cachedThreads: List<LxThread>? = null

    suspend fun getCachedOrFetchThreads(fid: String = "68", forceRefresh: Boolean = false): List<LxThread> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedThreads != null) {
            return@withContext cachedThreads!!
        }
        val url = "https://www.55188.com/forum-$fid-1.html"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val htmlBytes = response.body?.bytes() ?: throw Exception("返回为空")
        val html = String(htmlBytes, charset("UTF-8"))
        
        val parsed = LxParser.parseThreadList(html)
        cachedThreads = parsed
        parsed
    }
}
