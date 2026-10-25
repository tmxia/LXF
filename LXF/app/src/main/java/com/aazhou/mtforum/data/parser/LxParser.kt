package com.aazhou.mtforum.data.parser

import com.aazhou.mtforum.data.model.LxThread
import org.jsoup.Jsoup

object LxParser {
    fun parseThreadList(html: String): List<LxThread> {
        val doc = Jsoup.parse(html)
        val threads = mutableListOf<LxThread>()
        val uniqueTids = mutableSetOf<String>()

        
        val elements = doc.select("a[href^=thread-]")
        for (el in elements) {
            val href = el.attr("href")
            val title = el.text().trim()
            
            if (title.isNotEmpty() && !title.contains("thread-") && title.length > 3) {
                try {
                    val tid = href.substringAfter("thread-").substringBefore("-")
                    if (uniqueTids.add(tid)) {
                        
                        var container = el.parent()
                        for (i in 0 until 3) {
                            if (container?.tagName() == "tbody" || container?.tagName() == "tr" || container?.hasClass("thread-item") == true) break
                            container = container?.parent()
                        }

                        val author = container?.selectFirst("a[href*='space-uid']")?.text() ?: "未知"
                        val tag = el.parent()?.selectFirst("em, span.tag")?.text() ?: ""
                        
                        
                        val spans = container?.select("span")?.map { it.text() } ?: emptyList()
                        val time = spans.firstOrNull { it.contains("-") || it.contains("小时前") } ?: ""
                        val views = container?.select("a[href*='mod=viewthread']")?.text()?.filter { it.isDigit() } ?: "0"
                        val replies = container?.select("a[href*='mod=redirect']")?.text()?.filter { it.isDigit() } ?: "0"

                        threads.add(
                            LxThread(
                                tid = tid,
                                title = title,
                                author = author,
                                time = if (time.isBlank()) "刚刚" else time,
                                viewCount = if (views.isBlank()) "0" else views,
                                replyCount = if (replies.isBlank()) "0" else replies,
                                tag = tag,
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
