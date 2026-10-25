package com.aazhou.mtforum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    LxForumScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LxForumScreen() {
    var threadList by remember { mutableStateOf<List<LxThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // fid=68 是软件数据教程版块
            threadList = LxRepository.getCachedOrFetchThreads("68")
        } catch (e: Exception) {
            errorMsg = e.message
        } finally {
            isLoading = false
        }
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
            } else if (errorMsg != null) {
                Text(text = "加载失败：$errorMsg", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(threadList) { thread ->
                        ThreadCard(thread)
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadCard(thread: LxThread) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题和标签
            Row(verticalAlignment = Alignment.Top) {
                if (thread.tag.isNotBlank()) {
                    Text(
                        text = "[${thread.tag}] ",
                        color = Color(0xFF1976D2),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = thread.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 底部数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = thread.author, fontSize = 12.sp, color = Color(0xFF666666))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = thread.time, fontSize = 12.sp, color = Color(0xFF999999))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "查看: ${thread.viewCount}", fontSize = 12.sp, color = Color(0xFF999999))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "回复: ${thread.replyCount}", fontSize = 12.sp, color = Color(0xFF999999))
                }
            }
        }
    }
}
