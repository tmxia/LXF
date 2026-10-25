package com.aazhou.mtforum.data.parser

import com.aazhou.mtforum.data.model.LxThread
import org.jsoup.Jsoup

object LxParser {
    fun parseThreadList(html: String): List<LxThread> {
        val doc = Jsoup.parse(html)
        val threads = mutableListOf<LxThread>()
        val uniqueTids = mutableSetOf<String>()

        // 找到所有包含 thread- 的 a 标签
        val elements = doc.select("a[href^=thread-]")
        for (el in elements) {
            val href = el.attr("href")
            val title = el.text().trim()
            
            if (title.isNotEmpty() && !title.contains("thread-") && title.length > 3) {
                try {
                    val tid = href.substringAfter("thread-").substringBefore("-")
                    if (uniqueTids.add(tid)) {
                        // 核心修复：向上寻找包含作者信息的容器
                        // Discuz 的帖子列表通常有 tbody[id^=normalthread] 或类似的包裹结构
                        var author = "未知"
                        
                        // 尝试在当前链接的父级、祖父级中寻找指向用户空间(space-uid)的链接
                        val container = el.parent()?.parent()
                        if (container != null) {
                            val authorEl = container.selectFirst("a[href*='space-uid'], cite a")
                            if (authorEl != null) {
                                author = authorEl.text().trim()
                            }
                        }

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
