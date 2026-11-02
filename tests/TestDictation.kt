import pe.atena.study.DictationCore

fun main() {
    check(DictationCore.blocks("Disminuye la tensión muscular y facilita identificar órganos") ==
        listOf("Disminuye la tensión", "muscular y facilita", "identificar órganos"))
    check(DictationCore.blocks("   ").isEmpty())
    check(DictationCore.blocks("una dos tres cuatro").last() == "cuatro")
    check(DictationCore.normalize("¡YAYÁ!  ") == "yaya")
    check(DictationCore.isEcho("la tensión", "Disminuye la tensión"))
    check(DictationCore.isEcho("Disminuye la", "Disminuye la tensión"))
    check(!DictationCore.isEcho("yaya", "Disminuye la tensión"))
    println("7 pruebas de dictado ATENA: OK")
}
