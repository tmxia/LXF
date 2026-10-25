package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.model.ForumPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object LxfParser {
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun fetchHomeThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            val url = URL("https://www.55188.com/")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
            val doc = Jsoup.parse(html)
            
            val elements = doc.select("a[href^=thread-]")
            val addedTids = mutableSetOf<String>()
            
            for (el in elements) {
                val href = el.attr("href")
                val tidMatch = Regex("thread-(\\d+)-").find(href)
                val titleText = el.text().trim()
                
                if (tidMatch != null && titleText.isNotEmpty() && titleText.length > 5) {
                    val tid = tidMatch.groupValues[1]
                    if (!addedTids.contains(tid)) {
                        addedTids.add(tid)
                        // 尝试获取作者，如果找不到则用默认
                        var author = "理想股友"
                        val parent = el.parent()
                        if (parent != null) {
                            val authorEl = parent.select("a[href^=space-uid-]").firstOrNull()
                            if (authorEl != null) author = authorEl.text().trim()
                        }
                        
                        threads.add(
                            Thread(
                                tid = tid,
                                subject = titleText,
                                authorName = author
                            )
                        )
                    }
                }
                if (threads.size >= 20) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        threads
    }

    // 抓取详情页真实内容
    suspend fun fetchThreadDetail(tid: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.55188.com/thread-$tid-1-1.html")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
            val doc = Jsoup.parse(html)
            
            // 提取主楼内容，Discuz 通常是 .t_f 或者 #postmessage_xxx
            val postContent = doc.select("td.t_f").firstOrNull()?.html() ?: "无法解析内容"
            // 简单清洗一下 HTML
            return@withContext postContent.replace("<br>", "\n").replace(Regex("<[^>]*>"), "")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "网络请求失败: ${e.message}"
        }
    }
}
