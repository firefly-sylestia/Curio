package com.curio.app.data

/**
 * Singleton holder for [CaptureRepository], initialized by [MainActivity]
 * after Room database is created. Provides a convenient access point for
 * screens to call repository methods without manual dependency injection.
 *
 * Must call [init] before any screen accesses [repo].
 */
object CurioRepositoryHolder {
    @Volatile
    private var _repo: CaptureRepository? = null

    val repo: CaptureRepository
        get() = _repo ?: error(
            "CaptureRepository not initialized. Call CurioRepositoryHolder.init() " +
            "from MainActivity.onCreate() before any screen accesses it."
        )

    fun init(dao: CaptureDao, cachedTopicDao: CachedTopicDao) {
        if (_repo == null) {
            synchronized(this) {
                if (_repo == null) {
                    _repo = CaptureRepository(dao, cachedTopicDao)
                }
            }
        }
    }
}
