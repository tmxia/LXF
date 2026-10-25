package com.aazhou.mtforum

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import org.jsoup.Jsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

// ==================== 数据模型 ====================
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
            MaterialTheme {
                MTFApp()
            }
        }
    }
}

// ==================== 网络与登录逻辑 ====================
object CookieManager {
    val cookieStore = mutableMapOf<String, List<Cookie>>()
    val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
}

suspend fun fetchLxfThreads(): List<LxThread> = withContext(Dispatchers.IO) {
    val threads = mutableListOf<LxThread>()
    try {
        val url = URL("https://www.55188.com/")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        val bytes = conn.inputStream.readBytes()
        val html = String(bytes, Charset.forName("GBK"))
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
            threads.add(
                LxThread(
                    tid = tid, title = title, author = "理想股友", time = "刚刚", tag = "最新资讯",
                    viewCount = (100..9999).random().toString(), replyCount = (1..100).random().toString()
                )
            )
            if (threads.size >= 30) break
        }
    } catch (e: Exception) { e.printStackTrace() }
    threads
}

suspend fun fetchCaptchaImage(): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder().url("https://passport.55188.com/index/validate/login").build()
        val resp = CookieManager.client.newCall(req).execute()
        resp.body?.bytes()
    } catch (e: Exception) { null }
}

suspend fun performLogin(username: String, password: String, captcha: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = "username=$username&password=$password&validate=$captcha&cookietime=2592000&loginsubmit=%E7%AB%8B%E5%8D%B3%E7%99%BB%E5%BD%95".toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val req = Request.Builder()
            .url("https://passport.55188.com/index/login")
            .post(body)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
            .header("Referer", "https://passport.55188.com/index/login")
            .build()
        val resp = CookieManager.client.newCall(req).execute()
        resp.isSuccessful
    } catch (e: Exception) { false }
}

// ==================== UI 主框架 ====================
@Composable
fun MTFApp() {
    var currentBottomTab by remember { mutableIntStateOf(0) }
    var selectedTid by remember { mutableStateOf<String?>(null) }

    if (selectedTid != null) {
        PostDetailScreen(tid = selectedTid!!, onBack = { selectedTid = null })
    } else {
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
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) }, label = { Text("我的") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00897B), selectedTextColor = Color(0xFF00897B))
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
                when (currentBottomTab) {
                    0 -> LxHomeScreen(onThreadClick = { tid -> selectedTid = tid })
                    1 -> LxBoardScreen()
                    2 -> LxProfileScreen()
                }
            }
        }
    }
}

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
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(36.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp)), contentAlignment = Alignment.CenterStart) {
                Text("🔍 搜索帖子、用户...", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
            }
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
                items(threadList) { thread -> 
                    MTFThreadCard(thread) { onThreadClick(thread.tid) } 
                }
            }
        }
    }
}

// ==================== 帖子详情页面 ====================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PostDetailScreen(tid: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 顶部导航栏
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE53935)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text("帖子详情", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        // WebView 加载真实网页
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.defaultTextEncodingName = "GBK"
                    webViewClient = WebViewClient()
                    loadUrl("https://www.55188.com/thread-$tid-1-1.html")
                }
            }
        )
    }
}

// ==================== 个人中心与登录页面 ====================
@Composable
fun LxProfileScreen() {
    var showLoginDialog by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp).clickable { if (!isLoggedIn) showLoginDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0F2F1)), contentAlignment = Alignment.Center) {
                Text(if (isLoggedIn) "大" else "未", fontSize = 24.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(if (isLoggedIn) "大海2010" else "点击登录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
        
        // 数据面板
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProfileStatItem(if (isLoggedIn) "4493" else "-", "理想币", Color.Red)
            ProfileStatItem(if (isLoggedIn) "174" else "-", "共享币", Color.Red)
            ProfileStatItem(if (isLoggedIn) "3396" else "-", "分享值", Color.Red)
            ProfileStatItem(if (isLoggedIn) "10" else "-", "金钻", Color.Red)
        }
        
        // 功能宫格 (代码过长，省略部分占位)
        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Text("常用功能", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ProfileGridItem("共享币购买")
                ProfileGridItem("金钻购买")
                ProfileGridItem("短消息")
                ProfileGridItem("系统消息")
            }
        }
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { 
                isLoggedIn = true
                showLoginDialog = false 
            }
        )
    }
}

@Composable
fun LoginDialog(onDismiss: () -> Unit, onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var captchaBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    var isLoggingIn by remember { mutableStateOf(false) }

    fun loadCaptcha() {
        scope.launch {
            captchaBytes = fetchCaptchaImage()
        }
    }

    LaunchedEffect(Unit) { loadCaptcha() }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("登录理想论坛", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("账号") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = captcha, onValueChange = { captcha = it }, label = { Text("验证码") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(80.dp, 40.dp).background(Color.LightGray).clickable { loadCaptcha() }, contentAlignment = Alignment.Center) {
                        if (captchaBytes != null) {
                            val bitmap = BitmapFactory.decodeByteArray(captchaBytes, 0, captchaBytes!!.size)
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "验证码", modifier = Modifier.fillMaxSize())
                        } else {
                            Text("加载中", fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isLoggingIn = true
                        scope.launch {
                            val success = performLogin(username, password, captcha)
                            isLoggingIn = false
                            if (success) onLoginSuccess() else loadCaptcha() // 失败刷新验证码
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoggingIn && username.isNotBlank() && password.isNotBlank() && captcha.isNotBlank()
                ) {
                    Text(if (isLoggingIn) "登录中..." else "登录")
                }
            }
        }
    }
}

// ==================== 辅助组件 ====================
@Composable
fun ProfileStatItem(num: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileGridItem(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF7043), RoundedCornerShape(8.dp)))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 11.sp, color = Color.DarkGray)
    }
}

@Composable
fun LxBoardScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("板块开发中...", color = Color.Gray) }
}

@Composable
fun MTFThreadCard(thread: LxThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, // 增加点击事件
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
