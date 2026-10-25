package com.aazhou.mtforum.data.repo

import android.content.Context
import com.aazhou.mtforum.data.model.LxThread
import com.aazhou.mtforum.data.parser.LxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.File
import java.util.concurrent.TimeUnit

object LxRepository {
    private const val COOKIE = "HMACCOUNT=F3FD580504BD7E15; Hm_lpvt_0233b908b2bf29bfd788abd7ee3556da=1757035032; 55188_passport=XQbXwQnsMTPVjz0f289tEqA1yBENwwP0LKUwEJPVcP2K%2Fh9czvN0Odaeil2mq0q%2B%2FoDOUdMjXbkJq5%2F72eybrVGWt2J0dE8nBItzlqUoFKju%2BeUADFNhFhZvatV2V0G6N5e0J8uHNn2h7bnLeE6aC92dTCbtOvPl5QsVq8ygxrg%3D; passport2bbs=gXfzvUFx5Yx567ZhgndUtE89%2F81R6s5TCwZUBHERqfjVS%2FHg1lbEh%2FKGNJI7x1dK; cdb2_auth=jiqsgmbJXdBFKY8Asxbfh26nwQIv%2BJpbCF%2F01k%2BELaRiGSsEFr%2BxBzq%2BVeirpKA0; vOVx_56cc_saltkey=NN6A55Gs; vOVx_56cc_lastvisit=1792912771; vOVx_56cc_auth=031dQCd%2FCf1lezfzIt37Cr6SNufUPY99nXC1aHDYwbBy4Wr4vfCb4hcoHMT4fo7MuRbUjY3%2FiXlpSPm19SyBB5YWYls; vOVx_56cc_ulastactivity=9c91VEYN82V2Y9GJ5aM10iJ9Ct3zLWZz3GCnH8jisy0avIRUXTmF; vOVx_56cc_sid=EDh4Q1; vOVx_56cc_lip=49.118.82.123%2C1792916374; vOVx_56cc_atarget=1; vOVx_56cc_visitedfid=68; vOVx_56cc_home_diymode=1; vOVx_56cc_st_t=784998%7C1792925605%7C44054d47715c210d61bec28229d9defb; vOVx_56cc_forum_lastvisit=D_68_1792925605; vOVx_56cc_lastact=1792925653%09forum.php%09image"

    private const val PC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Cookie", COOKIE)
                .header("User-Agent", PC_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Referer", "https://www.55188.com/")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun batchDownloadAndCache(context: Context, fid: String = "68"): List<LxThread> = withContext(Dispatchers.IO) {
        val allThreads = mutableListOf<LxThread>()
        
        coroutineScope {
            val deferreds = (1..3).map { page ->
                async<List<LxThread>> {
                    try {
                        val url = "https://www.55188.com/forum-$fid-$page.html"
                        val request = Request.Builder().url(url).build()
                        val response: Response = client.newCall(request).execute()
                        val htmlBytes = response.body?.bytes() ?: return@async emptyList()
                        val html = String(htmlBytes, charset("UTF-8"))
                        
                        val file = File(context.cacheDir, "lx_forum_${fid}_$page.html")
                        file.writeText(html)
                        
                        LxParser.parseThreadList(html)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            val results = deferreds.awaitAll()
            results.forEach { allThreads.addAll(it) }
        }
        return@withContext allThreads
    }

    suspend fun getCachedThreads(context: Context, fid: String = "68"): List<LxThread> = withContext(Dispatchers.IO) {
        val allThreads = mutableListOf<LxThread>()
        val uniqueTids = mutableSetOf<String>()
        
        (1..3).forEach { page ->
            val file = File(context.cacheDir, "lx_forum_${fid}_$page.html")
            if (file.exists()) {
                val html = file.readText()
                val pageThreads = LxParser.parseThreadList(html)
                pageThreads.forEach {
                    if (uniqueTids.add(it.tid)) {
                        allThreads.add(it)
                    }
                }
            }
        }
        return@withContext allThreads
    }

    suspend fun fetchThreadDetail(tid: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val url = "https://www.55188.com/thread-$tid-1-1.html"
        val request = Request.Builder().url(url).build()
        val response: Response = client.newCall(request).execute()
        val htmlBytes = response.body?.bytes() ?: throw Exception("返回为空")
        val html = String(htmlBytes, charset("UTF-8"))

        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("h1, #thread_subject, .thread-title, .ts")?.text() ?: "无标题"

        var contentEl = doc.selectFirst("td.t_f, div.t_f, .postmessage")
        
        var contentText = if (contentEl != null) {
            contentEl.html()
        } else {
            doc.selectFirst("div[id^=postmessage_]")?.html() ?: "未提取到正文内容"
        }
        
        // 使用标准的正则字符串
        contentText = Jsoup.parse(contentText).text().replace(Regex("\\s+"), " ").trim()

        Pair(title, contentText)
    }
}
