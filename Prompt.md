# Request — Fix CI compile failure in neural-brain integration

## Reported CI failure
Release and debug Kotlin compilation failed in `AppPreferences.kt` and `NeuralPetBrain.kt`.

## Causes
- The neural-brain state declaration had been accidentally joined to the preceding comment, and an orphaned `the` line remained in the following comment. This made `petBrainEnabledState` disappear and caused the AppPreferences cascade.
- ONNX Runtime Android classes were imported from the wrong package. The artifact coordinates are `com.microsoft.onnxruntime:onnxruntime-android:1.20.0`, but its Kotlin/Java classes are in `ai.onnxruntime`.
- The version-catalog Kotlin accessor was written with dotted navigation instead of the generated camel-case accessor.
- `longArrayOf` received `Int` literals where this compiler/API path requires `Long` values.

## Fix
- Restored separate `petBrainEnabledState` member syntax and repaired the comment.
- Changed ONNX imports to `ai.onnxruntime.*`.
- Changed the dependency reference to `libs.comMicrosoftOnnxruntimeAndroid`.
- Made tensor dimensions explicit `Long` values.

## Validation
- Prior Python schema/smoke/compileall checks passed.
- Prior brace and diff checks passed.
- Gradle is not run locally per the repository DOX rules; CI is the source of truth.

## Closeout
Commit and push this focused CI fix. Do not stage the untracked `pet_brain_emotional_v3.zip`.
