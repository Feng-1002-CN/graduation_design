package com.graduation.competition.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    String put(String objectName, String contentType, byte[] content);

    InputStream get(String fileUrl);

    void delete(String fileUrl);
}

