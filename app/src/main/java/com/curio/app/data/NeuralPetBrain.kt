package com.curio.app.data

import android.content.Context
import android.util.Base64
import com.microsoft.onnxruntime.OnnxTensor
import com.microsoft.onnxruntime.OrtEnvironment
import com.microsoft.onnxruntime.OrtSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.json.JSONObject

/**
 * Optional on-device neural brain boundary.
 *
 * This adapter never silently replaces [CurioPetBrain]. It is active only when
 * the user enables the separate neural-brain setting and a verified ONNX asset
 * is present. Any load/inference failure returns null so callers keep the
 * current deterministic Kotlin brain.
 *
 * The model contract is the Python Emotional v3 export:
 * observations [1, 1, 128], hidden_in [1, 1, 1536], and outputs for action
 * logits, scalar value, 24 emotion channels, 16 needs, 256 memory channels,
 * and hidden_out [1, 1, 1536]. Hidden state is persisted as compact local
 * app state, separately from model weights, and reset when the user disables
 * this experiment or calls [reset].
 */
object NeuralPetBrain {
    private const val MODEL_ASSET = "pet_brain.onnx"
    private const val MODEL_DATA_ASSET = "pet_brain.onnx.data"
    private const val MODEL_MANIFEST = "pet_brain.manifest.json"
    private const val PREFS = "curio_neural_pet_brain"
    private const val KEY_HIDDEN = "hidden_f32_base64"

    private const val INPUT_NAME = "observations"
    private const val HIDDEN_INPUT_NAME = "hidden_in"
    private const val ACTION_OUTPUT_NAME = "action_logits"
    private const val VALUE_OUTPUT_NAME = "value"
    private const val EMOTION_OUTPUT_NAME = "emotion"
    private const val NEEDS_OUTPUT_NAME = "needs"
    private const val MEMORY_OUTPUT_NAME = "memory"
    private const val HIDDEN_OUTPUT_NAME = "hidden_out"

    private const val INPUT_SIZE = 128
    private const val HIDDEN_SIZE = 1536
    private const val ACTION_SIZE = 64
    private const val EMOTION_SIZE = 24
    private const val NEED_SIZE = 16
    private const val MEMORY_SIZE = 256

    data class Output(
        val actionLogits: FloatArray,
        val value: Float,
        val emotion: FloatArray,
        val needs: FloatArray,
        val memory: FloatArray
    )

    private val lock = Any()
    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var hidden: FloatArray? = null
    private var initializationAttempted = false

    /**
     * Returns true only for an explicitly verified, evaluated model asset.
     * The manifest prevents a random or smoke-only checkpoint from becoming
     * user-visible merely because someone copied an `.onnx` file into assets.
     */
    fun isAvailable(context: Context): Boolean =
        AppPreferences.neuralPetBrainEnabledState && hasVerifiedModel(context)

    /**
     * Translates model outputs into a short line. This is deliberately a
     * constrained renderer, not a language model and not a behavior policy.
     */
    fun speechFor(output: Output): String {
        val action = output.actionLogits.indices.maxByOrNull { output.actionLogits[it] } ?: 0
        val hunger = output.needs.getOrNull(0) ?: 0f
        val sleepiness = output.needs.getOrNull(2) ?: 0f
        val affection = output.emotion.getOrNull(4) ?: 0f
        val curiosity = output.emotion.getOrNull(6) ?: 0f
        return when {
            action == 6 || hunger > 0.75f -> "Is there something to eat?"
            action == 5 || action == 18 || sleepiness > 0.75f -> "Mrr... I think I need a nap."
            action == 16 && affection > 0.45f -> "You're here!"
            (action == 9 || action == 15 || action == 10) && curiosity > 0.45f -> "What's that?"
            action == 8 -> "Play with me?"
            else -> "Hmm..."
        }
    }

