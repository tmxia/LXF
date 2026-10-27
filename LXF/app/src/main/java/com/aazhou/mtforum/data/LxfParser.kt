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
            // 强制使用 68（股票软件板块）
            val url = URL("https://www.55188.com/forum-68-1.html")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Cookie", "HMACCOUNT=F3FD580504BD7E15; Hm_lpvt_0233b908b2bf29bfd788abd7ee3556da=1757035032; 55188_passport=kn9qGAdENqKqk9ZT%2B%2FWjDVP35XMgGU142hHt6Ui3%2F9zKlODtT6%2F2%2B%2BoqX38%2FYp%2F4hg4hK7ss26VuIVFTpcvK%2BOOz%2FCvwqmKKSStJjyp%2FYnZQHjsm1NIFcGkjPvkyZkTmqZntqnYuN%2BuUJH%2B4qfsL2cST%2F2tMPu29b27w%2FXQF1ak%3D; passport2bbs=gXfzvUFx5Yx567ZhgndUtE89%2F81R6s5TCwZUBHERqfjVS%2FHg1lbEh%2FKGNJI7x1dK; cdb2_auth=jiqsgmbJXdBFKY8Asxbfh26nwQIv%2BJpbCF%2F01k%2BELaRiGSsEFr%2BxBzq%2BVeirpKA0; vOVx_56cc_saltkey=aLJSxMVL; vOVx_56cc_lastvisit=1792927840; vOVx_56cc_auth=b651Jm4PoQPCrseWTLmKlXp4zb8jVErgIwlAgbGXSi8pm42VFPmi788YjNWvibki6GzDbVKMY%2BxCC%2FC5UYkNjzlTAXo; vOVx_56cc_home_diymode=1; vOVx_56cc_atarget=1; attack_pass=pyh0VRLAJ0gLuaQMOiHLxQ%2C1793167318; vOVx_56cc_visitedfid=68D90D48D93D77D8D20D102; vOVx_56cc_sid=c9HHt7; vOVx_56cc_lip=49.118.86.233%2C1793080918; vOVx_56cc_lastact=1793104148%09forum.php%09forumdisplay; vOVx_56cc_st_t=784998%7C1793104148%7C158dfd241300e62b8417234e70a6fe54; vOVx_56cc_forum_lastvisit=D_102_1793002691D_20_1793002694D_8_1793002717D_77_1793002722D_93_1793002727D_48_1793002736D_90_1793080918D_68_1793104148; vOVx_56cc_ulastactivity=608b5Iwg34O0vA0kJ0gHcUd9ZjTiG2S6yQgcUnXnFTTQmY%2F2GD2R")
            
            val html = String(conn.inputStream.readBytes(), Charset.forName("UTF-8"))
            val doc = Jsoup.parse(html)
            
            // 直接锁定真实的帖子区域
            val articles = doc.select("article.thread-item")
            for (article in articles) {
                // 排除带有 zd（置顶）的全局公告
                if (article.classNames().contains("zd")) continue
                
                val a = article.selectFirst("a.subject") ?: continue
                val href = a.attr("href")
                val tidMatch = Regex("thread-(\\d+)-").find(href) ?: continue
                val tid = tidMatch.groupValues[1]
                val title = a.text().trim()
                
                // 提取卡片内所有文本，用正则捞取数据
                val text = article.text()
                val viewsMatch = Regex("(\\d+)\\s*(?:阅|浏览)").find(text)
                val repliesMatch = Regex("(\\d+)\\s*回复").find(text)
                val timeMatch = Regex("\\d{2,4}-\\d{2}-\\d{2}\\s*\\d{2}:\\d{2}").find(text)
                
                threads.add(
                    Thread(
                        tid = tid,
                        subject = title,
                        authorName = "理想股友",
                        viewCount = viewsMatch?.groupValues?.get(1) ?: "0",
                        replyCount = repliesMatch?.groupValues?.get(1) ?: "0",
                        timeText = timeMatch?.value ?: "刚刚"
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
            conn.setRequestProperty("Cookie", "HMACCOUNT=F3FD580504BD7E15; Hm_lpvt_0233b908b2bf29bfd788abd7ee3556da=1757035032; 55188_passport=kn9qGAdENqKqk9ZT%2B%2FWjDVP35XMgGU142hHt6Ui3%2F9zKlODtT6%2F2%2B%2BoqX38%2FYp%2F4hg4hK7ss26VuIVFTpcvK%2BOOz%2FCvwqmKKSStJjyp%2FYnZQHjsm1NIFcGkjPvkyZkTmqZntqnYuN%2BuUJH%2B4qfsL2cST%2F2tMPu29b27w%2FXQF1ak%3D; passport2bbs=gXfzvUFx5Yx567ZhgndUtE89%2F81R6s5TCwZUBHERqfjVS%2FHg1lbEh%2FKGNJI7x1dK; cdb2_auth=jiqsgmbJXdBFKY8Asxbfh26nwQIv%2BJpbCF%2F01k%2BELaRiGSsEFr%2BxBzq%2BVeirpKA0; vOVx_56cc_saltkey=aLJSxMVL; vOVx_56cc_lastvisit=1792927840; vOVx_56cc_auth=b651Jm4PoQPCrseWTLmKlXp4zb8jVErgIwlAgbGXSi8pm42VFPmi788YjNWvibki6GzDbVKMY%2BxCC%2FC5UYkNjzlTAXo; vOVx_56cc_home_diymode=1; vOVx_56cc_atarget=1; attack_pass=pyh0VRLAJ0gLuaQMOiHLxQ%2C1793167318; vOVx_56cc_visitedfid=68D90D48D93D77D8D20D102; vOVx_56cc_sid=c9HHt7; vOVx_56cc_lip=49.118.86.233%2C1793080918; vOVx_56cc_lastact=1793104148%09forum.php%09forumdisplay; vOVx_56cc_st_t=784998%7C1793104148%7C158dfd241300e62b8417234e70a6fe54; vOVx_56cc_forum_lastvisit=D_102_1793002691D_20_1793002694D_8_1793002717D_77_1793002722D_93_1793002727D_48_1793002736D_90_1793080918D_68_1793104148; vOVx_56cc_ulastactivity=608b5Iwg34O0vA0kJ0gHcUd9ZjTiG2S6yQgcUnXnFTTQmY%2F2GD2R")
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

    suspend fun fetchForumGroups(): List<ForumGroup> = withContext(Dispatchers.IO) {
        listOf(
            ForumGroup(name = "股票软件", forums = listOf(
                Forum(fid = "68", name = "全部主题"),
                Forum(fid = "68_129", name = "通达信"),
                Forum(fid = "68_179", name = "大智慧"),
                Forum(fid = "68_130", name = "同花顺")
            ))
        )
    }
}
