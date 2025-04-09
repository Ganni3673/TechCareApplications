package com.Gmail_Count;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GmailEmailService {

    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/credentials.json";

   
    // Authenticate and build the Gmail service
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        try (FileInputStream credentialsStream = new FileInputStream(CREDENTIALS_FILE_PATH)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(credentialsStream));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")  // Ensure offline access
                    .build();


            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }  catch (TokenResponseException e) {
            System.err.println("Token error: " + e.getDetails());
            if ("invalid_grant".equals(e.getDetails().getError())) {
                System.err.println("Token expired or revoked. Cleaning tokens and retrying...");
                cleanupTokens();  // Delete and retry
                return getCredentials(HTTP_TRANSPORT);  // Retry after cleanup
            }
            throw e; // Re-throw for other errors
        }
    }

    
    
   

    // Delete invalid tokens and force reauthorization
    private void cleanupTokens() throws IOException {
        Path tokenPath = Paths.get(TOKENS_DIRECTORY_PATH);
        if (Files.exists(tokenPath)) {
            Files.walk(tokenPath)
                .map(Path::toFile)
                .forEach(java.io.File::delete);
            Files.deleteIfExists(tokenPath);
            System.out.println("Tokens deleted. Reauthorization required.");
        }
    }


    // Fetch total email count
    public int fetchTotalEmailCount() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        return getAllEmailsCount(service);
    }

    // Helper method to fetch and count email
    private int getAllEmailsCount(Gmail service) throws IOException {
        String userId = "me";
        String query = ""; // Fetch all email
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();

        List<Message> messages = new ArrayList<>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                response = service.users().messages().list(userId)
                        .setQ(query)
                        .setPageToken(response.getNextPageToken())
                        .execute();
            } else {
                break;
            }
        }
        return messages.size();
    }
}
