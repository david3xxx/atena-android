package pe.atena.study

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var question: EditText
    private lateinit var course: Spinner
    private lateinit var status: TextView
    private val labels = arrayOf("PM4: palpación abdominal", "MECA II: microbiología", "SO3: neuroanatomía")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 40)
        }
        fun label(t: String, large: Boolean = false) {
            col.addView(TextView(this).apply { text = t; textSize = if (large) 25f else 16f; setPadding(0, 7, 0, 7) })
        }
        fun btn(t: String, cb: () -> Unit) {
            col.addView(Button(this).apply { text = t; setOnClickListener { cb() } })
        }
        label("ATENA · DEMOSTRACIÓN GRATUITA", true)
        label("Prueba el dictado de tres palabras y los comandos. Esta versión NO se conecta a ChatGPT ni utiliza una API de pago.")
        label("Selecciona un curso de prueba:")
        course = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels) }
        col.addView(course)
        btn("INICIAR ESCUCHA") { startListen() }
        btn("DETENER ESCUCHA") { send(VoiceService.STOP) }
        label("Escribe una pregunta o usa el micrófono:")
        question = EditText(this).apply { hint = "¿Por qué es importante relajar al paciente durante la palpación?"; minLines = 3 }
        col.addView(question)
        btn("RESPONDER PREGUNTA DE PRUEBA") {
            val q = question.text.toString().ifBlank { "¿Por qué es importante relajar al paciente durante la palpación?" }
            val i = Intent(this, VoiceService::class.java).setAction(VoiceService.MANUAL)
            i.putExtra("text", q)
            i.putExtra("course", course.selectedItemPosition)
            startService(i)
            status.text = "Respuesta de demostración solicitada. Avanza con YAYA."
        }
        btn("YAYA · SIGUIENTES TRES PALABRAS") { cmd("yaya") }
        btn("REPITE / NO ENTENDÍ") { cmd("no entendi") }
        btn("DESDE CERO") { cmd("desde cero") }
        btn("MÁS LENTO") { cmd("mas lento") }
        btn("EMERGENCIA · SILENCIAR") { cmd("emergencia") }
        btn("FUERA EMERGENCIA") { cmd("fuera emergencia") }
        status = TextView(this).apply { text = "Listo para probar. Concede permisos de micrófono y notificaciones."; textSize = 15f }
        col.addView(status)
        scroll.addView(col); setContentView(scroll)
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 10)
    }
    private fun send(action: String) { startService(Intent(this, VoiceService::class.java).setAction(action)) }
    private fun cmd(text: String) {
        startService(Intent(this, VoiceService::class.java).setAction(VoiceService.COMMAND).putExtra("text", text))
    }
    private fun startListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Concede permiso de micrófono", Toast.LENGTH_LONG).show(); return
        }
        val i = Intent(this, VoiceService::class.java).setAction(VoiceService.START)
            .putExtra("course", course.selectedItemPosition)
        startForegroundService(i)
        status.text = "Escucha solicitada. Prueba: 'Modo abierta', tu pregunta y 'Responde'."
    }
}
