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
    val selectedBackend: StorageBackend = StorageBackend.SUPABASE

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
    const val supabaseUrl: String = "https://vjgkrpuahuzfuguezesv.supabase.co"
    const val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZqZ2tycHVhaHV6ZnVndWV6ZXN2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1MTUxNTIsImV4cCI6MjA5NTA5MTE1Mn0.fpymvAk1dOeTLyBCC3xagV8kZ8uweylxiHwsrOSX0tI"
}
