package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object LxfParser {
    private const val UA = "Mozilla/5.0 (Linux; Android 12)"

    suspend fun fetchHomeThreads(): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            val url = URL("https://www.55188.com/forum.php?mod=guide&view=newthread&mobile=2")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val links = doc.select("a[href~=thread-\\d+-1-1\\.html]")
            for (a in links) {
                val href = a.attr("href")
                val tidMatch = Regex("thread-(\\d+)-").find(href)
                if (tidMatch == null) continue
                val tid = tidMatch.groupValues[1]
                val titleTag = a.selectFirst("h6.article-title")
                val title = titleTag?.text()?.trim() ?: continue
                
                // 只传绝对存在的 tid 和 subject，防止模型不匹配
                threads.add(
                    Thread(
                        tid = tid,
                        subject = title
                    )
                )
                if (threads.size >= 20) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        threads
    }
}
