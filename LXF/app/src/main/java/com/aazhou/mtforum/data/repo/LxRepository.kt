package com.aazhou.mtforum.data.repo

import com.aazhou.mtforum.data.model.LxThread
import com.aazhou.mtforum.data.parser.LxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object LxRepository {
    private val client = OkHttpClient.Builder().build()

    suspend fun fetchForumThreads(fid: String = "48", page: Int = 1): List<LxThread> = withContext(Dispatchers.IO) {
        val url = "https://www.55188.com/forum-$fid-$page.html"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", "https://www.55188.com/")
            .build()

        val response = client.newCall(request).execute()
        val htmlBytes = response.body?.bytes() ?: throw Exception("返回为空")
        val html = String(htmlBytes, charset("UTF-8"))

        // 解析网页
        LxParser.parseThreadList(html)
    }
}
