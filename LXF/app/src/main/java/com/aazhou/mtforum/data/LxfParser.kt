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
                
                var views = 0
                var replies = 0
                var dateline = "刚刚"
                val infoDiv = a.selectFirst("div.article-info")
                if (infoDiv != null) {
                    val ems = infoDiv.select("em.z")
                    if (ems.isNotEmpty()) {
                        val nums = Regex("(\\d+)").findAll(ems[0].text()).toList()
                        if (nums.isNotEmpty()) views = nums[0].value.toIntOrNull() ?: 0
                        if (nums.size > 1) replies = nums[1].value.toIntOrNull() ?: 0
                    }
                    if (ems.size > 1) {
                        dateline = ems[1].text().trim().replace("\u00a0", "")
                    }
                }
                
                // 这里使用 .toString() 将 Int 强转为 String，匹配报错日志中的 'String?' 类型
                threads.add(
                    Thread(
                        tid = tid,
                        subject = title,
                        authorName = "理想股友",
                        viewCount = views.toString(),
                        replyCount = replies.toString(),
                        timeText = dateline
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
