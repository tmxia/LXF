package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object LxfParser {
    suspend fun fetchHomeThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            val url = URL("https://www.55188.com/")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            // 55188 使用 GBK 编码
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
                        
                        // 根据报错，MTF 的 Thread 模型标题字段名为 subject
                        threads.add(
                            Thread(
                                tid = tid,
                                subject = titleText,
                                authorName = "理想股友" // 如果还有报错，直接在 Thread.kt 改默认值即可
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
}
