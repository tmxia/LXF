package com.aazhou.mtforum.ui.screen.profile

import androidx.compose.runtime.Composable
import com.aazhou.mtforum.data.model.UserProfile
import androidx.compose.ui.Modifier

@Composable
fun ProfileScreen(
    onLogin: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onHistory: () -> Unit = {},
    onDrafts: () -> Unit = {},
    onMyPosts: () -> Unit = {},
    onMyFavorites: () -> Unit = {},
    onMyFriends: () -> Unit = {},
    onFollowList: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onSettings: () -> Unit = {},
    onMessages: () -> Unit = {}
) {
    // 核心：无视传进来的回调参数，直接渲染静态 UI
    LxfProfileScreen()
}

// 为了兼容 UserProfileScreen.kt 的报错，保留一个空的 ProfileContent 占位符
@Composable
fun ProfileContent(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    // 暂时留空，或者直接调用静态 UI
}
