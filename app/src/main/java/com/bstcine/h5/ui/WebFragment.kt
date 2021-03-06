package com.bstcine.h5.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bstcine.h5.CineApplication
import com.bstcine.h5.CineJsNative
import com.bstcine.h5.R
import com.bstcine.h5.widget.CWebView
import com.google.gson.Gson
import com.tencent.smtt.sdk.WebViewClient
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.sdk.WebView

private const val ARG_HREF = "param_url"

class WebFragment : Fragment() {

    private var mHref: String? = null

    private var mRefresh: SwipeRefreshLayout? = null
    private var mWebView: CWebView? = null

    companion object {
        @JvmStatic
        fun newInstance(href: String) =
                WebFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_HREF, href)
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mHref = it.getString(ARG_HREF)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_web, container, false)

        mRefresh = view.findViewById(R.id.refresh)
        mWebView = CWebView(activity!!.applicationContext)

        mRefresh!!.addView(mWebView)

        mWebView!!.setSwipeRefreshLayout(mRefresh!!)
        mRefresh!!.setOnRefreshListener { mWebView?.reload() }
        mWebView!!.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (!url!!.contains("bstcine.com")) return true

                val intent = Intent(activity, WebActivity::class.java)
                intent.putExtra("url", url)
                startActivity(intent)
                return true
            }

            override fun onReceivedSslError(p0: WebView?, p1: SslErrorHandler?, p2: SslError?) {
                p1!!.proceed()
            }
        }
        mWebView!!.addJavascriptInterface(CineJsNative(), "Android")

        mWebView!!.loadUrl(bindUrl(this.mHref!!))

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        mRefresh!!.removeAllViews()

        mWebView?.clearHistory()

        mWebView?.clearCache(true)

        mWebView?.loadUrl("about:blank")

        mWebView?.onPause()
        mWebView?.removeAllViews()

        mWebView?.destroy()

        mWebView = null
    }

    private fun bindUrl(url: String): String {
        var tempUrl = if (url.contains("?")) {
            "$url&sitecode=cine.web.android.kotlin"
        } else {
            "$url?sitecode=cine.web.android.kotlin"
        }

        if (CineApplication.INSTANCE.isLogin()) tempUrl += "&token=" + CineApplication.INSTANCE.token()

        return tempUrl
    }

    fun emitJavascript(name: String, arg: Any) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            mWebView?.evaluateJavascript("window._cine_listener.emit('$name',${Gson().toJson(arg)})", null)
        } else {
            mWebView?.loadUrl("javascript:window._cine_listener.emit('$name',${Gson().toJson(arg)})")
        }
    }

}
