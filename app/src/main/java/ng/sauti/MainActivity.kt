package ng.sauti

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var engine: SpeechEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { App() }
    }

    override fun onStop() { super.onStop(); engine?.stop() }

    @Composable
    fun App() {
        val lines = remember { mutableStateListOf<String>() }
        var partial by remember { mutableStateOf("") }
        var listening by remember { mutableStateOf(false) }
        var lang by remember { mutableStateOf(LANGS[0]) }
        var size by remember { mutableFloatStateOf(28f) }
        var dark by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        val listState = rememberLazyListState()

        val bg = if (dark) Color.Black else Color.White
        val fg = if (dark) Color.White else Color.Black
        val accent = if (dark) Color(0xFFFFEB3B) else Color(0xFF0D47A1)

        fun begin() {
            error = null
            engine?.stop()
            engine = AndroidSpeechEngine(this,
                onPartial = { partial = it },
                onFinal = { lines.add(it); partial = "" },
                onError = { error = it; listening = false; partial = "" })
            engine!!.start(lang); listening = true
        }
        fun end() { engine?.stop(); listening = false; partial = "" }

        val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) begin() else error = "Microphone permission is required."
        }
        fun toggle() {
            if (listening) end()
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) begin()
            else perm.launch(Manifest.permission.RECORD_AUDIO)
        }
        LaunchedEffect(lines.size, partial) {
            if (lines.isNotEmpty() || partial.isNotEmpty()) listState.animateScrollToItem(lines.size)
        }

        MaterialTheme {
            Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LANGS.forEach { l ->
                        val sel = l.name == lang.name
                        Button(onClick = { lang = l; if (listening) { end(); begin() } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (sel) accent else Color.DarkGray,
                                contentColor = if (sel) bg else Color.White)) { Text(l.name, fontSize = 16.sp) }
                    }
                }
                if (lang.note.isNotEmpty()) Text(lang.note, color = accent, fontSize = 13.sp)
                error?.let { Text(it, color = Color(0xFFFF5252), fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp)) }

                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp), state = listState) {
                    items(lines) { Text(it, color = fg, fontSize = size.sp, lineHeight = (size * 1.3f).sp, modifier = Modifier.padding(bottom = 12.dp)) }
                    item { Text(partial, color = accent, fontSize = size.sp, lineHeight = (size * 1.3f).sp) }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", color = fg, fontSize = 14.sp)
                    Slider(size, { size = it }, valueRange = 18f..56f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text("A", color = fg, fontSize = 30.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { dark = !dark }) { Text(if (dark) "Light" else "Dark") }
                    OutlinedButton(onClick = { lines.clear(); partial = "" }) { Text("Clear") }
                    OutlinedButton(onClick = {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
                        }, "Share transcript"))
                    }) { Text("Share") }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { toggle() }, modifier = Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (listening) Color(0xFFD32F2F) else accent,
                        contentColor = if (listening) Color.White else bg)) {
                    Text(if (listening) "■  STOP" else "●  START LISTENING", fontSize = 22.sp)
                }
            }
        }
    }
}
