package com.flxrs.dankchat.main

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.main.stream.StreamWebView
import com.flxrs.dankchat.preferences.stream.StreamsSettingsDataStore
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class StreamWebViewModel(
    application: Application,
    private val streamsSettingsDataStore: StreamsSettingsDataStore,
) : AndroidViewModel(application) {

    private var lastStreamedChannel: UserName? = null

    @SuppressLint("StaticFieldLeak")
    private var streamWebView: StreamWebView? = null

    fun getOrCreateStreamWebView(): StreamWebView {
        return when {
            !streamsSettingsDataStore.current().preventStreamReloads -> StreamWebView(getApplication())
            else                                                     -> streamWebView ?: StreamWebView(getApplication()).also {
                streamWebView = it
            }
        }
    }

    fun setStream(channel: UserName?, webView: StreamWebView) {
        if (!streamsSettingsDataStore.current().preventStreamReloads) {
            // Clear previous retained WebView instance
            streamWebView?.let {
                it.destroy()
                streamWebView = null
                lastStreamedChannel = null
            }

            webView.setStream(channel, streamsSettingsDataStore.current().showStreamExtensions)
            return
        }

        // Prevent unnecessary stream loading
        if (channel == lastStreamedChannel) {
            return
        }

        lastStreamedChannel = channel
        webView.setStream(channel, streamsSettingsDataStore.current().showStreamExtensions)
    }

    val splitFraction: Float
        get() = streamsSettingsDataStore.current().splitFraction

    fun setSplitFraction(fraction: Float) {
        viewModelScope.launch {
            streamsSettingsDataStore.update { it.copy(splitFraction = fraction) }
        }
    }

    override fun onCleared() {
        streamWebView?.destroy()
        streamWebView = null
        lastStreamedChannel = null
        super.onCleared()
    }
}
