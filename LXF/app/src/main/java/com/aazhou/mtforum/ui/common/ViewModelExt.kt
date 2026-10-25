package com.aazhou.lxforum.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aazhou.lxforum.AppContainer
import com.aazhou.lxforum.MtfApp

fun Context.container(): AppContainer = (applicationContext as MtfApp).container


@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalContext.current.container()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
