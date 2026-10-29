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

    suspend fun fetchThreads(view: String): List<Thread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<Thread>()
        try {
            // 强制首页默认走 68，如果传了纯数字就走对应 fid
            var actualFid = "68"
            var typeId: String? = null
            
            if (view.contains("_")) {
                val parts = view.split("_")
                actualFid = parts[0]
                typeId = parts.getOrNull(1)
            } else if (view.isNotEmpty() && view.all { it.isDigit() }) {
                actualFid = view
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
            
            for (article in articles) {
                if (article.classNames().contains("zd")) continue
                
                val a = article.selectFirst("a.subject") ?: continue
                val href = a.attr("href")
                val tidMatch = Regex("thread-(\\d+)-").find(href) ?: continue
                val tid = tidMatch.groupValues[1]
                if (addedTids.contains(tid)) continue
                
                val title = a.text().trim()
                if (title.length < 5) continue

                var timeText = "刚刚"
                val timeSpan = article.selectFirst(".author-name p span")
                if (timeSpan != null) timeText = timeSpan.text().trim()

                var views = "0"
                var replies = "0"
                val footer = article.selectFirst("footer.item-footer")
                if (footer != null) {
                    val footerText = footer.text()
                    val viewMatch = Regex("查看\\s*([\\d\\.]+万?)").find(footerText)
                    if (viewMatch != null) views = viewMatch.groupValues[1]
                    val replyMatch = Regex("回复\\s*(\\d+)").find(footerText)
                    if (replyMatch != null) replies = replyMatch.groupValues[1]
                }
                
                addedTids.add(tid)
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
            conn.setRequestProperty("Cookie", COOKIE)
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
            ThreadPage(tid = tid, subject = "加载失败", forumName = "", posts = emptyList(), page = 1, totalPages = 1, formhash = "", replyUrl = "")
        }
    }

    suspend fun fetchForumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        listOf(
            ForumGroup(name = "股票软件", forums = listOf(
                Forum(fid = "68", name = "全部主题"),
                Forum(fid = "68_88", name = "大智慧"),
                Forum(fid = "68_84", name = "通达信"),
                Forum(fid = "68_86", name = "同花顺")
            )),
            ForumGroup(name = "核心板块", forums = listOf(
                Forum(fid = "48", name = "指标公式"),
                Forum(fid = "90", name = "指标问答"),
                Forum(fid = "93", name = "金钻指标")
            ))
        )
    }
}
