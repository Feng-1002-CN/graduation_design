package com.graduation.competition.gen.video;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * 远端 TTS REST + 本地音频占位。本地占位会生成可播放 WAV，但不等同于正式中文配音。
 */
@Slf4j
@Service
public class TtsService {

    private final String baseUrl;
    private final String apiKey;
    private final String voice;

    public TtsService(
            @Value("${app.tts.base-url:}") String baseUrl,
            @Value("${app.tts.api-key:}") String apiKey,
            @Value("${app.tts.voice:zh-CN-XiaoxiaoNeural}") String voice) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.voice = voice;
    }

    public Path synthesize(String text, Path output) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                RestClient client = RestClient.builder().baseUrl(baseUrl).build();
                byte[] audio = client.post()
                        .uri("/tts")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("text", text, "voice", voice, "format", "mp3"))
                        .retrieve()
                        .body(byte[].class);
                if (audio == null || audio.length < 100) {
                    throw new IllegalStateException("TTS 返回空音频");
                }
                java.nio.file.Files.write(output, audio);
                return output;
            } catch (Exception exception) {
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
                        "TTS 服务调用失败: " + exception.getMessage());
            }
        }
        return localPlaceholder(text, output);
    }

    private Path localPlaceholder(String text, Path output) {
        int seconds = Math.min(8, Math.max(2, text.length() / 15));
        float sampleRate = 16_000f;
        int sampleCount = (int) (sampleRate * seconds);
        byte[] pcm = new byte[sampleCount * 2];
        for (int index = 0; index < sampleCount; index++) {
            double time = index / sampleRate;
            double envelope = Math.min(1, Math.min(index / 1200.0, (sampleCount - index) / 1200.0));
            short sample = (short) (Math.sin(2 * Math.PI * (220 + 35 * Math.sin(time * 2)) * time)
                    * 1800 * envelope);
            pcm[index * 2] = (byte) (sample & 0xff);
            pcm[index * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), format, sampleCount)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output.toFile());
            log.info("TTS 未配置，已生成 {} 秒本地演示音轨", seconds);
            return output;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "本地演示音频生成失败");
        }
    }
}

