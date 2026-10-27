package com.aazhou.mtforum.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aazhou.mtforum.data.model.UserProfile

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
    LxfProfileScreen()
}

// 补全 UserProfileScreen.kt 报错缺失的 ProfileContent 占位符
@Composable
fun ProfileContent(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    // 查看他人主页时，目前直接复用静态 UI 或留空，保证编译通过
    LxfProfileScreen()
}
