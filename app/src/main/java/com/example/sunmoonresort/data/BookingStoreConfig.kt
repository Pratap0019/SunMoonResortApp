package com.example.sunmoonresort.data

/**
 * Local compile-time storage backend selector.
 *
 * Change [selectedBackend] and rebuild the app.
 */
object BookingStoreConfig {

    enum class StorageBackend {
        LOCAL,
        FIREBASE,
        SUPABASE,
    }

    /**
     * Active booking backend.
     * Keep LOCAL unless cloud setup is complete.
     */
    val selectedBackend: StorageBackend = StorageBackend.LOCAL

    /** Compatibility alias used by older call sites/comments. */
    val useFirebaseStore: Boolean
        get() = selectedBackend == StorageBackend.FIREBASE

    /** True for any remote backend (Firebase/Supabase). */
    val isRemoteStoreEnabled: Boolean
        get() = selectedBackend != StorageBackend.LOCAL

    // Supabase REST settings (required only when selectedBackend == SUPABASE)
    // Example:
    //   supabaseUrl = "https://YOUR_PROJECT_REF.supabase.co"
    //   supabaseAnonKey = "eyJhbGciOi..."
    const val supabaseUrl: String = ""
    const val supabaseAnonKey: String = ""
}