    /** Clears recurrent state and releases this adapter's native session. */
    fun reset(context: Context) {
        synchronized(lock) {
            hidden = null
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_HIDDEN).apply()
            session?.close()
            session = null
            // OrtEnvironment is process-global in the Java API. Leave it
            // alive for reuse, but discard our session and retry next time.
            environment = null
            initializationAttempted = false
        }
    }

    /**
     * Runs one recurrent step. Returns null on disabled/missing/invalid model
     * so callers can use the existing brain as a safe fallback.
     */
    fun infer(context: Context, observation: FloatArray): Output? {
        if (!AppPreferences.neuralPetBrainEnabledState || observation.size != INPUT_SIZE) return null
        return synchronized(lock) {
            runCatching {
                ensureSession(context) ?: return@synchronized null
                val ortEnvironment = environment ?: return@synchronized null
                val ortSession = session ?: return@synchronized null
                val currentHidden = hidden ?: loadHidden(context) ?: FloatArray(HIDDEN_SIZE)
                if (currentHidden.size != HIDDEN_SIZE) return@synchronized null

                val observationTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    FloatBuffer.wrap(observation),
                    longArrayOf(1, 1, INPUT_SIZE)
                )
                val hiddenTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    FloatBuffer.wrap(currentHidden),
                    longArrayOf(1, 1, HIDDEN_SIZE)
                )
                observationTensor.use { input ->
                    hiddenTensor.use { state ->
                        ortSession.run(
                            mapOf(INPUT_NAME to input, HIDDEN_INPUT_NAME to state)
                        ).use result@{ result ->
                            val actionLogits = result.get(ACTION_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null
                            val value = result.get(VALUE_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null
                            val emotion = result.get(EMOTION_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null
                            val needs = result.get(NEEDS_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null
                            val memory = result.get(MEMORY_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null
                            val nextHidden = result.get(HIDDEN_OUTPUT_NAME).orElse(null)?.value
                                ?: return@result null

                            val actionValues = flatten(actionLogits)
                            val valueValues = flatten(value)
                            val emotionValues = flatten(emotion)
                            val needValues = flatten(needs)
                            val memoryValues = flatten(memory)
                            val nextHiddenValues = flatten(nextHidden)
                            if (actionValues.size != ACTION_SIZE ||
                                valueValues.isEmpty() ||
                                emotionValues.size != EMOTION_SIZE ||
                                needValues.size != NEED_SIZE ||
                                memoryValues.size != MEMORY_SIZE ||
                                nextHiddenValues.size != HIDDEN_SIZE
                            ) {
                                return@result null
                            }

                            hidden = nextHiddenValues
                            saveHidden(context, nextHiddenValues)
                            Output(
                                actionLogits = actionValues,
                                value = valueValues[0],
                                emotion = emotionValues,
                                needs = needValues,
                                memory = memoryValues
                            )
                        }
                    }
                }
            }.getOrNull()
        }
    }

    private fun ensureSession(context: Context): OrtSession? {
        session?.let { return it }
        if (initializationAttempted) return null
        initializationAttempted = true
        if (!hasVerifiedModel(context)) return null

        // Most exports embed initializers in the .onnx file. If an exporter
        // emits external data, copying the optional sidecar next to the graph
        // lets ONNX Runtime resolve it by the same relative filename.
        val modelFile = copyAssetIfNeeded(context, MODEL_ASSET)
        if (context.assets.list("").orEmpty().contains(MODEL_DATA_ASSET)) {
            copyAssetIfNeeded(context, MODEL_DATA_ASSET)
        }
        return runCatching {
            val ortEnvironment = OrtEnvironment.getEnvironment()
            environment = ortEnvironment
            ortEnvironment.createSession(modelFile.absolutePath)
        }.onFailure {
            environment = null
        }.getOrNull().also { loaded ->
            session = loaded
            if (loaded == null) initializationAttempted = false
        }
    }

    private fun hasVerifiedModel(context: Context): Boolean {
        val assets = context.assets.list("").orEmpty()
        if (MODEL_ASSET !in assets || MODEL_MANIFEST !in assets) return false
        return runCatching {
            context.assets.open(MODEL_MANIFEST).bufferedReader().use { reader ->
                val manifest = JSONObject(reader.readText())
                manifest.optBoolean("verified_for_android", false) &&
                    manifest.optString("training_status") == "evaluated" &&
                    manifest.optInt("input_size", 0) == INPUT_SIZE &&
                    manifest.optInt("hidden_size", 0) == HIDDEN_SIZE &&
                    manifest.optInt("action_count", 0) == ACTION_SIZE
            }
        }.getOrDefault(false)
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String): File {
        val destination = File(context.filesDir, assetName)
        val assetLength = context.assets.open(assetName).use { it.available().toLong() }
        if (!destination.exists() || destination.length() != assetLength) {
            context.assets.open(assetName).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return destination
    }

    private fun loadHidden(context: Context): FloatArray? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HIDDEN, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            if (bytes.size != HIDDEN_SIZE * Float.SIZE_BYTES) return@runCatching null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            FloatArray(HIDDEN_SIZE) { buffer.float }
        }.getOrNull()
    }

    private fun saveHidden(context: Context, values: FloatArray) {
        val buffer = ByteBuffer.allocate(HIDDEN_SIZE * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        val encoded = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_HIDDEN, encoded).apply()
    }

    private fun flatten(value: Any?): FloatArray = when (value) {
        is FloatArray -> value
        is Array<*> -> value.flatMap { flatten(it).asList() }.toFloatArray()
        is Number -> floatArrayOf(value.toFloat())
        else -> FloatArray(0)
    }
}
