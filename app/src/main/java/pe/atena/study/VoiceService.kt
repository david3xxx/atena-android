package pe.atena.study

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Demo local: sin respuestas generadas por ChatGPT y sin sonidos propios de interfaz. */
class VoiceService : Service(), RecognitionListener, TextToSpeech.OnInitListener {
    companion object {
        const val START = "atena.start"
        const val STOP = "atena.stop"
        const val COMMAND = "atena.command"
        const val MANUAL = "atena.manual"
        private const val CHANNEL = "atena_voice_silent_v05"
        private const val NOTIFICATION_ID = 22
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var queuedSpeech: String? = null
    private var active = false
    private var suspended = false
    private var speaking = false
    private var listening = false
    private var course = 0
    private var pending = ""
    private var mode = "abierta"
    private var blocks = emptyList<String>()
    private var position = 0
    private var previous = ""
    private var speechRate = 0.70f

    private val listenRequest by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    // Nunca cancela una sesión solo para volver a abrirla. Evita reinicios duplicados.
    private val beginListening = Runnable {
        if (!active || speaking || listening || recognizer == null) return@Runnable
        try {
            listening = true
            recognizer?.startListening(listenRequest)
        } catch (_: Exception) {
            listening = false
            scheduleListening(1800)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, "ATENA · escucha silenciosa", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(this@VoiceService)
            }
        }
    }

