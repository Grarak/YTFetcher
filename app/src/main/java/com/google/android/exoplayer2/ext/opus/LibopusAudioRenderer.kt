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

import android.os.Handler
import com.google.android.exoplayer2.BaseRenderer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.SimpleDecoderAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.util.MimeTypes

/**
 * Decodes and renders audio using the native Opus decoder.
 */
class LibopusAudioRenderer : SimpleDecoderAudioRenderer {

    private var decoder: OpusDecoder? = null

    constructor() : this(null, null)

    /**
     * @param eventHandler A handler to use when delivering events to `eventListener`. May be
     * null if delivery of events is not required.
     * @param eventListener A listener of events. May be null if delivery of events is not required.
     * @param audioProcessors Optional [AudioProcessor]s that will process audio before output.
     */
    constructor(eventHandler: Handler?, eventListener: AudioRendererEventListener?,
                vararg audioProcessors: AudioProcessor) : super(eventHandler, eventListener, *audioProcessors) {
    }

    /**
     * @param eventHandler A handler to use when delivering events to `eventListener`. May be
     * null if delivery of events is not required.
     * @param eventListener A listener of events. May be null if delivery of events is not required.
     * @param drmSessionManager For use with encrypted media. May be null if support for encrypted
     * media is not required.
     * @param playClearSamplesWithoutKeys Encrypted media may contain clear (un-encrypted) regions.
     * For example a media file may start with a short clear region so as to allow playback to
     * begin in parallel with key acquisition. This parameter specifies whether the renderer is
     * permitted to play clear regions of encrypted media files before `drmSessionManager`
     * has obtained the keys necessary to decrypt encrypted regions of the media.
     * @param audioProcessors Optional [AudioProcessor]s that will process audio before output.
     */
    constructor(eventHandler: Handler, eventListener: AudioRendererEventListener,
                drmSessionManager: DrmSessionManager<ExoMediaCrypto>, playClearSamplesWithoutKeys: Boolean,
                vararg audioProcessors: AudioProcessor) : super(eventHandler, eventListener, null, drmSessionManager, playClearSamplesWithoutKeys,
            *audioProcessors)

    override fun supportsFormatInternal(drmSessionManager: DrmSessionManager<ExoMediaCrypto>?,
                                        format: Format): Int {
        return if (!OpusLibrary.isAvailable || !MimeTypes.AUDIO_OPUS.equals(format.sampleMimeType, ignoreCase = true)) {
            RendererCapabilities.FORMAT_UNSUPPORTED_TYPE
        } else if (!supportsOutputEncoding(C.ENCODING_PCM_16BIT)) {
            RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE
        } else if (!BaseRenderer.supportsFormatDrm(drmSessionManager, format.drmInitData)) {
            RendererCapabilities.FORMAT_UNSUPPORTED_DRM
        } else {
            RendererCapabilities.FORMAT_HANDLED
        }
    }

    @Throws(OpusDecoderException::class)
    override fun createDecoder(format: Format, mediaCrypto: ExoMediaCrypto?): OpusDecoder {
        decoder = OpusDecoder(NUM_BUFFERS, NUM_BUFFERS, INITIAL_INPUT_BUFFER_SIZE,
                format.initializationData, mediaCrypto)
        return decoder!!
    }

    override fun getOutputFormat(): Format {
        return Format.createAudioSampleFormat(null, MimeTypes.AUDIO_RAW, null, Format.NO_VALUE,
                Format.NO_VALUE, decoder!!.channelCount, decoder!!.sampleRate, C.ENCODING_PCM_16BIT, null, null, 0, null)
    }

    companion object {

        private const val NUM_BUFFERS = 16
        private const val INITIAL_INPUT_BUFFER_SIZE = 960 * 6
    }

}
