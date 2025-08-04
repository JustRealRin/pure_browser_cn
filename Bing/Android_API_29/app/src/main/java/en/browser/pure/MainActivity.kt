package cn.browser.pure

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import android.graphics.drawable.Drawable
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebChromeClient
import android.content.Intent
import android.net.Uri
import java.io.File

class MainActivity : AppCompatActivity() {
    private val homepage = "file:///android_asset/index.html"
    private data class Tab(val webView: WebView, var url: String)
    private val tabs = mutableListOf<Tab>()
    private var currentTab = 0
    private lateinit var container: FrameLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var backBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var tabsBtn: ImageButton
    private lateinit var clearBtn: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        container = FrameLayout(this)
        setContentView(container)
        setupBottomBar()
        addNewTab()
        showTab(currentTab)
    }

    private fun setupBottomBar() {
        bottomBar = LinearLayout(this)
        bottomBar.orientation = LinearLayout.HORIZONTAL
        bottomBar.gravity = Gravity.CENTER
        bottomBar.setBackgroundColor(0xFFFFFFFF.toInt())
        val btnSize = resources.displayMetrics.density * 48
        backBtn = ImageButton(this)
        backBtn.setImageResource(R.drawable.back)
        backBtn.setOnClickListener {
            getCurrentWebView()?.let { if (it.canGoBack()) it.goBack() }
        }
        forwardBtn = ImageButton(this)
        forwardBtn.setImageResource(R.drawable.forward)
        forwardBtn.setOnClickListener {
            getCurrentWebView()?.let { if (it.canGoForward()) it.goForward() }
        }
        tabsBtn = ImageButton(this)
        tabsBtn.setImageResource(R.drawable.tabs)
        tabsBtn.setOnClickListener {
            showTabManager()
        }
        clearBtn = ImageButton(this)
        clearBtn.setImageResource(R.drawable.clear)
        clearBtn.setOnClickListener {
            // Completely close and destroy all tabs
            for (tab in tabs) {
                tab.webView.clearCache(true)
                tab.webView.clearHistory()
                tab.webView.removeAllViews()
                tab.webView.destroy()
            }
            tabs.clear()
            addNewTab()
            showTab(currentTab)
            Toast.makeText(this, "所有记录均已安全清除", Toast.LENGTH_SHORT).show()
        }
        val params = LinearLayout.LayoutParams(btnSize.toInt(), btnSize.toInt())
        params.weight = 1f
        bottomBar.addView(backBtn, params)
        bottomBar.addView(forwardBtn, params)
        bottomBar.addView(tabsBtn, params)
        bottomBar.addView(clearBtn, params)
        val barParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, btnSize.toInt())
        barParams.gravity = Gravity.BOTTOM
        container.addView(bottomBar, barParams)
    }

    private fun addNewTab(url: String = homepage) {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view?.loadUrl(url)
                    return true
                }
                return false
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun search(query: String) {
                runOnUiThread {
                    if (isUrl(query)) {
                        webView.loadUrl(query)
                    } else {
                        val duckUrl = "https://www.bing.com/search?q=" + Uri.encode(query)
                        webView.loadUrl(duckUrl)
                    }
                }
            }
        }, "pywebview")
        webView.loadUrl(url)
        tabs.add(Tab(webView, url))
        currentTab = tabs.size - 1
    }

    private fun showTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        container.removeAllViews()
        container.addView(tabs[index].webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        container.addView(bottomBar)
        currentTab = index
    }

    private fun getCurrentWebView(): WebView? {
        return if (tabs.isNotEmpty() && currentTab in tabs.indices) tabs[currentTab].webView else null
    }

    private fun showTabManager() {
        val dialog = android.app.Dialog(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        for ((i, tab) in tabs.withIndex()) {
            val tabLayout = LinearLayout(this)
            tabLayout.orientation = LinearLayout.HORIZONTAL
            tabLayout.setPadding(0, 16, 0, 16)
            val title = android.widget.TextView(this)
            title.text = tab.webView.title ?: tab.url
            title.textSize = 16f
            title.setPadding(0, 0, 16, 0)
            tabLayout.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val switchBtn = ImageButton(this)
            switchBtn.setImageResource(R.drawable.to)
            switchBtn.setOnClickListener {
                showTab(i)
                dialog.dismiss()
            }
            tabLayout.addView(switchBtn, LinearLayout.LayoutParams(96, 96))
            val closeBtn = ImageButton(this)
            closeBtn.setImageResource(R.drawable.close)
            closeBtn.setOnClickListener {
                removeTab(i)
                dialog.dismiss()
                if (tabs.isNotEmpty()) showTab(currentTab)
                else addNewTab()
            }
            tabLayout.addView(closeBtn, LinearLayout.LayoutParams(96, 96))
            layout.addView(tabLayout)
        }
        val addBtn = ImageButton(this)
        addBtn.setImageResource(R.drawable.tab_add)
        addBtn.setOnClickListener {
            addNewTab()
            dialog.dismiss()
            showTab(currentTab)
        }
        layout.addView(addBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 128))
        dialog.setContentView(layout)
        dialog.show()
    }

    private fun removeTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        val tab = tabs[index]
        tab.webView.clearCache(true)
        tab.webView.clearHistory()
        tab.webView.removeAllViews()
        tab.webView.destroy()
        tabs.removeAt(index)
        if (currentTab >= tabs.size) currentTab = tabs.size - 1
    }

    private fun isUrl(input: String): Boolean {
        return input.startsWith("http://") || input.startsWith("https://") || input.matches(Regex("[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*"))
    }

    override fun onPause() {
        super.onPause()
        if (tabs.isNotEmpty()) {
            clearAllData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tabs.isNotEmpty()) {
            clearAllData()
        }
    }

    private fun clearAllData() {
        for (tab in tabs) {
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        deleteAppData()
    }

    private fun deleteAppData() {
        val appDir = applicationContext.filesDir.parentFile
        appDir?.listFiles()?.forEach {
            if (it.isDirectory) deleteDir(it) else it.delete()
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null) return false
        val files = dir.listFiles() ?: return false
        for (file in files) {
            if (file.isDirectory) deleteDir(file) else file.delete()
        }
        return dir.delete()
    }
}
