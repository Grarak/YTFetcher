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

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.decoder.DecoderInputBuffer
import com.google.android.exoplayer2.decoder.SimpleDecoder
import com.google.android.exoplayer2.decoder.SimpleOutputBuffer
import com.google.android.exoplayer2.drm.DecryptionException
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Opus decoder.
 */
/* package */  class OpusDecoder
/**
 * Creates an Opus decoder.
 *
 * @param numInputBuffers The number of input buffers.
 * @param numOutputBuffers The number of output buffers.
 * @param initialInputBufferSize The initial size of each input buffer.
 * @param initializationData Codec-specific initialization data. The first element must contain an
 * opus header. Optionally, the list may contain two additional buffers, which must contain
 * the encoder delay and seek pre roll values in nanoseconds, encoded as longs.
 * @param exoMediaCrypto The [ExoMediaCrypto] object required for decoding encrypted
 * content. Maybe null and can be ignored if decoder does not handle encrypted content.
 * @throws OpusDecoderException Thrown if an exception occurs when initializing the decoder.
 */
@Throws(OpusDecoderException::class)
constructor(numInputBuffers: Int, numOutputBuffers: Int, initialInputBufferSize: Int,
            initializationData: List<ByteArray>, private val exoMediaCrypto: ExoMediaCrypto?) : SimpleDecoder<DecoderInputBuffer, SimpleOutputBuffer, OpusDecoderException>(arrayOfNulls(numInputBuffers), arrayOfNulls(numOutputBuffers)) {

    /**
     * Returns the channel count of output audio.
     */
    val channelCount: Int
    private val headerSkipSamples: Int
    private val headerSeekPreRollSamples: Int
    private val nativeDecoderContext: Long

    private var skipSamples: Int = 0

    /**
     * Returns the sample rate of output audio.
     */
    val sampleRate: Int
        get() = SAMPLE_RATE

    init {
        if (!OpusLibrary.isAvailable) {
            throw OpusDecoderException("Failed to load decoder native libraries.")
        }
        if (exoMediaCrypto != null && !OpusLibrary.opusIsSecureDecodeSupported()) {
            throw OpusDecoderException("Opus decoder does not support secure decode.")
        }
        val headerBytes = initializationData[0]
        if (headerBytes.size < 19) {
            throw OpusDecoderException("Header size is too small.")
        }
        channelCount = headerBytes[9].toInt() and 0xFF
        if (channelCount > 8) {
            throw OpusDecoderException("Invalid channel count: $channelCount")
        }
        val preskip = readLittleEndian16(headerBytes, 10)
        val gain = readLittleEndian16(headerBytes, 16)

        val streamMap = ByteArray(8)
        val numStreams: Int
        val numCoupled: Int
        if (headerBytes[18].toInt() == 0) { // Channel mapping
            // If there is no channel mapping, use the defaults.
            if (channelCount > 2) { // Maximum channel count with default layout.
                throw OpusDecoderException("Invalid Header, missing stream map.")
            }
            numStreams = 1
            numCoupled = if (channelCount == 2) 1 else 0
            streamMap[0] = 0
            streamMap[1] = 1
        } else {
            if (headerBytes.size < 21 + channelCount) {
                throw OpusDecoderException("Header size is too small.")
            }
            // Read the channel mapping.
            numStreams = headerBytes[19].toInt() and 0xFF
            numCoupled = headerBytes[20].toInt() and 0xFF
            System.arraycopy(headerBytes, 21, streamMap, 0, channelCount)
        }
        if (initializationData.size == 3) {
            if (initializationData[1].size != 8 || initializationData[2].size != 8) {
                throw OpusDecoderException("Invalid Codec Delay or Seek Preroll")
            }
            val codecDelayNs = ByteBuffer.wrap(initializationData[1]).order(ByteOrder.nativeOrder()).long
            val seekPreRollNs = ByteBuffer.wrap(initializationData[2]).order(ByteOrder.nativeOrder()).long
            headerSkipSamples = nsToSamples(codecDelayNs)
            headerSeekPreRollSamples = nsToSamples(seekPreRollNs)
        } else {
            headerSkipSamples = preskip
            headerSeekPreRollSamples = DEFAULT_SEEK_PRE_ROLL_SAMPLES
        }
        nativeDecoderContext = opusInit(SAMPLE_RATE, channelCount, numStreams, numCoupled, gain,
                streamMap)
        if (nativeDecoderContext == 0L) {
            throw OpusDecoderException("Failed to initialize decoder")
        }
        setInitialInputBufferSize(initialInputBufferSize)
    }

    override fun getName(): String {
        return "libopus" + OpusLibrary.version!!
    }

    override fun createInputBuffer(): DecoderInputBuffer {
        return DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT)
    }

    override fun createOutputBuffer(): SimpleOutputBuffer {
        return SimpleOutputBuffer(this)
    }

    override fun createUnexpectedDecodeException(error: Throwable): OpusDecoderException {
        return OpusDecoderException("Unexpected decode error", error)
    }

    override fun decode(
            inputBuffer: DecoderInputBuffer, outputBuffer: SimpleOutputBuffer, reset: Boolean): OpusDecoderException? {
        if (reset) {
            opusReset(nativeDecoderContext)
            // When seeking to 0, skip number of samples as specified in opus header. When seeking to
            // any other time, skip number of samples as specified by seek preroll.
            skipSamples = if (inputBuffer.timeUs == 0L) headerSkipSamples else headerSeekPreRollSamples
        }
        val inputData = inputBuffer.data
        val cryptoInfo = inputBuffer.cryptoInfo
        val result = if (inputBuffer.isEncrypted)
            opusSecureDecode(nativeDecoderContext, inputBuffer.timeUs, inputData, inputData.limit(),
                    outputBuffer, SAMPLE_RATE, exoMediaCrypto, cryptoInfo.mode,
                    cryptoInfo.key, cryptoInfo.iv, cryptoInfo.numSubSamples,
                    cryptoInfo.numBytesOfClearData, cryptoInfo.numBytesOfEncryptedData)
        else
            opusDecode(nativeDecoderContext, inputBuffer.timeUs, inputData, inputData.limit(),
                    outputBuffer)
        if (result < 0) {
            return if (result == DRM_ERROR) {
                val message = "Drm error: " + opusGetErrorMessage(nativeDecoderContext)
                val cause = DecryptionException(
                        opusGetErrorCode(nativeDecoderContext), message)
                OpusDecoderException(message, cause)
            } else {
                OpusDecoderException("Decode error: " + opusGetErrorMessage(result.toLong()))
            }
        }

        val outputData = outputBuffer.data
        outputData.position(0)
        outputData.limit(result)
        if (skipSamples > 0) {
            val bytesPerSample = channelCount * 2
            val skipBytes = skipSamples * bytesPerSample
            if (result <= skipBytes) {
                skipSamples -= result / bytesPerSample
                outputBuffer.addFlag(C.BUFFER_FLAG_DECODE_ONLY)
                outputData.position(result)
            } else {
                skipSamples = 0
                outputData.position(skipBytes)
            }
        }
        return null
    }

    override fun release() {
        super.release()
        opusClose(nativeDecoderContext)
    }

    private external fun opusInit(sampleRate: Int, channelCount: Int, numStreams: Int, numCoupled: Int,
                                  gain: Int, streamMap: ByteArray): Long

    private external fun opusDecode(decoder: Long, timeUs: Long, inputBuffer: ByteBuffer, inputSize: Int,
                                    outputBuffer: SimpleOutputBuffer): Int

    private external fun opusSecureDecode(decoder: Long, timeUs: Long, inputBuffer: ByteBuffer,
                                          inputSize: Int, outputBuffer: SimpleOutputBuffer, sampleRate: Int,
                                          mediaCrypto: ExoMediaCrypto?, inputMode: Int, key: ByteArray, iv: ByteArray,
                                          numSubSamples: Int, numBytesOfClearData: IntArray, numBytesOfEncryptedData: IntArray): Int

    private external fun opusClose(decoder: Long)
    private external fun opusReset(decoder: Long)
    private external fun opusGetErrorCode(decoder: Long): Int
    private external fun opusGetErrorMessage(decoder: Long): String

    companion object {

        private const val DEFAULT_SEEK_PRE_ROLL_SAMPLES = 3840

        /**
         * Opus streams are always decoded at 48000 Hz.
         */
        private const val SAMPLE_RATE = 48000

        private const val NO_ERROR = 0
        private const val DECODE_ERROR = -1
        private const val DRM_ERROR = -2

        private fun nsToSamples(ns: Long): Int {
            return (ns * SAMPLE_RATE / 1000000000).toInt()
        }

        private fun readLittleEndian16(input: ByteArray, offset: Int): Int {
            var value = input[offset].toInt() and 0xFF
            value = value or (input[offset + 1].toInt() and 0xFF shl 8)
            return value
        }
    }

}
