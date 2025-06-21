package com.watermelon.embybalance.service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;

@Service
public class DriveService {

    private Drive drive;

    @PostConstruct
    public void init() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("token.json"))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

        drive = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Cd2DriveLinkApp").build();
    }

    public String getDownloadLink(String fileId) throws IOException {
        Permission permission = new Permission();
        permission.setType("anyone");
        permission.setRole("reader");

        drive.permissions().create(fileId, permission).execute();

        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }
}