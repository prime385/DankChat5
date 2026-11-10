package com.flxrs.dankchat.main.stream

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.flxrs.dankchat.data.UserName

@SuppressLint("SetJavaScriptEnabled")
class StreamWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    init {
        with(settings) {
            javaScriptEnabled = true
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
        }
        webViewClient = StreamWebViewClient()
        // Runs before Twitch's player scripts, so the player starts with the chosen quality
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching { WebViewCompat.addDocumentStartJavaScript(this, QUALITY_SCRIPT, setOf("https://player.twitch.tv")) }
        }
    }

    fun setStream(channel: UserName?, enableExtensions: Boolean = false, audioOnly: Boolean = false) {
        val isActive = channel != null
        isVisible = isActive
        val audioOnlyParam = if (audioOnly) "&$AUDIO_ONLY_PARAM" else ""
        val url = when {
            isActive -> "https://player.twitch.tv/?channel=$channel&enableExtensions=$enableExtensions&muted=false&parent=twitch.tv$audioOnlyParam"
            else     -> BLANK_URL
        }

        stopLoading()
        loadUrl(url)
    }

    private class StreamWebViewClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url.isNullOrBlank()) {
                return true
            }

            return ALLOWED_PATHS.none { url.startsWith(it) }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            if (url.isNullOrBlank()) {
                return true
            }

            return ALLOWED_PATHS.none { url.startsWith(it) }
        }
    }

    companion object {
        private const val BLANK_URL = "about:blank"
        private const val AUDIO_ONLY_PARAM = "dankchatAudioOnly=true"

        // Audio-only: remember the player's quality preference and switch it to "audio_only".
        // Normal streams: put the remembered preference back.
        private val QUALITY_SCRIPT = """
            (function() {
              try {
                var key = 'video-quality', saved = 'dankchat-saved-video-quality';
                if (location.search.indexOf('$AUDIO_ONLY_PARAM') !== -1) {
                  if (localStorage.getItem(saved) === null) localStorage.setItem(saved, localStorage.getItem(key) || '');
                  localStorage.setItem(key, JSON.stringify({ 'default': 'audio_only' }));
                } else {
                  var previous = localStorage.getItem(saved);
                  if (previous !== null) {
                    if (previous) localStorage.setItem(key, previous); else localStorage.removeItem(key);
                    localStorage.removeItem(saved);
                  }
                }
              } catch (e) {}
            })();
        """.trimIndent()
        private val ALLOWED_PATHS = listOf(
            BLANK_URL,
            "https://id.twitch.tv/",
            "https://www.twitch.tv/passport-callback",
            "https://player.twitch.tv/",
        )
    }

}
