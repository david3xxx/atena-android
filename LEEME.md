# ATENA DEMO GRATIS v0.5 — Código Android para compilar

**NO ES UN APK PRECOMPILADO.** Es código de una demostración Android, con un sistema de compilación automática mediante GitHub Actions. No está validado en ningún teléfono. También incluye una demo web que puedes abrir y probar ahora mismo sin compilar.

## Qué permite probar
- Interfaz simple para elegir PM4, MECA II, SO3.
- Preguntas de demostración respondidas con **respuestas locales predefinidas**, sin consultar ChatGPT ni pagar API.
- TTS Android en español, bloques de tres palabras y comandos YAYA, REPITE, NO ENTENDÍ, DESDE CERO, MÁS LENTO.
- Reconocimiento Android de voz disponible según servicios del dispositivo.
- Servicio de primer plano con notificación. Android puede suspender reconocimiento con pantalla apagada.

**IMPORTANTE**: la conexión oficial desde una app propia con tu cuenta ChatGPT sigue pendiente de validación. Esta demostración no inicia sesión en ChatGPT, no tiene acceso a tus chats, no responde libremente con inteligencia artificial y no almacena tus documentos. Las respuestas demostrativas son fijas y limitadas.

## Probar YA sin compilar nada — demo web

1. Abre `demo-web/ATENA_DEMO_GRATIS.html` con **Chrome para Android** (o Chrome de escritorio).
2. Elige PM4, MECA II o SO3 y selecciona una pregunta de ejemplo.
3. Pulsa **RESPONDER · DEMO**. Se reproducirán tres palabras con síntesis de voz del navegador; luego pulsa **YAYA · SIGUIENTE**.
4. Prueba **REPITE**, **DESDE CERO**, **MÁS LENTO** y **EMERGENCIA**.
5. Si Chrome autoriza el reconocimiento de voz desde ese archivo, prueba **INICIAR ESCUCHA**. La compatibilidad de micrófono depende de navegador, permisos y origen; puede no funcionar directamente desde archivos locales.

**No es una APK**. Es una demostración alternativa para comprobar el control del dictado ahora. Tampoco consulta ChatGPT.

## Compilar gratis en GitHub Actions
1. Crea un repositorio privado de GitHub.
2. Sube **el contenido** de esta carpeta a la raíz del repositorio (incluida `.github/workflows/build-apk.yml`).
3. Abre la pestaña **Actions** > **Compilar ATENA Demo APK** > **Run workflow**.
4. Cuando termine correctamente, abre la ejecución, busca **Artifacts** y descarga `ATENA-demo-v0-4-APK`.
5. Extrae el archivo ZIP que GitHub entrega: dentro estará `app-debug.apk`.
6. Instálalo en tu Android solo si confías en el APK que acabas de compilar tú mismo; quizás debas permitir instalación desde esa fuente.

Usa solamente un repositorio que controles. El uso de GitHub Actions depende de las condiciones y cuotas de tu cuenta.

## Compilar en Android Studio
1. Abre Android Studio, selecciona **Open** y elige esta carpeta.
2. Espera la sincronización Gradle y descarga Android SDK Platform 35 si se solicita.
3. En menú **Build** selecciona **Build APK(s)**.
4. Busca el archivo `app/build/outputs/apk/debug/app-debug.apk`.

## Seguridad y limitaciones
- Esta demo no necesita claves ni servicio API.
- Los comandos de voz no están garantizados si Android corta el reconocimiento, si el TTS no está inicializado o si hay ruido.
- La opción EMERGENCIA silencia la reproducción; NO reemplaza un interruptor físico de micrófono.
- La funcionalidad está destinada a prácticas, estudio y casos de uso autorizados.
- No hay garantía de funcionamiento continuo en segundo plano ni de recuperación automática tras muerte del proceso.


## Estado de la validación
- [x] El código, el HTML y las pruebas de dictado están presentes.
- [x] El JavaScript del HTML pasó una verificación sintáctica con Node.js y 7 pruebas Kotlin del motor de dictado pasaron localmente.
- [ ] La aplicación Android **no** se ha compilado aquí: faltan Android SDK, Gradle y acceso a descargas en el entorno.
- [ ] No se ha instalado ni ensayado en un teléfono físico.
- [ ] La autenticación oficial Sign in with ChatGPT está pendiente; no está simulada como si ya existiera.


## Prueba v0.5
Incluye mejor reconocimiento de los comandos de voz y reducción de reinicios innecesarios. **No garantiza suprimir los pitidos del servicio de reconocimiento instalado por Android**, ni escucha física ininterrumpida. No se conecta a ChatGPT. Consulta CAMBIOS_V0_5.txt.
