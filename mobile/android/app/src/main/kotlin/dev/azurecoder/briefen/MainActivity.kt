package dev.azurecoder.briefen

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val CHANNEL = "dev.azurecoder.briefen/share"
    }

    private var methodChannel: MethodChannel? = null
    private var pendingUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractSharedUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUrl(intent)
        // App is already running — push the URL to Flutter immediately
        methodChannel?.invokeMethod("sharedUrl", pendingUrl)
        pendingUrl = null
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL,
        ).also { ch ->
            ch.setMethodCallHandler { call, result ->
                when (call.method) {
                    "getInitialUrl" -> {
                        result.success(pendingUrl)
                        pendingUrl = null
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    private fun extractSharedUrl(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            pendingUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }
}
