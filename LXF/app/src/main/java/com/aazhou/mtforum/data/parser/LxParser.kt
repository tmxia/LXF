package com.aazhou.mtforum.data.parser

import com.aazhou.mtforum.data.model.LxThread
import org.jsoup.Jsoup

object LxParser {
    fun parseThreadList(html: String): List<LxThread> {
        val doc = Jsoup.parse(html)
        val threads = mutableListOf<LxThread>()
        val uniqueTids = mutableSetOf<String>()

        // 匹配所有 thread-xxxxx-1-1.html 格式的链接
        val elements = doc.select("a[href^=thread-]")
        for (el in elements) {
            val href = el.attr("href")
            val title = el.text().trim()
            if (title.isNotEmpty() && !title.contains("thread-") && title.length > 3) {
                try {
                    val tid = href.substringAfter("thread-").substringBefore("-")
                    if (uniqueTids.add(tid)) {
                        // 尝试在附近找作者，找不到就填未知
                        val author = el.parent()?.parent()?.select("a[href^=space-uid]")?.text() ?: "未知"
                        threads.add(
                            LxThread(
                                tid = tid,
                                title = title,
                                author = author,
                                url = "https://www.55188.com/$href"
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }
        return threads
    }
}
