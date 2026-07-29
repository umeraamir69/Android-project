package com.lecturelens.data.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes any MediaExtractor-supported audio file to mono 16-bit PCM for
 * Speech-to-Text v1 {@code LINEAR16} inline recognize.
 */
public final class AudioToLinear16 {

    public static final class PcmAudio {
        @NonNull public final byte[] pcmLittleEndian;
        public final int sampleRateHz;

        public PcmAudio(@NonNull byte[] pcmLittleEndian, int sampleRateHz) {
            this.pcmLittleEndian = pcmLittleEndian;
            this.sampleRateHz = sampleRateHz;
        }
    }

    private AudioToLinear16() {
    }

    @NonNull
    public static PcmAudio convert(@NonNull File input) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(input.getAbsolutePath());
            int track = selectAudioTrack(extractor);
            if (track < 0) {
                throw new IOException("No audio track in " + input.getName());
            }
            extractor.selectTrack(track);
            MediaFormat format = extractor.getTrackFormat(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null || !mime.startsWith("audio/")) {
                throw new IOException("Unsupported audio mime: " + mime);
            }

            // Already raw PCM — rare, but avoid a decoder round-trip.
            if (MediaFormat.MIMETYPE_AUDIO_RAW.equals(mime)) {
                return readRawPcm(extractor, format);
            }

            MediaCodec codec = MediaCodec.createDecoderByType(mime);
            ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();
            int sampleRate;
            try {
                codec.configure(format, null, null, 0);
                codec.start();
                sampleRate = decodeAll(extractor, codec, format, pcmOut);
            } finally {
                try {
                    codec.stop();
                } catch (Exception ignored) {
                }
                codec.release();
            }

            byte[] pcm = pcmOut.toByteArray();
            if (pcm.length == 0) {
                throw new IOException("Decoded audio is empty.");
            }
            return new PcmAudio(pcm, sampleRate);
        } finally {
            extractor.release();
        }
    }

    private static int selectAudioTrack(@NonNull MediaExtractor extractor) {
        int count = extractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private static PcmAudio readRawPcm(@NonNull MediaExtractor extractor,
                                      @NonNull MediaFormat format) throws IOException {
        int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 16_000;
        int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
        while (true) {
            int size = extractor.readSampleData(buffer, 0);
            if (size < 0) {
                break;
            }
            byte[] chunk = new byte[size];
            buffer.position(0);
            buffer.get(chunk);
            out.write(chunk);
            extractor.advance();
            buffer.clear();
        }
        byte[] pcm = out.toByteArray();
        if (channels > 1) {
            pcm = downmixToMono(pcm, channels);
        }
        return new PcmAudio(pcm, sampleRate);
    }

    private static int decodeAll(@NonNull MediaExtractor extractor,
                                 @NonNull MediaCodec codec,
                                 @NonNull MediaFormat inputFormat,
                                 @NonNull ByteArrayOutputStream pcmOut) throws IOException {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean outputDone = false;
        int sampleRate = inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                ? inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 16_000;
        int channels = inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                ? inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

        while (!outputDone) {
            if (!inputDone) {
                int inIndex = codec.dequeueInputBuffer(10_000);
                if (inIndex >= 0) {
                    ByteBuffer inBuf = codec.getInputBuffer(inIndex);
                    if (inBuf == null) {
                        throw new IOException("Decoder input buffer was null.");
                    }
                    inBuf.clear();
                    int sampleSize = extractor.readSampleData(inBuf, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long pts = extractor.getSampleTime();
                        codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0);
                        extractor.advance();
                    }
                }
            }

            int outIndex = codec.dequeueOutputBuffer(info, 10_000);
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat outFormat = codec.getOutputFormat();
                if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                }
                if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                }
            } else if (outIndex >= 0) {
                ByteBuffer outBuf = codec.getOutputBuffer(outIndex);
                if (outBuf != null && info.size > 0) {
                    byte[] chunk = new byte[info.size];
                    outBuf.position(info.offset);
                    outBuf.get(chunk);
                    if (channels > 1) {
                        chunk = downmixToMono(chunk, channels);
                    }
                    pcmOut.write(chunk);
                }
                codec.releaseOutputBuffer(outIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }
        return sampleRate;
    }

    @NonNull
    private static byte[] downmixToMono(@NonNull byte[] interleaved, int channels) {
        if (channels <= 1) {
            return interleaved;
        }
        int frameBytes = channels * 2; // 16-bit
        int frames = interleaved.length / frameBytes;
        byte[] mono = new byte[frames * 2];
        ByteBuffer in = ByteBuffer.wrap(interleaved).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer out = ByteBuffer.wrap(mono).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += in.getShort();
            }
            out.putShort((short) (sum / channels));
        }
        return mono;
    }
}
