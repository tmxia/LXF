package com.aazhou.mtforum

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
import androidx.compose.material.icons.outlined.*
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

import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Surface
import java.nio.charset.Charset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aazhou.mtforum.data.model.LxThread
import com.aazhou.mtforum.data.repo.LxRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MTFApp() }
            }
        }
    }
}

@Composable
fun LxApp() {
    var currentTid by remember { mutableStateOf<String?>(null) }
    if (currentTid == null) {
        LxForumScreen(onThreadClick = { tid -> currentTid = tid })
    } else {
        LxDetailScreen(tid = currentTid!!, onBack = { currentTid = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LxForumScreen(onThreadClick: (String) -> Unit) {
    val context = LocalContext.current
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 首次加载逻辑
    LaunchedEffect(Unit) {
        isLoading = true
        // 先尝试读本地缓存
        val cached = LxRepository.getCachedThreads(context, "68")
        if (cached.isNotEmpty()) {
            threadList = cached
            isLoading = false
        } else {
            // 如果没缓存，直接并发全量下载
            isDownloading = true
            threadList = LxRepository.batchDownloadAndCache(context, "68")
            isDownloading = false
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("理想论坛 · 本地全量版", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                actions = {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                isDownloading = true
                                threadList = LxRepository.batchDownloadAndCache(context, "68")
                                isDownloading = false
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "全量同步", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在并发下载前3页数据...", fontSize = 14.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(threadList) { thread ->
                        ThreadCard(thread, onClick = { onThreadClick(thread.tid) })
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadCard(thread: LxThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 第一行：头像、作者、时间、右侧标签
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // 模拟圆形头像 (取作者名字第一个字)
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0F2F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (thread.author.isNotEmpty()) thread.author.take(1) else "匿",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = thread.author, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Text(text = thread.time, fontSize = 11.sp, color = Color(0xFF999999))
                }
                
                // 右侧标签
                if (thread.tag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Text(
                            text = thread.tag,
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 第二行：帖子标题
            Text(
                text = thread.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 第三行：摘要占位
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击查看帖子详情与讨论内容...",
                fontSize = 13.sp,
                color = Color(0xFF888888),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 第四行：底部互动数据图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Visibility, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(text = " ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(text = " ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LxDetailScreen(tid: String, onBack: () -> Unit) {
    var title by remember { mutableStateOf("加载中...") }
    var content by remember { mutableStateOf("正在抓取正文内容...") }
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    LaunchedEffect(tid) {
        isLoading = true
        try {
            val result = LxRepository.fetchThreadDetail(tid)
            title = result.first
            content = result.second
        } catch (e: Exception) {
            title = "加载失败"
            content = e.message ?: "未知错误"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935))
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = content, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 24.sp)
            }
        }
    }
}


// ==================== MTF 适配框架 UI ====================

@Composable
fun MTFApp() {
    var currentBottomTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = currentBottomTab == 0,
                    onClick = { currentBottomTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00897B), selectedTextColor = Color(0xFF00897B))
                )
                NavigationBarItem(
                    selected = currentBottomTab == 1,
                    onClick = { currentBottomTab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = "板块") },
                    label = { Text("板块") }
                )
                NavigationBarItem(
                    selected = currentBottomTab == 2,
                    onClick = { currentBottomTab = 2 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                    label = { Text("我的") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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

    // TODO: 你的网络请求逻辑，这里保留你原有的抓取逻辑，假设在 LaunchedEffect 里
    LaunchedEffect(Unit) {
        // 请确保你的 fetchData 替换在这里
        // 示例: threadList = fetchLxfThreads()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // 顶部搜索栏与功能图标
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).height(36.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("🔍 搜索帖子、用户...", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = Color.Gray)
        }

        // 顶部 Tab 栏
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 15.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 帖子列表
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(threadList) { thread ->
                    MTFThreadCard(thread)
                }
            }
        }
    }
}

@Composable
fun LxProfileScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
        // 顶部用户信息区
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                Text("大", fontSize = 24.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("大海2010", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }

        // 数据面板
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatItem("4493", "理想币", Color.Red)
            ProfileStatItem("174", "共享币", Color.Red)
            ProfileStatItem("3396", "分享值", Color.Red)
            ProfileStatItem("10", "金钻", Color.Red)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 宫格功能区 (模拟)
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            val gridItems = listOf("共享币购买", "金钻购买", "短消息", "系统消息", "关注的主题", "关注的用户", "全民选股", "我的自选", "我的主题", "我的说吧", "我的收藏", "收藏排行")
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(240.dp), // 固定高度防冲突
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(gridItems.size) { index ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF7043), RoundedCornerShape(8.dp))) // 橙色占位
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(gridItems[index], fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 底部最新主题
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Text("最新主题", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            Text("如果看均线如何判断个股是否即将突破？", fontSize = 14.sp, maxLines = 1)
            // ... 可以继续添加多条
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("板块页面开发中...", color = Color.Gray)
    }
}

@Composable
fun MTFThreadCard(thread: LxThread) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
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
                Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(" ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(" ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}


// ==================== MTF 适配框架 UI ====================
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
    // 这里保留你原有的列表变量
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    
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
        
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(threadList) { thread -> MTFThreadCard(thread) }
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
                Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(" ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                Text(" ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}
