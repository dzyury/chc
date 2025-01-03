package misc.data;

import lombok.Data;

@Data
public class Token {
    private String access_token;
    private String token_type;
    private int expires_in;
    private String refresh_token;
    private String scope;
    private String uid;
    private String account_id;
}
