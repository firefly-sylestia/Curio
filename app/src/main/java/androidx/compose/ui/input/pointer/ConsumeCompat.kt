package androidx.compose.ui.input.pointer

/** Compatibility bridge for Compose versions where PointerInputChange.consume() is not exposed. */
fun PointerInputChange.consume() {
    consumePositionChange()
}