    private fun foregroundNotice(): Notification = Notification.Builder(this, CHANNEL)
        .setContentTitle("ATENA · práctica por voz")
        .setContentText("Escucha por turnos; comandos YAYA, REPITE y MÁS LENTO")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOnlyAlertOnce(true)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
.setOnlyAlertOnce(true)
.setContentIntent(

        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun enableVoice(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (!active) {
            active = true
            startForeground(
                NOTIFICATION_ID,
                foregroundNotice(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        }
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> {
                active = false
                handler.removeCallbacks(beginListening)
                listening = false
                speaking = false
                queuedSpeech = null
                recognizer?.cancel()
                tts?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            START -> {
                course = intent.getIntExtra("course", 0)
                suspended = false
                if (enableVoice()) scheduleListening(500)
            }
            MANUAL -> {
                course = intent.getIntExtra("course", 0)
                // Al pulsar responder, activa también la escucha para recibir YAYA después.
                enableVoice()
                answer(intent.getStringExtra("text").orEmpty())
            }
            COMMAND -> command(intent.getStringExtra("text").orEmpty())
        }
        return START_NOT_STICKY
    }

    private fun scheduleListening(delay: Long = 650L) {
        handler.removeCallbacks(beginListening)
        if (active && !speaking && !listening && recognizer != null) {
            handler.postDelayed(beginListening, delay)
        }
    }

    private fun clean(raw: String) = DictationCore.normalize(raw)

    private fun command(raw: String) {
        val s = clean(raw)
        when {
            s.contains("fuera emergencia") || s == "reanudar" -> {
                suspended = false
                scheduleListening()
            }
            s.contains("emergencia") -> {
                suspended = true
                queuedSpeech = null
                tts?.stop()
                speaking = false
                scheduleListening()
            }
            suspended -> scheduleListening()
            s in setOf("yaya", "ya ya", "siguiente") -> next()
            s in setOf("repite", "repetir", "no entendi", "otra vez") -> repeatLast()
            s in setOf("mas lento", "mas despacio", "habla mas lento", "mas lentamente") -> {
                speechRate = (speechRate - 0.15f).coerceAtLeast(0.35f)
                repeatLast()
            }
            s in setOf("desde cero", "desde el inicio") -> { position = 0; next() }
            s in setOf("cancelar", "finalizar modo") -> {
                blocks = emptyList()
                previous = ""
                pending = ""
                scheduleListening()
            }
            s in setOf("modo abierta", "abierta") -> { mode = "abierta"; scheduleListening() }
            s in setOf("modo multiple", "multiple") -> { mode = "multiple"; scheduleListening() }
            s.startsWith("modo verdadero") -> { mode = "verdadero_falso"; scheduleListening() }
            s == "modo completar" -> { mode = "completar"; scheduleListening() }
            s in setOf("responde", "responder", "responde pregunta", "responder pregunta") -> {
                val question = pending.trim()
                pending = ""
                if (question.isNotBlank()) answer(question) else scheduleListening()
            }
            s.endsWith(" responde") || s.endsWith(" responder") -> {
                val question = (pending + " " + raw.replace(Regex("(?i)\\s+respond(?:e|er)[.!? ]*$"), "")).trim()
                pending = ""
                if (question.isNotBlank()) answer(question) else scheduleListening()
            }
            previous.isNotBlank() && blocks.isNotEmpty() && DictationCore.isEcho(s, previous) -> repeatLast()
            s.isNotBlank() -> {
                pending = (pending + " " + raw).trim()
                scheduleListening()
            }
            else -> scheduleListening()
        }
    }

    private fun localAnswer(question: String): String {
        val q = clean(question)
        if (mode == "verdadero_falso") return if (q.contains("no se palpa") && q.contains("bazo")) "VERDADERO" else "FALSO"
        if (mode == "multiple") return if (q.contains("gastrina") || q.contains("inspiracion")) "SEGUNDO" else "PRIMERO"
        return when (course) {
            1 -> when {
                q.contains("leptospira") -> "Leptospira es una bacteria espiroqueta delgada y móvil."
                q.contains("tuberculosis") -> "La prueba molecular GeneXpert detecta ADN de Mycobacterium tuberculosis."
                else -> "La microbiología estudia microorganismos, sus mecanismos patógenos y su diagnóstico."
            }
            2 -> when {
                q.contains("ganglios basales") -> "Los ganglios basales regulan la selección e inhibición del movimiento."
                q.contains("area 4") -> "El área cuatro corresponde a la corteza motora primaria."
                else -> "El sistema nervioso integra información sensitiva y coordina respuestas motoras."
            }
            else -> when {
                q.contains("relajar") -> "Disminuye la tensión muscular y facilita identificar órganos, masas y zonas dolorosas."
                q.contains("superficial") && q.contains("profunda") -> "La superficial evalúa sensibilidad y tensión muscular. La profunda identifica órganos y masas abdominales."
                q.contains("higado") -> "Coloca la mano bajo el reborde costal derecho y palpa durante la inspiración profunda."
                q.contains("bazo") -> "El bazo normal generalmente no es palpable porque permanece protegido por las costillas."
                else -> "La palpación abdominal permite identificar dolor, resistencia muscular, órganos y masas anormales."
            }
        }
    }

    private fun answer(question: String) {
        if (suspended || question.isBlank()) {
            scheduleListening()
            return
        }
        val result = localAnswer(question)
        blocks = if (mode == "multiple" || mode == "verdadero_falso") listOf(result)
                 else DictationCore.blocks(result)
        position = 0
        next()
    }

    private fun next() {
        if (suspended) return
        if (position >= blocks.size) {
            scheduleListening()
            return
        }
        previous = blocks[position++]
        speak(previous)
    }

    private fun repeatLast() {
        if (previous.isNotBlank()) speak(previous) else scheduleListening()
    }

    private fun speak(text: String) {
        if (text.isBlank() || suspended) return
        handler.removeCallbacks(beginListening)
        speaking = true
        // Cancelar solamente cuando realmente hay una sesión de reconocimiento abierta.
        if (listening) {
            listening = false
            recognizer?.cancel()
        }
        if (ttsReady) {
            tts?.setSpeechRate(speechRate)
            val response = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "atena_${System.nanoTime()}")
            if (response != TextToSpeech.SUCCESS) {
                speaking = false
                scheduleListening()
            }
        } else {
            queuedSpeech = text
            speaking = false
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (!ttsReady) return
        tts?.language = Locale("es", "PE")
        tts?.setSpeechRate(speechRate)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                handler.post { speaking = false; scheduleListening(650) }
            }
            override fun onError(utteranceId: String?) {
                handler.post { speaking = false; scheduleListening(1000) }
            }
        })
        queuedSpeech?.let { text ->
            queuedSpeech = null
            handler.post { speak(text) }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onError(error: Int) {
        listening = false
        if (active && !speaking) {
            val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) 2200L else 1400L
            scheduleListening(delay)
        }
    }

    override fun onResults(results: Bundle?) {
        listening = false
        val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull().orEmpty()
        command(heard)
    }

    override fun onDestroy() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
