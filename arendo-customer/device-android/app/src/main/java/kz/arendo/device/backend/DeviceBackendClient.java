package kz.arendo.device.backend;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** WebSocket transport for the postamat device contract. Never logs the device token. */
public final class DeviceBackendClient extends WebSocketClient {
    public interface Listener {
        void onOpen();
        void onMessage(JSONObject message);
        void onClose(String reason);
        void onError(Exception error);
    }

    private final Listener listener;

    public DeviceBackendClient(URI uri, String deviceToken, Listener listener) {
        super(uri, createHeaders(deviceToken));
        if (listener == null) {
            throw new IllegalArgumentException("Listener is required");
        }
        this.listener = listener;
        setConnectionLostTimeout(45);
    }

    private static Map<String, String> createHeaders(String deviceToken) {
        Map<String, String> headers = new HashMap<>();
        if (deviceToken != null && !deviceToken.trim().isEmpty()) {
            headers.put("X-Device-Token", deviceToken.trim());
        }
        return headers;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        listener.onOpen();
    }

    @Override
    public void onMessage(String message) {
        try {
            listener.onMessage(new JSONObject(message));
        } catch (JSONException error) {
            listener.onError(error);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        listener.onClose(reason == null ? "" : reason);
    }

    @Override
    public void onError(Exception error) {
        listener.onError(error);
    }
}
