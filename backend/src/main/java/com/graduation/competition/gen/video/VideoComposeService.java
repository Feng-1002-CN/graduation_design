package com.graduation.competition.gen.video;

import com.graduation.competition.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频编排：脚本文本 → 音轨 → 用户素材/占位图 → FFmpeg 合成。
 */
@Service
@RequiredArgsConstructor
public class VideoComposeService {

    private final TtsService ttsService;
    private final FfmpegService ffmpegService;
    private final ObjectStorageService storageService;

    public Path compose(List<VideoScene> scenes, Path workspace) {
        try {
            List<FfmpegService.SceneMedia> media = new ArrayList<>();
            for (int index = 0; index < scenes.size(); index++) {
                VideoScene scene = scenes.get(index);
                Path audio = ttsService.synthesize(
                        scene.text(), workspace.resolve("audio-" + index + ".wav"));
                Path image = scene.imageUrl() == null || scene.imageUrl().isBlank()
                        ? placeholder(workspace.resolve("scene-" + index + ".png"), index + 1, scene.text())
                        : material(scene.imageUrl(), workspace.resolve("source-" + index));
                media.add(new FfmpegService.SceneMedia(image, audio, scene.durationSec()));
            }
            return ffmpegService.compose(media, workspace, workspace.resolve("project-video.mp4"));
        } catch (IOException exception) {
            throw new IllegalStateException("视频临时素材准备失败", exception);
        }
    }

    private Path material(String fileUrl, Path target) throws IOException {
        try (InputStream input = storageService.get(fileUrl)) {
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    private Path placeholder(Path target, int index, String text) throws IOException {
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(22, 61, 51));
        graphics.fillRect(0, 0, 1280, 720);
        graphics.setColor(new Color(226, 120, 79));
        graphics.fillRoundRect(70, 70, 112, 112, 24, 24);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Serif", Font.BOLD, 52));
        graphics.drawString(String.format("%02d", index), 92, 145);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 40));
        graphics.drawString("学科竞赛项目演示", 70, 290);
        graphics.setColor(new Color(223, 230, 225));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 25));
        drawWrapped(graphics, text, 70, 360, 1100, 42);
        graphics.dispose();
        ImageIO.write(image, "png", target.toFile());
        return target;
    }

    private void drawWrapped(Graphics2D graphics, String text, int x, int y, int width, int lineHeight) {
        int line = 0;
        int start = 0;
        while (start < text.length() && line < 5) {
            int end = Math.min(text.length(), start + 28);
            graphics.drawString(text.substring(start, end), x, y + line * lineHeight);
            start = end;
            line++;
        }
    }
}

