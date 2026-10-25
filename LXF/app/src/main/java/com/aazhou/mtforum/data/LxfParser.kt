package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object LxfParser {
    suspend fun fetchHomeThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            // 请求理想论坛首页
            val url = URL("https://www.55188.com/")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            // 55188 使用 GBK 编码
            val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
            val doc = Jsoup.parse(html)
            
            // 抓取所有的帖子链接
            val elements = doc.select("a[href^=thread-]")
            val addedTids = mutableSetOf<String>()
            
            for (el in elements) {
                val href = el.attr("href")
                // 正则提取 tid
                val tidMatch = Regex("thread-(\\d+)-").find(href)
                val title = el.text().trim()
                
                // 过滤掉短标题和无 tid 的链接
                if (tidMatch != null && title.isNotEmpty() && title.length > 5) {
                    val tid = tidMatch.groupValues[1]
                    if (!addedTids.contains(tid)) {
                        addedTids.add(tid)
                        
                        // 构造 MTF 的 Thread 数据模型（填充默认值防止 UI 崩溃）
                        threads.add(
                            Thread(
                                tid = tid,
                                title = title,
                                author = "理想股友",
                                fid = "1" // 默认板块 ID
                                // 如果 MTF 的 Thread 还有其他必填字段，编译报错时请告诉我
                            )
                        )
                    }
                }
                if (threads.size >= 20) break // 首页抓取 20 条
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        threads
    }
}
