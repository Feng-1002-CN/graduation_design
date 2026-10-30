package com.graduation.competition.gen.video;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg 外部进程封装。所有参数均以独立 argv 传递，不经过 shell，避免命令注入。
 */
@Slf4j
@Service
public class FfmpegService {

    private final String executable;
    private volatile String videoEncoder;

    public FfmpegService(@Value("${app.ffmpeg.executable:ffmpeg}") String executable) {
        this.executable = executable;
    }

    /** 注意：这里只合成用户素材、TTS 音频与字幕，不调用任何视频生成模型。 */
    public Path compose(List<SceneMedia> scenes, Path workspace, Path output) {
        ensureAvailable();
        try {
            List<Path> clips = new ArrayList<>();
            for (int index = 0; index < scenes.size(); index++) {
                SceneMedia scene = scenes.get(index);
                Path clip = workspace.resolve("scene-" + index + ".mp4");
                run(List.of(
                        executable, "-y",
                        "-loop", "1", "-i", scene.image().toString(),
                        "-i", scene.audio().toString(),
                        "-t", String.valueOf(scene.durationSec()),
                        "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,"
                                + "pad=1280:720:(ow-iw)/2:(oh-ih)/2,format=yuv420p",
                        "-r", "25",
                        "-c:v", videoEncoder(),
                        "-preset", "veryfast",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-shortest",
                        clip.toString()));
                clips.add(clip);
            }
            Path concat = workspace.resolve("concat.txt");
            StringBuilder list = new StringBuilder();
            for (Path clip : clips) {
                list.append("file '")
                        .append(clip.toAbsolutePath().toString().replace("\\", "/")
                                .replace("'", "'\\''"))
                        .append("'\n");
            }
            Files.writeString(concat, list, StandardCharsets.UTF_8);
            run(List.of(executable, "-y", "-f", "concat", "-safe", "0",
                    "-i", concat.toString(), "-c", "copy", output.toString()));
            return output;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "视频合成临时文件处理失败");
        }
    }

    public void ensureAvailable() {
        try {
            Process process = new ProcessBuilder(executable, "-version")
                    .redirectErrorStream(true).start();
            boolean ended = process.waitFor(5, TimeUnit.SECONDS);
            if (!ended || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
                        "FFmpeg 不可用，请安装 FFmpeg 或配置 FFMPEG_EXECUTABLE");
            }
        } catch (IOException exception) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
                    "FFmpeg 不可用，请安装 FFmpeg 或配置 FFMPEG_EXECUTABLE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.INTERNAL_ERROR, "FFmpeg 检查被中断");
        }
    }

    private String videoEncoder() {
        if (videoEncoder != null) {
            return videoEncoder;
        }
        try {
            Process process = new ProcessBuilder(executable, "-hide_banner", "-encoders")
                    .redirectErrorStream(true).start();
            String encoders = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "无法读取 FFmpeg 编码器列表");
            }
            for (String candidate : List.of("libx264", "libopenh264", "h264_mf", "mpeg4")) {
                if (encoders.contains(candidate)) {
                    videoEncoder = candidate;
                    return candidate;
                }
            }
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "FFmpeg 缺少可用的视频编码器");
        } catch (IOException exception) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "FFmpeg 编码器探测失败");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.INTERNAL_ERROR, "FFmpeg 编码器探测被中断");
        }
    }

    private void run(List<String> command) {
        try {
            Path logFile = Files.createTempFile("ffmpeg-", ".log");
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BizException(ErrorCode.INTERNAL_ERROR, "FFmpeg 合成超时");
            }
            if (process.exitValue() != 0) {
                String log = Files.readString(logFile, StandardCharsets.UTF_8);
                Files.deleteIfExists(logFile);
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "FFmpeg 合成失败: " + tail(log, 1000));
            }
            Files.deleteIfExists(logFile);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "FFmpeg 进程启动失败");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.INTERNAL_ERROR, "FFmpeg 合成被中断");
        }
    }

    private String tail(String value, int max) {
        return value.length() <= max ? value : value.substring(value.length() - max);
    }

    public record SceneMedia(Path image, Path audio, int durationSec) {
    }
}

