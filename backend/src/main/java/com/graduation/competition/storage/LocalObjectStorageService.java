package com.graduation.competition.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 本地文件存储仅用于开发与测试，接口路径与 MinIO 模式保持一致。
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "filesystem")
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path root;

    public LocalObjectStorageService(@Value("${app.storage.local-directory:uploads}") String directory)
            throws IOException {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String put(String objectName, String contentType, byte[] content) {
        Path target = root.resolve(objectName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法存储路径");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "file:///" + target.toString().replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件写入失败", exception);
        }
    }

    @Override
    public InputStream get(String fileUrl) {
        try {
            return Files.newInputStream(resolve(fileUrl));
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件读取失败", exception);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Files.deleteIfExists(resolve(fileUrl));
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件删除失败", exception);
        }
    }

    private Path resolve(String fileUrl) {
        String relative = fileUrl.replaceFirst("^file:///", "").replace('/', '\\');
        Path path = Path.of(relative).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法存储路径");
        }
        return path;
    }
}

