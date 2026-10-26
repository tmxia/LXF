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

    suspend fun fetchHomeThreads(fid: String = ""): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            // 强制首页默认为股票软件 68
            var actualFid = "68"
            var typeId: String? = null

            // 如果传入的 fid 包含下划线（如 68_129），说明是子标签
            if (fid.contains("_")) {
                val parts = fid.split("_")
                actualFid = parts[0]
                typeId = parts.getOrNull(1)
            } else if (fid.isNotEmpty() && fid.all { it.isDigit() }) {
                actualFid = fid
            }

            // 构造 URL：普通板块用 forum-xx-1.html，子标签用 filter=typeid 形式
            val urlStr = if (typeId != null) {
                "https://www.55188.com/forum.php?mod=forumdisplay&fid=$actualFid&filter=typeid&typeid=$typeId"
            } else {
                "https://www.55188.com/forum-$actualFid-1.html"
            }
            
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val links = doc.select("a[href*=thread-]")
            val addedTids = mutableSetOf<String>()
            
            for (a in links) {
                val href = a.attr("href")
                if (href.contains("forum-post")) continue
                
                val tidMatch = Regex("thread-(\\d+)-").find(href)
                if (tidMatch == null) continue
                val tid = tidMatch.groupValues[1]
                if (addedTids.contains(tid)) continue
                
                var title = ""
                val h6 = a.selectFirst("h6")
                if (h6 != null) {
                    title = h6.text().trim()
                } else {
                    title = a.text().trim()
                }
                
                if (title.length < 5 || title.contains("发帖") || title.contains("回复")) continue
                
                var views = 0
                var replies = 0
                var dateline = "刚刚"
                
                val infoDiv = a.selectFirst("div.article-info") ?: a.parent()?.selectFirst("div.article-info")
                if (infoDiv != null) {
                    val ems = infoDiv.select("em.z")
                    if (ems.isNotEmpty()) {
                        val nums = Regex("(\\d+)").findAll(ems[0].text()).toList()
                        if (nums.isNotEmpty()) views = nums[0].value.toIntOrNull() ?: 0
                        if (nums.size > 1) replies = nums[1].value.toIntOrNull() ?: 0
                    }
                    if (ems.size > 1) dateline = ems[1].text().trim().replace("\u00a0", "")
                }
                
                addedTids.add(tid)
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
                if (threads.size >= 30) break
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
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val title = doc.select("title").text().replace(" - 理想论坛", "").trim()
            val contentHtml = doc.select("td.t_f").firstOrNull()?.html() ?: "无法解析内容或需要登录查看"
            
            val fakePost = Post(
                pid = "post_" + tid,
                authorName = "理想楼主",
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
                totalPages = 1,
                formhash = "",
                replyUrl = ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ThreadPage(
                tid = tid,
                subject = "加载失败",
                forumName = "",
                posts = emptyList(),
                page = 1,
                totalPages = 1,
                formhash = "",
                replyUrl = ""
            )
        }
    }

    // 定制板块列表：主要展示股票软件及其子分类
    suspend fun fetchForumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        listOf(
            ForumGroup(name = "股票软件", forums = listOf(
                Forum(fid = "68", name = "全部主题"),
                Forum(fid = "68_129", name = "通达信"),
                Forum(fid = "68_179", name = "大智慧"),
                Forum(fid = "68_130", name = "同花顺"),
                Forum(fid = "68_131", name = "东财通"),
                Forum(fid = "68_127", name = "飞狐"),
                Forum(fid = "68_132", name = "其它")
            )),
            ForumGroup(name = "热门核心", forums = listOf(
                Forum(fid = "48", name = "指标公式"),
                Forum(fid = "90", name = "指标问答"),
                Forum(fid = "93", name = "金钻指标")
            ))
        )
    }
}
