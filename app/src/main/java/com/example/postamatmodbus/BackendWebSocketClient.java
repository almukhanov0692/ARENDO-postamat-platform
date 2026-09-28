package com.example.postamatmodbus;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** WebSocket transport for the postamat device protocol. */
public final class BackendWebSocketClient {
    public interface Listener {
        void onOpen();
        void onMessage(String message);
        void onClosed(String reason);
        void onError(String message);
    }

    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    private volatile WebSocketClient socket;
    private volatile boolean shouldReconnect;
    private volatile int reconnectAttempt;
    private String url;
    private String token;
    private final boolean allowInsecureTls;

    public BackendWebSocketClient(Listener listener) {
        this(listener, false);
    }

    public BackendWebSocketClient(Listener listener, boolean allowInsecureTls) {
        this.listener = listener;
        this.allowInsecureTls = allowInsecureTls;
    }

    public synchronized void connect(String url, String token) {
        disconnect(false);
        this.url = url.trim();
        this.token = token == null ? "" : token.trim();
        this.shouldReconnect = true;
        this.reconnectAttempt = 0;
        connectInternal();
    }

    public synchronized void disconnect() {
        disconnect(true);
    }

    private synchronized void disconnect(boolean notify) {
        shouldReconnect = false;
        WebSocketClient old = socket;
        socket = null;
        if (old != null) {
            old.close();
        }
        if (notify) {
            listener.onClosed("Отключено пользователем");
        }
    }

    public boolean isOpen() {
        WebSocketClient current = socket;
        return current != null && current.isOpen();
    }

    public void send(String message) {
        WebSocketClient current = socket;
        if (current != null && current.isOpen()) {
            current.send(message);
        }
    }

    private synchronized void connectInternal() {
        if (!shouldReconnect || url == null || url.isEmpty()) {
            return;
        }
        try {
            Map<String, String> headers = new HashMap<>();
            if (!token.isEmpty()) {
                headers.put("X-Device-Token", token);
            }
            WebSocketClient next = new WebSocketClient(new URI(url), headers) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    reconnectAttempt = 0;
                    listener.onOpen();
                }

                @Override
                public void onMessage(String message) {
                    listener.onMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (socket == this) {
                        socket = null;
                    }
                    listener.onClosed(reason == null || reason.isEmpty()
                            ? "Соединение закрыто (" + code + ")" : reason);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception error) {
                    listener.onError(error == null ? "WebSocket error" : error.getMessage());
                }

                @Override
                protected void onSetSSLParameters(SSLParameters parameters) {
                    if (allowInsecureTls) {
                        parameters.setEndpointIdentificationAlgorithm(null);
                    } else {
                        super.onSetSSLParameters(parameters);
                    }
                }
            };
            if (allowInsecureTls && url.startsWith("wss://")) {
                next.setSocketFactory(insecureSocketFactory());
            }
            next.setConnectionLostTimeout(45);
            socket = next;
            next.connect();
        } catch (Exception error) {
            listener.onError(error.getMessage());
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        if (!shouldReconnect) {
            return;
        }
        int exponent = Math.min(reconnectAttempt, 4);
        int baseSeconds = Math.min(30, 1 << exponent);
        int jitter = ThreadLocalRandom.current().nextInt(0, 1000);
        reconnectAttempt++;
        reconnectExecutor.schedule(this::connectInternal,
                baseSeconds * 1000L + jitter, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        disconnect(false);
        reconnectExecutor.shutdownNow();
    }

    private static SocketFactory insecureSocketFactory() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new SecureRandom());
        return context.getSocketFactory();
    }
}
