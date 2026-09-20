package jp.trackrail.shopclipper.ui.share

import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.io.File
import java.util.Date
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jp.trackrail.shopclipper.ui.theme.ShopClipperTheme

/**
 * I2 (temporary): shows what another app sent with ACTION_SEND, without any processing,
 * so that the real shared text can be recorded in the plan (sec.2).
 * The same dump goes to logcat (tag [TAG]) so that it can be read exactly with adb.
 * Replaced by the real share form in I5 (the log output goes away then).
 */
class ShareActivity : ComponentActivity() {

    private var dump by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        show(intent)
        setContent {
            ShopClipperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SelectionContainer {
                        Text(
                            text = dump,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent)
    }

    private fun show(intent: Intent) {
        dump = describe(intent)
        Log.i(TAG, dump)
        // The logcat ring buffer on the Pixel holds only about a minute, so keep a copy in
        // app-private storage too (read with `adb exec-out run-as <pkg> cat files/<file>`).
        runCatching { File(filesDir, SAMPLES_FILE).appendText("=== ${Date()}\n$dump\n") }
            .onFailure { Log.w(TAG, "could not save the sample", it) }
    }

    private fun describe(intent: Intent): String = buildString {
        appendLine("referrer: $referrer")
        appendLine("action: ${intent.action}")
        appendLine("type: ${intent.type}")
        appendLine("EXTRA_TEXT: ${quote(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())}")
        appendLine("EXTRA_SUBJECT: ${quote(intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString())}")
        val extras = intent.extras
        val keys = extras?.keySet()?.sorted().orEmpty()
        appendLine("extras: $keys")
        for (key in keys) {
            @Suppress("DEPRECATION")
            val value = extras?.get(key)
            appendLine("  $key (${value?.javaClass?.name}): ${quote(value?.toString())}")
        }
        val clip = intent.clipData
        appendLine("clipData: ${clip?.itemCount ?: 0} item(s)")
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                appendLine("  [$i] text=${quote(item.text?.toString())} uri=${item.uri}")
            }
        }
    }

    /** Makes line breaks, tabs and invisible characters visible. */
    private fun quote(s: String?): String {
        if (s == null) return "null"
        return buildString {
            append('"')
            for (c in s) {
                when {
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c.isISOControl() || c.category == CharCategory.FORMAT ->
                        append("\\u").append(c.code.toString(16).padStart(4, '0'))
                    else -> append(c)
                }
            }
            append('"')
        }
    }

    private companion object {
        const val TAG = "ShopClipperI2"
        const val SAMPLES_FILE = "i2_samples.txt"
    }
}
