package com.aazhou.mtforum.ui.screen.profile

import androidx.compose.runtime.Composable

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
    // 核心：直接挂载写好的静态 UI
    LxfProfileScreen()
}
