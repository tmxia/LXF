package com.aazhou.mtforum

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aazhou.mtforum.data.model.LxThread
import com.aazhou.mtforum.data.repo.LxRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F6F8)) {
                    LxApp()
                }
            }
        }
    }
}

@Composable
fun LxApp() {
    // 状态：当前正在查看的 tid，如果为 null 则显示列表
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
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            threadList = LxRepository.getCachedOrFetchThreads("68")
        } catch (e: Exception) {} finally { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("理想论坛 · 软件数据教程", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE53935))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // 绑定点击事件
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (thread.tag.isNotBlank()) {
                    Text(text = "[${thread.tag}] ", color = Color(0xFF1976D2), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = thread.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222), lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text(text = thread.author, fontSize = 12.sp, color = Color(0xFF666666))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = thread.time, fontSize = 12.sp, color = Color(0xFF999999))
                }
                Row {
                    Text(text = "查看: ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "回复: ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
                }
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
                title = { Text("帖子详情", color = Color.White, fontWeight = FontWeight.Bold) },
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
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = content, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 24.sp)
            }
        }
    }
}
