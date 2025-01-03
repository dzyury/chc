package misc.drop;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Main {
    private static final String ACCESS_TOKEN = "sl.u.AFc_2-dKvWm9KKbEhS0KqqlQIIOqFCAMEL9Ai0lQ63R6-l1ECoxmXxOf79bhrJTHl2lO78lEsoiK1QA7HF1kDAyJdaVBH222tevYB-Gy6hQ3-R3IOQrMSYK7EExAWt3qfEGghXlp3txUlD1IQY7Yr6R9dSOmp-R9bVOyEt-SNR89P7KuCdyaneKTS5UBPXgh8j4_kOSq2HSFVD0xLpUiYyzINKzHx5z0yC16-N9WQ3Y-YAU16MSpptA9OlfnZSThC4QNOY9_ZOVC21x-1ah9rY-OijcVdPbpzNaAhyoH4fMBOIJ6Fiouwv3q5zYZo6osBSFv_CZPwzz_8FC0IvPXZdjsEpEr-Qau4v25ApsCQgfWX-y6lbSUHvywR0nl0WJ9qTu93WRfYKXHB0DrLF9re1ki8KNkwu3CGk-pskHMkAcYj1dzN3X039ooX8BTnGz6UNecxK_vyHEDefp-q-c4KuLEhvinkML6_AwDafi6fJ95wUwh2Jimre-nYHaBZqdhdpj7VnUEdAU2-vbWqdPpwjXNM5MLBByHnacQu-XRuHDpdzyII-mFxDeN1SECaUf_Nvk1qKZQT8TJRGMSa87vb5T4uxSuz5HMzkAcJGswxfAIlCTDdAAWRSJIm7jkaskvGebQc0de-cODARY86ey0Ij8Z6rK8OIb2Ct7gIjpco9jkzA2ZD6pu2i_C5YnAHprv2me2NJc-R8AyY4KTdNnexhgM63H833W-kTNWXwxhnUEd-GzYiYmTU12uHASg_qH8yfSt4gV9L9xOgfy_zDsX9jUQGqXykpES5Trd9il6qVq1PlveHBnBEHvG172ddkC6Ycygu0yjAN8Ie0sbr95H5aR-O1xxWbmjQtbHmGYLNGhV3oMlgrk-1CqZSq-NgzQzonWswwxV4t_goc5_83-X3YXxz473vcpWEvVUjdrNd2heNGrfWXAKubzh3woghA-_dFyvYGbzyyelLqMJvfKzzPal168kndmQUQVPRvtM3qiUmGE_4_2YrwIqCoMxowD48UlYezDnRoYWwNh5T5eNpb_andhQ7FoiEl-5Ke-mqClmBUl7lL75GGcfxhL7N4fm73fCjbKrsM4Ys1K-D1EWvI6OIHPUazBL6DaCrqJdHMy5D1ukazjacFNAWYMcMEz0ddc5cUrm731et2G_3XtlCfXfMkwFifExDt7SQFHwVp9QLlexYuRFGFXj571oAC0GFaw02jvgdD4k4NAnkt3SMycgP4AHw1weIMwfv7gVZqZ_g7mLdlZpgeIFNO1kvdWUqYixsw1lM0zmgD6zjEX_vVf9Wfdx9dEGLpr1O-JflYfE8ob8jd0uHWFEkmN3xYmr7nBcoTKI3vGIvBwYVnqG_5H9EXELpiqiMlbC1mwuda8b4aFjqusTMWUmnNlTF4uiw2Nt7X9wS5Yg0n8iHAUPS_Rs";

    public static void main(String[] args) throws DbxException, IOException, InterruptedException {
        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        // Get current account info
        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        // Get files and folder metadata from Dropbox root directory
        ObjectMapper mapper = new ObjectMapper();
        ListFolderResult result = client.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                System.out.println(metadata.getPathLower());
                if (metadata instanceof FolderMetadata) continue;

                fetch(metadata, client, mapper);
            }

            System.out.println("-");
            if (!result.getHasMore()) {
                Thread.sleep(100_000);
            }

            result = client.files().listFolderContinue(result.getCursor());
        }
    }

    private static void fetch(Metadata metadata, DbxClientV2 client, ObjectMapper mapper) throws IOException, DbxException {
        try (
                DbxDownloader<FileMetadata> content = client.files().download(metadata.getPathLower());
                InputStream is = content.getInputStream()
        ) {
//            mapper.readValue(is);
            String text = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
            System.out.println("---");
            System.out.println(text);
            System.out.println("---");
        }
    }
}