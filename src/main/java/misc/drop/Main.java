package misc.drop;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import misc.data.ChcResult;
import misc.data.Logger;
import misc.data.Schedule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;

public class Main {
    private static final String SCHEDULE = "/cat/schedule.json";
    public static final int PERIOD = 300_000;

    public static void main(String[] args) throws Exception {
        // Get current account info
        DBClient dbClient = new DBClient();
        FullAccount account = dbClient.getCurrentAccount();
        Logger.log(account.getName().getDisplayName());
        // Get files and folder metadata from Dropbox root directory
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        ListFolderResult result = dbClient.listFolder("/cat");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                Logger.log(metadata.getPathLower());
                if (metadata instanceof FolderMetadata) continue;
                if (!SCHEDULE.equals(metadata.getPathLower())) continue;

                Schedule schedule = fetch(metadata, dbClient, mapper);
                Logger.log(schedule);
            }

            if (!result.getHasMore()) upload(dbClient, mapper);
            result = dbClient.listFolderContinue(result.getCursor());
        }
    }

    private static void upload(DBClient dbClient, ObjectMapper mapper) throws Exception {
        var rem = System.currentTimeMillis() % PERIOD;
        Thread.sleep(rem);

        var now = LocalDateTime.now();
        ChcResult result = new ChcResult(now, Duration.ofMillis(2), null);
        byte[] bytes = mapper.writeValueAsBytes(result);
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            dbClient.upload("/cat/test" + now.toLocalDate() + ".txt", is);
        }
    }

    private static Schedule fetch(Metadata metadata, DBClient dbClient, ObjectMapper mapper) throws IOException, DbxException {
        try (
                DbxDownloader<FileMetadata> content = dbClient.download(metadata.getPathLower());
                InputStream is = content.getInputStream()
        ) {
            return mapper.readValue(is, Schedule.class);
        }
    }
}