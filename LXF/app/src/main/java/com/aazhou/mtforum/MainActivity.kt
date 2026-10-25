package com.aazhou.mtforum

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 纯原生 TextView，彻底排除 Compose 初始化导致的崩溃
        val textView = TextView(this).apply {
            text = "理想论坛原生测试成功！"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        setContentView(textView)
    }
}
