package com.aazhou.mtforum.data

import com.aazhou.mtforum.data.model.Thread
import com.aazhou.mtforum.data.model.ThreadPage
import com.aazhou.mtforum.data.model.Post
import com.aazhou.mtforum.data.model.ForumGroup
import com.aazhou.mtforum.data.model.Forum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object LxfParser {
    private const val UA = "Mozilla/5.0 (Linux; Android 12)"

    // 获取首页/板块列表
    suspend fun fetchHomeThreads(fid: String = ""): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            val urlStr = if (fid.isEmpty()) {
                "https://www.55188.com/forum.php?mod=guide&view=newthread&mobile=2"
            } else {
                "https://www.55188.com/forum-$fid-1.html"
            }
            val url = URL(urlStr)
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
                    if (ems.size > 1) dateline = ems[1].text().trim().replace("\u00a0", "")
                }
                
                threads.add(
                    Thread(
                        tid = tid,
                        subject = title,
                        authorName = "理想股友",
                        views = views.toString(),
                        replies = replies.toString(),
                        dateline = dateline
                    )
                )
                if (threads.size >= 20) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        threads
    }

    // 获取详情页
    suspend fun fetchThreadDetail(tid: String): ThreadPage = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.55188.com/thread-" + tid + "-1-1.html")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val title = doc.select("title").text().replace(" - 理想论坛", "").trim()
            // 获取楼主正文
            val contentHtml = doc.select("td.t_f").firstOrNull()?.html() ?: "无法解析内容或需要登录查看"
            
            val fakePost = Post(
                pid = "post_" + tid,
                authorName = "理想楼主",
                timeText = "刚刚",
                contentHtml = contentHtml,
                isOp = true,
                isFirstPost = true
            )
            
            ThreadPage(
                tid = tid,
                subject = title,
                forumName = "理想论坛",
                posts = listOf(fakePost),
                page = 1,
                totalPages = 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ThreadPage(
                tid = tid,
                subject = "加载失败",
                forumName = "",
                posts = emptyList(),
                page = 1,
                totalPages = 1
            )
        }
    }

    // 提供板块列表 (写死 55188 的核心板块，绕过复杂的原生 UI 适配)
    suspend fun fetchForumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        listOf(
            ForumGroup(name = "热门核心", forums = listOf(
                Forum(fid = "48", name = "指标公式"),
                Forum(fid = "68", name = "股票软件"),
                Forum(fid = "90", name = "指标问答"),
                Forum(fid = "93", name = "金钻指标")
            )),
            ForumGroup(name = "实战交流", forums = listOf(
                Forum(fid = "8", name = "实战交流"),
                Forum(fid = "102", name = "选股技巧"),
                Forum(fid = "45", name = "研究报告"),
                Forum(fid = "20", name = "休闲娱乐")
            ))
        )
    }
}
