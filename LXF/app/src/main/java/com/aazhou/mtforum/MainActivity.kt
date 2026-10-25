package com.aazhou.mtforum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

// ==================== 数据模型 ====================
data class LxThread(
    val tid: String, val title: String, val author: String,
    val time: String, val tag: String, val viewCount: String, val replyCount: String
)

data class PostDetail(
    val title: String = "", val author: String = "", val time: String = "",
    val content: String = "", val replies: List<PostReply> = emptyList()
)

data class PostReply(
    val author: String, val time: String, val content: String, val floor: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MTFApp()
            }
        }
    }
}

// ==================== 网络请求 ====================
suspend fun fetchLxfThreads(): List<LxThread> = withContext(Dispatchers.IO) {
    val threads = mutableListOf<LxThread>()
    try {
        val url = URL("https://www.55188.com/")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestProperty("User-Agent", "Mozilla/5.0")
        val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
        val doc = Jsoup.parse(html)
        val elements = doc.select("a[href^=thread-]")
        val addedTids = mutableSetOf<String>()
        for (el in elements) {
            val href = el.attr("href")
            val title = el.text().trim()
            if (title.length < 5 || title == "搜索说明") continue
            val match = Regex("thread-(\\d+)-").find(href)
            val tid = match?.groupValues?.get(1) ?: continue
            if (tid in addedTids) continue
            addedTids.add(tid)
            threads.add(LxThread(tid, title, "理想股友", "刚刚", "综合交流", (100..9999).random().toString(), (1..100).random().toString()))
            if (threads.size >= 30) break
        }
    } catch (e: Exception) { e.printStackTrace() }
    threads
}

suspend fun fetchPostDetail(tid: String): PostDetail = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://www.55188.com/thread-$tid-1-1.html")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        val html = String(conn.inputStream.readBytes(), Charset.forName("GBK"))
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst("span#thread_subject")?.text() ?: "帖子详情"
        val postElements = doc.select("div[id^=post_]")
        val replies = mutableListOf<PostReply>()
        var mainContent = ""

        postElements.forEachIndexed { index, post ->
            val author = post.selectFirst(".authi .z a")?.text() ?: "匿名"
            val content = post.selectFirst("td.t_f")?.text() ?: "无内容"
            if (index == 0) mainContent = content else replies.add(PostReply(author, "刚刚", content, "${index + 1}楼"))
        }
        PostDetail(title = title, content = mainContent, replies = replies)
    } catch (e: Exception) {
        PostDetail(title = "加载失败", content = "网络异常")
    }
}

// ==================== 主框架与手势动画 ====================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MTFApp() {
    var currentBottomTab by remember { mutableIntStateOf(0) }
    var selectedTid by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            if (selectedTid == null) { // 只有在非详情页才显示底栏
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = currentBottomTab == 0, onClick = { currentBottomTab = 0 },
                        icon = { Icon(Icons.Filled.Home, null) }, label = { Text("首页") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00897B), selectedTextColor = Color(0xFF00897B),
                            indicatorColor = Color(0xFFE0F2F1) // MTF 标志性的胶囊背景
                        )
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 1, onClick = { currentBottomTab = 1 },
                        icon = { Icon(Icons.Filled.List, null) }, label = { Text("板块") }
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 2, onClick = { currentBottomTab = 2 },
                        icon = { Icon(Icons.Filled.Person, null) }, label = { Text("我的") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00897B), selectedTextColor = Color(0xFF00897B),
                            indicatorColor = Color(0xFFE0F2F1)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            // 使用 AnimatedContent 实现页面切换的平滑过渡
            AnimatedContent(
                targetState = selectedTid,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tid ->
                if (tid != null) {
                    PostDetailScreen(tid = tid, onBack = { selectedTid = null })
                } else {
                    when (currentBottomTab) {
                        0 -> LxHomeScreen(onThreadClick = { selectedTid = it })
                        1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("板块开发中") }
                        2 -> LxProfileScreen()
                    }
                }
            }
        }
    }
}

// ==================== 首页 UI ====================
@Composable
fun LxHomeScreen(onThreadClick: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("最新", "热门", "精华", "回复", "抢沙发")
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        threadList = fetchLxfThreads()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 恢复：顶部搜索框
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(36.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp)), contentAlignment = Alignment.CenterStart) {
                Text("🔍 搜索帖子、用户...", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = Color.Gray)
        }
        
        // 恢复：顶部 Tab 导航
        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.White, edgePadding = 16.dp, divider = {}) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 15.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(threadList) { thread -> MTFThreadCard(thread) { onThreadClick(thread.tid) } }
            }
        }
    }
}

// 恢复：完整的 MTF 卡片（带头像和标签）
@Composable
fun MTFThreadCard(thread: LxThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                    Text(if (thread.author.isNotEmpty()) thread.author.take(1) else "匿", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(thread.author, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(thread.time, fontSize = 11.sp, color = Color(0xFF999999))
                }
                if (thread.tag.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF5F5F5), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                        Text(thread.tag, fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(thread.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222), lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Text("点击查看帖子详情与讨论内容...", fontSize = 13.sp, color = Color(0xFF888888), maxLines = 1)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("浏览: ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.width(16.dp))
                Text("回复: ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}

// ==================== 详情页 UI ====================
@Composable
fun PostDetailScreen(tid: String, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<PostDetail?>(null) }
    LaunchedEffect(tid) { detail = fetchPostDetail(tid) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("帖子详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(detail!!.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(detail!!.content, fontSize = 15.sp, color = Color(0xFF333333), lineHeight = 24.sp)
                        }
                    }
                }
                items(detail!!.replies) { reply ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(reply.author, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                                Text(reply.floor, fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(reply.content, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== 个人中心 UI (恢复完整宫格) ====================
@Composable
fun LxProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 头部
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                Text("大", fontSize = 24.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("大海2010", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
        
        // 数据面板
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProfileStatItem("4493", "理想币", Color.Red)
            ProfileStatItem("174", "共享币", Color.Red)
            ProfileStatItem("3396", "分享值", Color.Red)
            ProfileStatItem("10", "金钻", Color.Red)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 宫格 (恢复到 4x4 那样完整的视觉)
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            val gridItems = listOf("共享币购买", "金钻购买", "短消息", "系统消息", "关注的主题", "关注的用户", "全民选股", "我的自选", "我的主题", "我的说吧", "我的收藏", "收藏排行")
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp), userScrollEnabled = false) {
                items(gridItems.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF7043), RoundedCornerShape(12.dp)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(gridItems[index], fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(num: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}
