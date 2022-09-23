package ca.bc.gov.educ.api.dataconversion.model;

import org.json.JSONObject;

import java.util.Base64;

public class ResponseObjCache {

    private long tokenExpiry = 0;
    private ResponseObj responseObj;

    // tokenExpiry-[seconds] provides a slight offset, if token WILL expire in
    // [seconds], obtain a new one
    private int offset = 10;

    public ResponseObjCache(int offset) {
        this.offset = offset;
    }

    public ResponseObj getResponseObj() {
        return responseObj;
    }

    public void setResponseObj(ResponseObj responseObj) {
        this.setTokenExpiry(responseObj);
        this.responseObj = responseObj;
    }

    public boolean isExpired(){
        // tokenExpiry-[seconds] provides a slight offset, if token WILL expire in
        // 10 seconds, obtain a new one
        return (tokenExpiry-offset) < (System.currentTimeMillis() / 1000);
    }

    private void setTokenExpiry(ResponseObj responseObj){
        String[] parts = responseObj.getAccess_token().split("\\.");
        JSONObject payload = new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1])));
        this.tokenExpiry = payload.getLong("exp");
    }


}
