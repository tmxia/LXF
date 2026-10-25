package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.model.ThreadPage
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
                        threads.add(
                            Thread(
                                tid = tid,
                                subject = titleText
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

    suspend fun fetchThreadDetail(tid: String): ThreadPage = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.55188.com/thread-" + tid + "-1-1.html")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
            val doc = Jsoup.parse(html)
            
            val title = doc.select("title").text().replace(" - 理想论坛", "").trim()
            val postContent = doc.select("td.t_f").firstOrNull()?.html() ?: "无法解析内容或需要登录查看"
            val cleanContent = postContent.replace("<br>", "\\n").replace(Regex("<[^>]*>"), "")
            
            val fakePost = Post(
                pid = "pid_" + tid,
                authorName = "理想股友",
                authorUid = null,
                avatarUrl = null,
                level = null,
                timeText = null,
                location = null,
                contentHtml = cleanContent,
                isOp = false,
                isFirstPost = false,
                floor = null,
                floorLabel = null,
                quote = null,
                editedText = null,
                hasHiddenContent = false,
                sticky = false
            )
            
            ThreadPage(
                tid = "",
                subject = null,
                forumName = null,
                posts = listOf(fakePost)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ThreadPage(
                tid = "",
                subject = null,
                forumName = null,
                posts = emptyList()
            )
        }
    }
}
