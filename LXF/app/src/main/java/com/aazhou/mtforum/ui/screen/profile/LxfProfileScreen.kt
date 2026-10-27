package com.aazhou.mtforum.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LxfProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        // 头部用户信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Text("大", color = Color(0xFF00796B), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("大海2010", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }

        // 数据面板
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("4493个", "理想币")
            StatItem("174个", "共享币")
            StatItem("3396个", "分享值")
            StatItem("10颗", "金钻")
        }

        // 竞猜进度条
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(0.25f).height(20.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(Color(0xFF4CAF50))
                )
                Box(
                    modifier = Modifier.weight(0.75f).height(20.dp)
                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                        .background(Color(0xFFD32F2F))
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("3人看跌", color = Color.Gray, fontSize = 12.sp)
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("参加竞猜", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("9人看涨", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 宫格菜单
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp).height(400.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val menus = listOf(
                "共享币购买" to Icons.Default.ShoppingCart,
                "金钻购买" to Icons.Default.ShoppingCart,
                "短消息" to Icons.Default.Email,
                "系统消息" to Icons.Default.Notifications,
                "关注的主题" to Icons.Default.Star,
                "关注的用户" to Icons.Default.Person,
                "全民选股" to Icons.Default.List,
                "我的自选" to Icons.Default.Favorite,
                "我的主题" to Icons.Default.Edit,
                "我的说吧" to Icons.Default.Home,
                "我的收藏" to Icons.Default.Star,
                "收藏排行" to Icons.Default.List,
                "我的订阅" to Icons.Default.CheckCircle,
                "我加入的驿站" to Icons.Default.Home,
                "设置" to Icons.Default.Settings,
                "微信消息设置" to Icons.Default.Email,
                "我的任务" to Icons.Default.List,
                "金钻指标" to Icons.Default.Star,
                "消费明细" to Icons.Default.List,
                "悬赏任务" to Icons.Default.Star
            )
            
            items(menus.size) { index ->
                val (name, icon) = menus[index]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = name, tint = Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(name, fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}
