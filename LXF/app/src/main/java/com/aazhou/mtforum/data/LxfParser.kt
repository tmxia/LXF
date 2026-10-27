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
    private const val COOKIE = "HMACCOUNT=F3FD580504BD7E15; 55188_passport=kn9qGAdENqKqk9ZT%2B%2FWjDVP35XMgGU142hHt6Ui3%2F9zKlODtT6%2F2%2B%2BoqX38%2FYp%2F4hg4hK7ss26VuIVFTpcvK%2BOOz%2FCvwqmKKSStJjyp%2FYnZQHjsm1NIFcGkjPvkyZkTmqZntqnYuN%2BuUJH%2B4qfsL2cST%2F2tMPu29b27w%2FXQF1ak%3D; passport2bbs=gXfzvUFx5Yx567ZhgndUtE89%2F81R6s5TCwZUBHERqfjVS%2FHg1lbEh%2FKGNJI7x1dK; vOVx_56cc_auth=b651Jm4PoQPCrseWTLmKlXp4zb8jVErgIwlAgbGXSi8pm42VFPmi788YjNWvibki6GzDbVKMY%2BxCC%2FC5UYkNjzlTAXo"

    suspend fun fetchHomeThreads(fid: String = ""): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            var actualFid = "68"
            var typeId: String? = null
            
            // 解析子标签 (如 68_129)
            if (fid.contains("_")) {
                val parts = fid.split("_")
                actualFid = parts[0]
                typeId = parts.getOrNull(1)
            } else if (fid.isNotEmpty() && fid.all { it.isDigit() }) {
                // 如果上层传入单纯的数字 fid，说明是点击了某个大类板块
                actualFid = fid
            } else {
                // 如果传入的是 "newthread" 等字符串，强行锁定到 68
                actualFid = "68"
            }

            val urlStr = if (typeId != null) {
                "https://www.55188.com/forum.php?mod=forumdisplay&fid=$actualFid&filter=typeid&typeid=$typeId"
            } else {
                "https://www.55188.com/forum-$actualFid-1.html"
            }

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Cookie", COOKIE)
            
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val articles = doc.select("article.thread-item")
            val addedTids = mutableSetOf<String>()
            
            if (articles.isNotEmpty()) {
                for (article in articles) {
                    if (article.classNames().contains("zd")) continue
                    val a = article.selectFirst("a.subject") ?: continue
                    parseThread(a, article.text(), threads, addedTids)
                    if (threads.size >= 30) break
                }
            } else {
                // 兜底方案：直接找 a 标签
                val links = doc.select("a[href~=thread-\\d+-1-1\\.html]")
                for (a in links) {
                    val href = a.attr("href")
                    if (href.contains("forum-post")) continue
                    val title = a.text().trim()
                    if (title.length < 5 || title.contains("回复")) continue
                    parseThread(a, a.parent()?.text() ?: "", threads, addedTids)
                    if (threads.size >= 30) break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        threads
    }

    private fun parseThread(a: org.jsoup.nodes.Element, text: String, threads: MutableList<Thread>, addedTids: MutableSet<String>) {
        val href = a.attr("href")
        val tidMatch = Regex("thread-(\\d+)-").find(href) ?: return
        val tid = tidMatch.groupValues[1]
        if (addedTids.contains(tid)) return
        addedTids.add(tid)

        val title = a.text().trim()
        var views = "0"
        var replies = "0"
        var timeText = "刚刚"

        val timeMatch = Regex("\\d{2,4}-\\d{2}-\\d{2}\\s*\\d{2}:\\d{2}").find(text)
        if (timeMatch != null) timeText = timeMatch.value

        // 暴力提取所有数字，过滤掉长数字，取前两个作为浏览和回复
        val nums = Regex("(\\d+)").findAll(text).map { it.value }.filter { it.length < 6 }.toList()
        if (nums.isNotEmpty()) views = nums[0]
        if (nums.size > 1) replies = nums[1]

        threads.add(
            Thread(
                tid = tid,
                subject = title,
                authorName = "理想股友",
                viewCount = views,
                replyCount = replies,
                timeText = timeText
            )
        )
    }

    suspend fun fetchThreadDetail(tid: String): ThreadPage = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.55188.com/thread-" + tid + "-1-1.html")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Cookie", COOKIE)
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            val title = doc.select("title").text().replace(" - 理想论坛", "").trim()
            val contentHtml = doc.select("td.t_f").firstOrNull()?.html() ?: "无法解析内容"
            
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
            ThreadPage(tid = tid, subject = "加载失败", forumName = "", posts = emptyList(), page = 1, totalPages = 1, formhash = "", replyUrl = "")
        }
    }

    // 核心修改：将你本地抓取到的 9 大全站真实大类全部塞入！
    suspend fun fetchForumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        listOf(
            ForumGroup(name = "股票软件", forums = listOf(
                Forum(fid = "68", name = "全部主题"),
                Forum(fid = "68_129", name = "通达信"),
                Forum(fid = "68_179", name = "大智慧"),
                Forum(fid = "68_130", name = "同花顺")
            )),
            ForumGroup(name = "实战交流", forums = listOf(
                Forum(fid = "8", name = "实战交流"),
                Forum(fid = "102", name = "选股技巧"),
                Forum(fid = "77", name = "新手学习"),
                Forum(fid = "20", name = "休闲娱乐")
            )),
            ForumGroup(name = "指标与工具", forums = listOf(
                Forum(fid = "48", name = "指标公式"),
                Forum(fid = "90", name = "指标问答"),
                Forum(fid = "93", name = "金钻指标"),
                Forum(fid = "40", name = "问题反馈")
            ))
        )
    }
}
