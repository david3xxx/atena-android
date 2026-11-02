package pe.atena.study

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Demostrador gratuito: respuestas locales, sin API, sin conexión a ChatGPT. */
class VoiceService : Service(), RecognitionListener, TextToSpeech.OnInitListener {
    companion object {
        const val START="atena.start"; const val STOP="atena.stop"
        const val COMMAND="atena.command"; const val MANUAL="atena.manual"
        private const val CHANNEL="atena_demo_voice"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var queuedSpeech: String? = null
    private var active = false
    private var suspended = false
    private var speaking = false
    private var course = 0
    private var pending = ""
    private var mode = "abierta"
    private var blocks = emptyList<String>()
    private var position = 0
    private var previous = ""
    private var speechRate = 0.65f
    private val listenRequest by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }
    override fun onCreate() {
        super.onCreate()
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(NotificationChannel(CHANNEL,"ATENA prueba de voz",NotificationManager.IMPORTANCE_LOW))
        tts = TextToSpeech(this,this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply { setRecognitionListener(this@VoiceService) }
        }
    }
    override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int {
        when(intent?.action) {
            STOP -> { active=false; queuedSpeech=null; recognizer?.cancel(); tts?.stop(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            START -> {
                course=intent.getIntExtra("course",0)
                active=true; suspended=false
                val notif=Notification.Builder(this,CHANNEL)
                    .setContentTitle("ATENA · prueba de escucha")
                    .setContentText("Micrófono en uso · toca para abrir")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
                    .build()
                startForeground(22,notif,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                startListening(500)
            }
            MANUAL -> { course=intent.getIntExtra("course",0); answer(intent.getStringExtra("text").orEmpty()) }
            COMMAND -> command(intent.getStringExtra("text").orEmpty())
        }
        return START_NOT_STICKY
    }
    private fun clean(raw:String):String=DictationCore.normalize(raw)
    private fun startListening(delay:Long=450L) {
        if (!active || speaking || recognizer == null) return
        handler.postDelayed({
            if (!active || speaking) return@postDelayed
            try { recognizer?.cancel(); recognizer?.startListening(listenRequest) }
            catch (_:Exception) { handler.postDelayed({startListening(1200)},1200) }
        },delay)
    }
    private fun command(raw:String) {
        val s=clean(raw)
        when {
            s.contains("fuera emergencia") || s == "reanudar" -> { suspended=false; startListening() }
            s.contains("emergencia") -> { suspended=true; queuedSpeech=null; tts?.stop(); speaking=false; startListening() }
            suspended -> { startListening() }
            s == "yaya" || s == "ya ya" -> next()
            s == "no entendi" || s == "repite" -> speak(previous)
            s == "desde cero" || s == "desde el inicio" -> { position=0; next() }
            s == "mas lento" -> { speechRate=(speechRate-0.1f).coerceAtLeast(0.4f); speak(previous) }
            s == "cancelar" -> { blocks=emptyList();pending="";startListening() }
            s == "finalizar modo" -> { blocks=emptyList();pending="";startListening() }
            s == "modo ninja" -> { startListening() }
            s == "modo abierta" || s == "abierta" -> { mode="abierta";startListening() }
            s == "modo multiple" || s == "multiple" -> { mode="multiple";startListening() }
            s.startsWith("modo verdadero") -> { mode="verdadero_falso";startListening() }
            s == "modo completar" -> { mode="completar";startListening() }
            s == "responde" -> {
                val q=pending;pending=""
                if (q.isNotBlank()) answer(q) else startListening()
            }
            s.endsWith(" responde") -> {
                val q=(pending+" "+raw.replace(Regex("(?i)\\s+responde[.!? ]*$"),"")).trim()
                pending="";answer(q)
            }
            previous.isNotBlank() && blocks.isNotEmpty() &&
                DictationCore.isEcho(s, previous) -> speak(previous)
            s.isNotBlank() -> { pending=(pending+" "+raw).trim();startListening() }
        }
    }
    private fun localAnswer(question:String):String {
        val q=clean(question)
        if (mode=="verdadero_falso") return if (q.contains("no se palpa") && q.contains("bazo")) "VERDADERO" else "FALSO"
        if (mode=="multiple") return if (q.contains("gastrina") || q.contains("inspiracion")) "SEGUNDO" else "PRIMERO"
        return when(course) {
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
    private fun answer(q:String) {
        if (suspended || q.isBlank()) return
        val a=localAnswer(q)
        blocks=if (mode=="multiple" || mode=="verdadero_falso") listOf(a)
            else DictationCore.blocks(a)
        position=0; next()
    }
    private fun next() {
        if (suspended) return
        if (position >= blocks.size) { startListening();return }
        previous=blocks[position++]
        speak(previous)
    }
    private fun speak(txt:String) {
        if (txt.isBlank() || suspended) return
        recognizer?.cancel(); speaking=true
        if (ttsReady) {
            tts?.setSpeechRate(speechRate)
            tts?.speak(txt,TextToSpeech.QUEUE_FLUSH,null,"atena_${System.nanoTime()}")
        } else {
            // Conserve the current block while Android initializes its TTS engine.
            queuedSpeech=txt
            speaking=false
        }
    }
    override fun onInit(status:Int) {
        ttsReady=status==TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language=Locale("es","PE")
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
                override fun onStart(utteranceId:String?) {}
                override fun onDone(utteranceId:String?) { handler.post {speaking=false;startListening(700)} }
                override fun onError(utteranceId:String?) { handler.post {speaking=false;startListening(700)} }
            })
            queuedSpeech?.let { pendingBlock -> queuedSpeech=null; handler.post { speak(pendingBlock) } }
        }
    }
    override fun onReadyForSpeech(params:Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB:Float) {}
    override fun onBufferReceived(buffer:ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults:Bundle?) {}
    override fun onEvent(eventType:Int,params:Bundle?) {}
    override fun onError(error:Int) { if(active && !speaking) startListening(1500) }
    override fun onResults(results:Bundle?) {
        val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        command(text)
    }
    override fun onDestroy() {
        active=false;handler.removeCallbacksAndMessages(null)
        recognizer?.destroy();tts?.stop();tts?.shutdown()
        super.onDestroy()
    }
    override fun onBind(intent:Intent?)=null
}
