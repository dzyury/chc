package misc.drop;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.users.FullAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import misc.data.Token;

import java.io.InputStream;

import static com.dropbox.core.v2.auth.AuthError.Tag.EXPIRED_ACCESS_TOKEN;
import static com.dropbox.core.v2.files.WriteMode.OVERWRITE;

@Data
// https://www.dropboxforum.com/discussions/101000014/issue-in-generating-access-token/592667
// https://www.dropbox.com/developers/documentation/http/documentation#oauth2-token
public class DBClient {
    private String expiredAccessToken = "sl.u.AFc_2-dKvWm9KKbEhS0KqqlQIIOqFCAMEL9Ai0lQ63R6-l1ECoxmXxOf79bhrJTHl2lO78lEsoiK1QA7HF1kDAyJdaVBH222tevYB-Gy6hQ3-R3IOQrMSYK7EExAWt3qfEGghXlp3txUlD1IQY7Yr6R9dSOmp-R9bVOyEt-SNR89P7KuCdyaneKTS5UBPXgh8j4_kOSq2HSFVD0xLpUiYyzINKzHx5z0yC16-N9WQ3Y-YAU16MSpptA9OlfnZSThC4QNOY9_ZOVC21x-1ah9rY-OijcVdPbpzNaAhyoH4fMBOIJ6Fiouwv3q5zYZo6osBSFv_CZPwzz_8FC0IvPXZdjsEpEr-Qau4v25ApsCQgfWX-y6lbSUHvywR0nl0WJ9qTu93WRfYKXHB0DrLF9re1ki8KNkwu3CGk-pskHMkAcYj1dzN3X039ooX8BTnGz6UNecxK_vyHEDefp-q-c4KuLEhvinkML6_AwDafi6fJ95wUwh2Jimre-nYHaBZqdhdpj7VnUEdAU2-vbWqdPpwjXNM5MLBByHnacQu-XRuHDpdzyII-mFxDeN1SECaUf_Nvk1qKZQT8TJRGMSa87vb5T4uxSuz5HMzkAcJGswxfAIlCTDdAAWRSJIm7jkaskvGebQc0de-cODARY86ey0Ij8Z6rK8OIb2Ct7gIjpco9jkzA2ZD6pu2i_C5YnAHprv2me2NJc-R8AyY4KTdNnexhgM63H833W-kTNWXwxhnUEd-GzYiYmTU12uHASg_qH8yfSt4gV9L9xOgfy_zDsX9jUQGqXykpES5Trd9il6qVq1PlveHBnBEHvG172ddkC6Ycygu0yjAN8Ie0sbr95H5aR-O1xxWbmjQtbHmGYLNGhV3oMlgrk-1CqZSq-NgzQzonWswwxV4t_goc5_83-X3YXxz473vcpWEvVUjdrNd2heNGrfWXAKubzh3woghA-_dFyvYGbzyyelLqMJvfKzzPal168kndmQUQVPRvtM3qiUmGE_4_2YrwIqCoMxowD48UlYezDnRoYWwNh5T5eNpb_andhQ7FoiEl-5Ke-mqClmBUl7lL75GGcfxhL7N4fm73fCjbKrsM4Ys1K-D1EWvI6OIHPUazBL6DaCrqJdHMy5D1ukazjacFNAWYMcMEz0ddc5cUrm731et2G_3XtlCfXfMkwFifExDt7SQFHwVp9QLlexYuRFGFXj571oAC0GFaw02jvgdD4k4NAnkt3SMycgP4AHw1weIMwfv7gVZqZ_g7mLdlZpgeIFNO1kvdWUqYixsw1lM0zmgD6zjEX_vVf9Wfdx9dEGLpr1O-JflYfE8ob8jd0uHWFEkmN3xYmr7nBcoTKI3vGIvBwYVnqG_5H9EXELpiqiMlbC1mwuda8b4aFjqusTMWUmnNlTF4uiw2Nt7X9wS5Yg0n8iHAUPS_Rs";
    private String refreshToken = "8tdJdkB-SmgAAAAAAAAAAZPFOjP1iBl8awuWlNIeopUbJMht4XJPZ3fqePFywOsz";
    private DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox").build();
    private DbxCredential credential = createCredential();
    private DbxClientV2 client = new DbxClientV2(config, credential);

    @SneakyThrows
    private DbxCredential createCredential() {
        ObjectMapper mapper = new ObjectMapper();
        try (var stream = Main.class.getResourceAsStream("/token.json")) {
            Token token = mapper.readValue(stream, Token.class);
            return new DbxCredential(
                    token.getAccess_token(),
                    System.currentTimeMillis(),
                    token.getRefresh_token(),
                    "fyhzblmaesahb7v",
                    "qshw4rh368w2d5k"
            );
        }
    }

    private <T, E extends Exception> T withRetry(CheckedSupplier<T, E> supplier) throws E, DbxException {
        try {
            return supplier.get();
        } catch (Exception e) {
            // if token is expired
            // refresh access token and try once again
            if (e instanceof InvalidAccessTokenException iate && iate.getAuthError().tag() == EXPIRED_ACCESS_TOKEN) {
                client.refreshAccessToken();
                return supplier.get();
            }
            return null;
        }
    }

    public FullAccount getCurrentAccount() throws DbxException {
        return withRetry(() -> client.users().getCurrentAccount());
    }

    public ListFolderResult listFolder(String path) throws DbxException {
        return withRetry(() -> client.files().listFolder(path));
    }

    public ListFolderResult listFolderContinue(String cursor) throws DbxException {
        return withRetry(() -> client.files().listFolderContinue(cursor));
    }

    public DbxDownloader<FileMetadata> download(String path) throws DbxException {
        return withRetry(() -> client.files().download(path));
    }

    public void upload(String path, InputStream is) throws Exception {
        withRetry(() -> client.files().uploadBuilder(path).withMode(OVERWRITE).uploadAndFinish(is));
    }

    @SneakyThrows
    public <T> T fetch(String path, ObjectMapper mapper, Class<T> clazz) {
        try (DbxDownloader<FileMetadata> content = download(path)) {
            if (content == null) return null;

            try (InputStream is = content.getInputStream()) {
                return mapper.readValue(is, clazz);
            }
        }
    }

}
