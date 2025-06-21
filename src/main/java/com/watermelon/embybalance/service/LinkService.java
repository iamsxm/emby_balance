package com.watermelon.embybalance.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LinkService {

    @Autowired
    private Cd2Service cd2Service;

    @Autowired
    private DriveService driveService;

    @Autowired
    private Cache<String, String> pathToFileIdCache;

    public String getOrCreateLink(String path) throws Exception {
        String fileId = pathToFileIdCache.getIfPresent(path);
        if (fileId == null) {
            fileId = cd2Service.getFileIdByPath(path);
            if (fileId == null) {
                throw new Exception("File not found in CD2 for path: " + path);
            }
            pathToFileIdCache.put(path, fileId);
        }
        return driveService.getDownloadLink(fileId);
    }
}