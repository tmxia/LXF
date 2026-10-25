package com.aazhou.mtforum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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


data class LxThread(
    val tid: String,
    val title: String,
    val author: String,
    val time: String,
    val tag: String,
    val viewCount: String,
    val replyCount: String
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MTFApp()
        }
    }
}


suspend fun fetchLxfThreads(): List<LxThread> {
    // TODO: 这里放你原本的网络抓取和 GBK 解析逻辑
    return listOf(
        LxThread("1", "如果看均线如何判断个股是否即将突破？", "徒然一身", "22分钟前", "综合交流", "120", "5"),
        LxThread("2", "如何看分时图买卖点，哪些细节是新手容易忽视的？", "清风师", "2小时前", "股票池", "89", "2")
    )
}


@Composable
fun MTFApp() {
    var currentBottomTab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = currentBottomTab == 0, onClick = { currentBottomTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("首页") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00897B), selectedTextColor = Color(0xFF00897B))
                )
                NavigationBarItem(
                    selected = currentBottomTab == 1, onClick = { currentBottomTab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) }, label = { Text("板块") }
                )
                NavigationBarItem(
                    selected = currentBottomTab == 2, onClick = { currentBottomTab = 2 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) }, label = { Text("我的") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            when (currentBottomTab) {
                0 -> LxHomeScreen()
                1 -> LxBoardScreen()
                2 -> LxProfileScreen()
            }
        }
    }
}

@Composable
fun LxHomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("最新", "热门", "精华", "回复", "抢沙发")
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        threadList = fetchLxfThreads()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(36.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp)), contentAlignment = Alignment.CenterStart) {
                Text("🔍 搜索帖子、用户...", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = Color.Gray)
        }
        
        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.White, edgePadding = 16.dp, divider = {}) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontSize = 15.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(threadList) { thread -> MTFThreadCard(thread) }
            }
        }
    }
}

@Composable
fun LxProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                Text("大", fontSize = 24.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("大海2010", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProfileStatItem("4493", "理想币", Color.Red)
            ProfileStatItem("174", "共享币", Color.Red)
            ProfileStatItem("3396", "分享值", Color.Red)
            ProfileStatItem("10", "金钻", Color.Red)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            val gridItems = listOf("共享币购买", "金钻购买", "短消息", "系统消息", "关注的主题", "关注的用户", "全民选股", "我的自选", "我的主题", "我的说吧", "我的收藏", "收藏排行")
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(240.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(gridItems.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF7043), RoundedCornerShape(8.dp)))
                        Spacer(modifier = Modifier.height(4.dp))
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

@Composable
fun LxBoardScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("板块开发中...", color = Color.Gray) }
}

@Composable
fun MTFThreadCard(thread: LxThread) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(12.dp)) {
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
