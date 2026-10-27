package com.aazhou.mtforum.ui.screen.profile

import androidx.compose.runtime.Composable

@Composable
fun ProfileScreen(
    // 保留原有参数以适应 MTF 路由
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMyPosts: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {}
) {
    // 直接渲染我们写死的理想论坛个人中心
    LxfProfileScreen()
}
