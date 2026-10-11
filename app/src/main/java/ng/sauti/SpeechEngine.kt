package ng.sauti

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class Lang(val name: String, val tag: String, val note: String = "")

val LANGS = listOf(
    Lang("English", "en-NG"),
    Lang("Yorùbá", "yo-NG"),
    Lang("Igbo", "ig-NG"),
    Lang("Hausa", "ha-NG"),
    Lang("Pidgin", "en-NG", "Pidgin uses the Nigerian English model")
)

/** Swap this interface's implementation for a bundled offline model (Whisper/sherpa-onnx) — see README. */
interface SpeechEngine {
    fun start(lang: Lang)
    fun stop()
}

class AndroidSpeechEngine(
    private val ctx: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) : SpeechEngine {
    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private var running = false
    private var lang = LANGS[0]

    override fun start(lang: Lang) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            onError("No speech recognition service on this phone. Install/enable the Google app."); return
        }
        this.lang = lang; running = true; listen()
    }

    override fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        rec?.destroy(); rec = null
    }

    private fun listen() {
        if (!running) return
        rec?.destroy()
        rec = SpeechRecognizer.createSpeechRecognizer(ctx).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onPartialResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
                }
                override fun onResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }?.let(onFinal)
                    restart()
                }
                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail("Microphone permission needed.")
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                            fail("${lang.name} isn't available offline on this phone. Download it in Google app > Settings > Voice > Offline speech recognition.")
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restart()
                        else -> restart(1000)
                    }
                }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        }
    }

    private fun restart(delay: Long = 150) { if (running) main.postDelayed({ listen() }, delay) }
    private fun fail(msg: String) { stop(); onError(msg) }
}
