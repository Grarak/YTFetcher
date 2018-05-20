/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.ext.opus

import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.util.LibraryLoader

/**
 * Configures and queries the underlying native library.
 */
object OpusLibrary {

    private val LOADER = LibraryLoader("opusJNI")

    /**
     * Returns whether the underlying library is available, loading it if necessary.
     */
    val isAvailable: Boolean
        get() = LOADER.isAvailable

    /**
     * Returns the version of the underlying library if available, or null otherwise.
     */
    val version: String?
        get() = if (isAvailable) opusGetVersion() else null

    init {
        ExoPlayerLibraryInfo.registerModule("goog.exo.opus")
    }

    /**
     * Override the names of the Opus native libraries. If an application wishes to call this method,
     * it must do so before calling any other method defined by this class, and before instantiating a
     * [LibopusAudioRenderer] instance.
     *
     * @param libraries The names of the Opus native libraries.
     */
    @JvmStatic
    fun setLibraries(libraries: Array<String>) {
        LOADER.setLibraries(*libraries)
    }

    external fun opusGetVersion(): String
    external fun opusIsSecureDecodeSupported(): Boolean
}
