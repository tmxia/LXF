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
        return@withContext listOf(
            ForumGroup(name = "股票软件", forums = listOf(
                Forum(fid = "68", name = "全部主题"),
                Forum(fid = "68_88", name = "[大智慧]"),
                Forum(fid = "68_84", name = "[通达信]"),
                Forum(fid = "68_86", name = "同花顺"),
                Forum(fid = "68_91", name = "东财通"),
                Forum(fid = "68_80", name = "飞　狐"),
                Forum(fid = "68_85", name = "插　件"),
                Forum(fid = "68_87", name = "[股票池]"),
                Forum(fid = "68_92", name = "AI编程"),
                Forum(fid = "68_81", name = "教　程"),
                Forum(fid = "68_89", name = "[小工具]"),
                Forum(fid = "68_82", name = "[数　据]"),
                Forum(fid = "68_90", name = "互　助"),
                Forum(fid = "68_83", name = "[其　它]"),
            )),
            ForumGroup(name = "指标公式", forums = listOf(
                Forum(fid = "48", name = "全部主题"),
                Forum(fid = "48_58", name = "[通达信]"),
                Forum(fid = "48_59", name = "同花顺"),
                Forum(fid = "48_60", name = "大智慧"),
                Forum(fid = "48_61", name = "东财通"),
                Forum(fid = "48_54", name = "飞　狐"),
                Forum(fid = "48_167", name = "益　盟"),
                Forum(fid = "48_166", name = "文　华"),
                Forum(fid = "48_57", name = "[其　他]"),
            )),
            ForumGroup(name = "指标问答", forums = listOf(
                Forum(fid = "90", name = "全部主题"),
                Forum(fid = "90_130", name = "[通达信]"),
                Forum(fid = "90_132", name = "[大智慧]"),
                Forum(fid = "90_131", name = "[同花顺]"),
                Forum(fid = "90_133", name = "东财通"),
                Forum(fid = "90_127", name = "[飞　狐]"),
                Forum(fid = "90_179", name = "改　写"),
                Forum(fid = "90_129", name = "[其　他]"),
            )),
            ForumGroup(name = "金钻指标", forums = listOf(
                Forum(fid = "93", name = "全部主题"),
                Forum(fid = "93_138", name = "通达信"),
                Forum(fid = "93_141", name = "大智慧"),
                Forum(fid = "93_139", name = "同花顺"),
                Forum(fid = "93_140", name = "股票池"),
                Forum(fid = "93_142", name = "东财通"),
                Forum(fid = "93_136", name = "钱　龙"),
                Forum(fid = "93_135", name = "分析家"),
                Forum(fid = "93_134", name = "飞　狐"),
                Forum(fid = "93_137", name = "其　他"),
            )),
            ForumGroup(name = "实战交流", forums = listOf(
                Forum(fid = "8", name = "全部主题"),
                Forum(fid = "8_2", name = "[看盘]"),
                Forum(fid = "8_7", name = "新闻"),
                Forum(fid = "8_9", name = "[原创]"),
                Forum(fid = "8_8", name = "[实盘]"),
                Forum(fid = "8_4", name = "[个股]"),
                Forum(fid = "8_3", name = "技巧"),
                Forum(fid = "8_11", name = "概念"),
                Forum(fid = "8_13", name = "[闲聊]"),
                Forum(fid = "8_159", name = "[缠论]"),
                Forum(fid = "8_1", name = "其他"),
            )),
            ForumGroup(name = "选股技巧", forums = listOf(
                Forum(fid = "102", name = "全部主题"),
                Forum(fid = "102_150", name = "[技巧]"),
                Forum(fid = "102_156", name = "[知识]"),
            )),
            ForumGroup(name = "新手学习", forums = listOf(
                Forum(fid = "77", name = "全部主题"),
            )),
            ForumGroup(name = "休闲娱乐", forums = listOf(
                Forum(fid = "20", name = "全部主题"),
            )),
            ForumGroup(name = "问题反馈", forums = listOf(
                Forum(fid = "40", name = "全部主题"),
                Forum(fid = "40_44", name = "[求助]"),
                Forum(fid = "40_45", name = "[投诉]"),
                Forum(fid = "40_46", name = "[建议]"),
                Forum(fid = "40_158", name = "[公告]"),
                Forum(fid = "40_47", name = "[已答复]"),
                Forum(fid = "40_43", name = "已解决"),
            )),
        )
    }
}
