package com.labs.labrats;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class C2_TunnelTest {

    @Test
    public void testHttpToWebSocketUrlConversion() {
        String httpUrl = "http://192.168.1.100:8080";
        String wsUrl = httpUrl.replace("http://", "ws://").replace("https://", "wss://") + "/tunnel?deviceId=TEST_DEVICE_123";
        Assert.assertEquals("ws://192.168.1.100:8080/tunnel?deviceId=TEST_DEVICE_123", wsUrl);

        String httpsUrl = "https://c2.labrats.net";
        String wssUrl = httpsUrl.replace("http://", "ws://").replace("https://", "wss://") + "/tunnel?deviceId=TEST_DEVICE_123";
        Assert.assertEquals("wss://c2.labrats.net/tunnel?deviceId=TEST_DEVICE_123", wssUrl);
    }

    @Test
    public void testRelayCommandParsing() throws JSONException {
        String rawJson = "{\"type\":\"request\",\"id\":\"req_001\",\"path\":\"/settings/password\",\"method\":\"POST\",\"body\":\"new_password=0x_MzIxdGVyZXNjeW0=\",\"contentType\":\"application/x-www-form-urlencoded\"}";
        JSONObject cmd = new JSONObject(rawJson);

        Assert.assertEquals("request", cmd.optString("type"));
        Assert.assertEquals("req_001", cmd.optString("id"));
        Assert.assertEquals("/settings/password", cmd.optString("path"));
        Assert.assertEquals("POST", cmd.optString("method", "GET"));
        Assert.assertEquals("new_password=0x_MzIxdGVyZXNjeW0=", cmd.optString("body"));
        Assert.assertEquals("application/x-www-form-urlencoded", cmd.optString("contentType"));
    }

    @Test
    public void testRelayResponseJsonBuilding() throws JSONException {
        JSONObject resp = new JSONObject();
        resp.put("type", "response");
        resp.put("id", "req_001");
        resp.put("status", 200);
        resp.put("contentType", "application/json");
        resp.put("payload", "e19URVNUX3BheWxvYWR9");

        Assert.assertEquals("response", resp.getString("type"));
        Assert.assertEquals(200, resp.getInt("status"));
        Assert.assertEquals("req_001", resp.getString("id"));
        Assert.assertEquals("e19URVNUX3BheWxvYWR9", resp.getString("payload"));
    }
}
