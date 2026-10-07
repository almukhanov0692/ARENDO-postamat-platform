package kz.arendo.device;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kz.arendo.device.modbus.ModbusRtuClient;
import kz.arendo.device.modbus.ModbusCellMap;
import kz.arendo.device.modbus.DoorFeedbackTracker;
import kz.arendo.device.modbus.ModbusIoGateway;
import kz.arendo.device.modbus.UsbSerialTransport;
import kz.arendo.device.backend.CommandIdLedger;
import kz.arendo.device.backend.DeviceBackendClient;

/** Foundation UI for the Android gateway installed in the postamat. */
public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "kz.arendo.device.USB_PERMISSION";
    private static final ModbusCellMap CELL_MAP = ModbusCellMap.exhibition10();
    private static final int CELL_COUNT = CELL_MAP.getCells().size();
    private static final int BAUD_RATE = 9600;
    private static final long OUTPUT_PULSE_MILLIS = 2_000L;

    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService backendTimer = Executors.newSingleThreadScheduledExecutor();
    private final DoorFeedbackTracker doorFeedback = new DoorFeedbackTracker(CELL_MAP);
    private final boolean[] outputActive = new boolean[CELL_COUNT];
    private final ScheduledFuture<?>[] outputPulseOffTasks = new ScheduledFuture<?>[CELL_COUNT];
    private final TextView[] cellStates = new TextView[CELL_COUNT];
    private final Button[] cellButtons = new Button[CELL_COUNT];

    private UsbManager usbManager;
    private UsbSerialDriver pendingDriver;
    private UsbSerialPort serialPort;
    private ModbusRtuClient modbus;
    private ModbusIoGateway ioGateway;
    private ScheduledFuture<?> polling;
    private ScheduledFuture<?> heartbeat;
    private volatile boolean modbusOnline;
    private volatile boolean shuttingDown;
    private TextView usbStatus;
    private TextView backendStatus;
    private TextView logView;
    private EditText backendUrlInput;
    private EditText postamatIdInput;
    private EditText deviceTokenInput;
    private DeviceBackendClient backendClient;
    private CommandIdLedger commandLedger;
    private String activePostamatId = "map-7";
    private String activeDeviceToken = "";
    private String activeBackendUrl = "";
    private int heartbeatSeconds = 15;
    private volatile boolean backendRequested;
    private volatile boolean backendConnected;
    private int backendGeneration;
    private int reconnectAttempt;

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted && pendingDriver != null && device != null
                    && device.equals(pendingDriver.getDevice())) {
                openDriver(pendingDriver);
            } else {
                setUsbStatus("Нет разрешения на USB", false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(15, 23, 42));
        getWindow().setNavigationBarColor(Color.rgb(244, 247, 251));
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        try {
            commandLedger = new CommandIdLedger(getFilesDir());
        } catch (IOException error) {
            commandLedger = null;
        }
        registerUsbReceiver();
        setContentView(buildUi());
        if (commandLedger == null) {
            appendLog("Backend: не удалось подготовить журнал защиты от повторных команд");
        }
    }

    private void registerUsbReceiver() {
        IntentFilter filter = new IntentFilter(USB_PERMISSION);
        ContextCompat.registerReceiver(this, usbPermissionReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 247, 251));

        LinearLayout root = column(20);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        scroll.addView(root);

        LinearLayout header = column(8);
        header.setPadding(dp(20), dp(20), dp(20), dp(20));
        header.setBackgroundColor(Color.rgb(15, 23, 42));
        TextView brand = label("ARENDO", 14, Color.rgb(147, 197, 253));
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(brand);
        TextView title = label("Контроллер постамата", 25, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);
        header.addView(label("RK3568 · USB-RS485 · Modbus RTU", 13,
                Color.rgb(203, 213, 225)));
        root.addView(header, matchWrap(0));

        root.addView(sectionTitle("СОСТОЯНИЕ"), matchWrap(22));
        LinearLayout statusCard = column(10);
        statusCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        statusCard.setBackgroundColor(Color.WHITE);
        usbStatus = label("Modbus: не подключён", 16, Color.rgb(185, 28, 28));
        backendStatus = label("Backend WebSocket: не подключён", 14,
                Color.rgb(100, 116, 139));
        statusCard.addView(usbStatus);
        statusCard.addView(backendStatus);
        Button connect = button("Подключить USB-RS485", true);
        connect.setOnClickListener(view -> findAndConnect());
        statusCard.addView(connect, matchWrap(8));
        root.addView(statusCard, matchWrap(0));

        root.addView(sectionTitle("BACKEND WEBSOCKET"), matchWrap(22));
        SharedPreferences backendPreferences = getSharedPreferences(
                "backend_config", MODE_PRIVATE);
        LinearLayout backendCard = column(8);
        backendCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        backendCard.setBackgroundColor(Color.WHITE);
        backendCard.addView(label("Локально: ws://IP-ноутбука:8765/v1/device/socket · сервер: wss://",
                12, Color.rgb(100, 116, 139)));
        backendUrlInput = editField("ws://192.168.0.1:8765/v1/device/socket",
                backendPreferences.getString("url", ""), InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI);
        backendCard.addView(backendUrlInput, matchWrap(4));
        postamatIdInput = editField("ID постамата", backendPreferences.getString("postamatId",
                "map-7"), InputType.TYPE_CLASS_TEXT);
        backendCard.addView(postamatIdInput, matchWrap(4));
        deviceTokenInput = editField("Токен устройства", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        backendCard.addView(deviceTokenInput, matchWrap(4));
        if (BuildConfig.DEBUG && !BuildConfig.LOCAL_DEMO_WS_URL.isEmpty()
                && !BuildConfig.LOCAL_DEMO_WS_TOKEN.isEmpty()) {
            Button localDemoConnect = button("Подключить к локальному стенду", false);
            localDemoConnect.setOnClickListener(view -> {
                backendUrlInput.setText(BuildConfig.LOCAL_DEMO_WS_URL);
                postamatIdInput.setText("map-7");
                deviceTokenInput.setText(BuildConfig.LOCAL_DEMO_WS_TOKEN);
                connectBackendFromForm();
            });
            backendCard.addView(localDemoConnect, matchWrap(6));
        }
        Button backendConnect = button("Подключить Backend WebSocket", true);
        backendConnect.setOnClickListener(view -> connectBackendFromForm());
        backendCard.addView(backendConnect, matchWrap(6));
        root.addView(backendCard, matchWrap(0));

        root.addView(sectionTitle("ЯЧЕЙКИ · ДИАГНОСТИКА СТЕНДА"), matchWrap(22));
        root.addView(label("Демо-плата: 10 выходов; обратная связь заведена только для D01-D04.",
                13, Color.rgb(100, 116, 139)), matchWrap(0));

        for (int index = 0; index < CELL_COUNT; index++) {
            final int cell = index;
            ModbusCellMap.Cell mapping = CELL_MAP.getCell(index);
            LinearLayout card = column(8);
            card.setPadding(dp(18), dp(14), dp(18), dp(14));
            card.setBackgroundColor(Color.WHITE);
            TextView cellTitle = label("Ячейка " + mapping.getCode(),
                    17, Color.rgb(15, 23, 42));
            cellTitle.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(cellTitle);
            cellStates[index] = label("Состояние: неизвестно", 14, Color.rgb(100, 116, 139));
            card.addView(cellStates[index]);
            cellButtons[index] = button("Тест: импульс Y" + mapping.getOutputChannel() + " · 2 с", false);
            cellButtons[index].setEnabled(false);
            cellButtons[index].setOnClickListener(view -> toggleOutput(cell));
            card.addView(cellButtons[index], matchWrap(4));
            root.addView(card, matchWrap(10));
            updateCell(index, "unknown");
        }

        root.addView(sectionTitle("ЖУРНАЛ СТЕНДА"), matchWrap(22));
        logView = label("Готово к подключению.", 12, Color.rgb(226, 232, 240));
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(dp(14), dp(14), dp(14), dp(14));
        logView.setBackgroundColor(Color.rgb(15, 23, 42));
        root.addView(logView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180)));
        return scroll;
    }

    private EditText editField(String hint, String value, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(14);
        field.setInputType(inputType);
        field.setHint(hint);
        if (value != null && !value.isEmpty()) {
            field.setText(value);
            field.setSelection(value.length());
        }
        field.setPadding(dp(14), dp(10), dp(14), dp(10));
        field.setBackgroundColor(Color.rgb(244, 247, 251));
        return field;
    }

    private void connectBackendFromForm() {
        String endpoint = backendUrlInput.getText().toString().trim();
        String postamatId = postamatIdInput.getText().toString().trim();
        String token = deviceTokenInput.getText().toString().trim();
        if (endpoint.isEmpty() || postamatId.isEmpty()) {
            setBackendStatus("Укажи WebSocket URL и ID постамата", false);
            return;
        }
        try {
            URI uri = new URI(endpoint);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("ws".equalsIgnoreCase(scheme)
                    || "wss".equalsIgnoreCase(scheme))) {
                throw new URISyntaxException(endpoint, "Нужен ws:// или wss:// адрес");
            }
            if ("ws".equalsIgnoreCase(scheme)
                    && (!BuildConfig.DEBUG || !isPrivateNetworkHost(uri.getHost()))) {
                setBackendStatus("Незащищённый ws:// разрешён только для локальной сети в debug-сборке",
                        false);
                return;
            }
            if (token.isEmpty()) {
                setBackendStatus("Введи токен устройства или используй локальный стенд", false);
                return;
            }
            backendRequested = false;
            DeviceBackendClient oldClient = backendClient;
            backendClient = null;
            backendGeneration++;
            if (oldClient != null) {
                oldClient.close();
            }
            activeBackendUrl = endpoint;
            activePostamatId = postamatId;
            activeDeviceToken = token;
            reconnectAttempt = 0;
            getSharedPreferences("backend_config", MODE_PRIVATE).edit()
                    .putString("url", endpoint)
                    .putString("postamatId", postamatId)
                    .apply();
            backendRequested = true;
            openBackendConnection(backendGeneration);
        } catch (URISyntaxException error) {
            setBackendStatus("Проверь WebSocket URL", false);
        }
    }

    private boolean isPrivateNetworkHost(String host) {
        String value = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(value) || "::1".equals(value) || value.startsWith("127.")) {
            return true;
        }
        if (value.startsWith("10.") || value.startsWith("192.168.")
                || value.startsWith("169.254.")) {
            return true;
        }
        if (value.startsWith("172.")) {
            String[] parts = value.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private void openBackendConnection(int generation) {
        if (!backendRequested || generation != backendGeneration) {
            return;
        }
        setBackendStatus("Подключение…", false);
        try {
            URI uri = new URI(activeBackendUrl);
            final DeviceBackendClient[] holder = new DeviceBackendClient[1];
            DeviceBackendClient client = new DeviceBackendClient(uri, activeDeviceToken,
                    new DeviceBackendClient.Listener() {
                        @Override
                        public void onOpen() {
                            if (generation != backendGeneration) {
                                holder[0].close();
                                return;
                            }
                            backendConnected = true;
                            reconnectAttempt = 0;
                            setBackendStatus("Подключён · " + activePostamatId, true);
                            runOnUiThread(() -> appendLog("Backend WebSocket: соединение установлено"));
                            sendHello();
                        }

                        @Override
                        public void onMessage(JSONObject message) {
                            if (generation == backendGeneration) {
                                handleBackendMessage(message);
                            }
                        }

                        @Override
                        public void onClose(String reason) {
                            if (generation == backendGeneration) {
                                backendConnected = false;
                                cancelHeartbeat();
                                setBackendStatus("Связь потеряна · переподключение…", false);
                                scheduleBackendReconnect(generation);
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            if (generation == backendGeneration) {
                                setBackendStatus("Ошибка WebSocket · проверь URL/сеть/сертификат",
                                        false);
                            }
                        }
                    });
            holder[0] = client;
            backendClient = client;
            client.connect();
        } catch (URISyntaxException error) {
            backendRequested = false;
            setBackendStatus("Некорректный WebSocket URL", false);
        }
    }

    private void scheduleBackendReconnect(int generation) {
        if (!backendRequested || generation != backendGeneration) {
            return;
        }
        reconnectAttempt = Math.min(reconnectAttempt + 1, 5);
        long delaySeconds = Math.min(30, 1L << reconnectAttempt);
        backendTimer.schedule(() -> openBackendConnection(generation),
                delaySeconds, TimeUnit.SECONDS);
    }

    private void sendHello() {
        try {
            JSONObject hello = new JSONObject();
            JSONArray capabilities = new JSONArray();
            capabilities.put("locks");
            capabilities.put("buttons");
            capabilities.put("indicators");
            capabilities.put("demo_buttons_as_doors");
            capabilities.put("relay_pulse_2s");
            hello.put("type", "hello");
            hello.put("protocolVersion", 1);
            hello.put("postamatId", activePostamatId);
            hello.put("appVersion", BuildConfig.VERSION_NAME);
            hello.put("capabilities", capabilities);
            hello.put("cells", currentCellsSnapshot());
            hello.put("net", new JSONObject().put("kind", "unknown"));
            sendBackendMessage(hello);
        } catch (JSONException error) {
            appendLogOnUi("Backend: не удалось сформировать hello");
        }
    }

    private JSONArray currentCellsSnapshot() throws JSONException {
        JSONArray cells = new JSONArray();
        for (int index = 0; index < CELL_COUNT; index++) {
            JSONObject cell = new JSONObject();
            cell.put("code", CELL_MAP.getCell(index).getCode());
            cell.put("door", doorFeedback.getState(index));
            cell.put("lock", "unknown");
            cells.put(cell);
        }
        return cells;
    }

    private void sendHeartbeat() {
        if (!backendConnected) {
            return;
        }
        try {
            JSONObject message = new JSONObject();
            message.put("type", "heartbeat");
            message.put("postamatId", activePostamatId);
            message.put("sentAt", utcNow());
            message.put("cells", currentCellsSnapshot());
            message.put("net", new JSONObject().put("kind", "unknown"));
            JSONArray problems = new JSONArray();
            if (!modbusOnline) {
                problems.put("modbus_offline");
            }
            message.put("problems", problems);
            sendBackendMessage(message);
        } catch (JSONException error) {
            appendLogOnUi("Backend: не удалось сформировать heartbeat");
        }
    }

    private void handleBackendMessage(JSONObject message) {
        String type = message.optString("type", "");
        if ("welcome".equals(type)) {
            String serverPostamatId = message.optString("postamatId", activePostamatId);
            if (!activePostamatId.equals(serverPostamatId)) {
                setBackendStatus("Сервер вернул другой ID постамата", false);
                DeviceBackendClient client = backendClient;
                if (client != null) {
                    client.close();
                }
                return;
            }
            JSONObject config = message.optJSONObject("config");
            if (config != null) {
                heartbeatSeconds = Math.max(5,
                        Math.min(300, config.optInt("heartbeatSec", 15)));
            }
            cancelHeartbeat();
            sendHeartbeat();
            heartbeat = backendTimer.scheduleWithFixedDelay(this::sendHeartbeat,
                    heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
            appendLogOnUi("Backend: welcome получен · heartbeat " + heartbeatSeconds + " сек");
            return;
        }
        if ("command".equals(type)) {
            io.execute(() -> processBackendCommand(message));
            return;
        }
        appendLogOnUi("Backend: получено сообщение " + type);
    }

    private void processBackendCommand(JSONObject command) {
        String commandId = command.optString("id", "").trim();
        String kind = command.optString("kind", "");
        String targetPostamat = command.optString("postamatId", "");
        JSONObject payload = command.optJSONObject("payload");
        String code = payload == null ? "" : normalizeCellCode(payload.optString("cellCode", ""));

        if (commandId.isEmpty()) {
            sendCommandAck("", false, "invalid_command_id", code);
            return;
        }
        if (!activePostamatId.equals(targetPostamat)) {
            sendCommandAck(commandId, false, "wrong_postamat", code);
            return;
        }
        if (isExpired(command.optString("expiresAt", ""))) {
            sendCommandAck(commandId, false, "expired", code);
            return;
        }
        if ("cell_report".equals(kind)) {
            sendHeartbeat();
            sendCommandAck(commandId, true, "cell_report_sent", "");
            return;
        }
        if (!"open_cell".equals(kind)) {
            sendCommandAck(commandId, false, "unsupported_command", code);
            return;
        }
        int index = findCellIndex(code);
        if (index < 0) {
            sendCommandAck(commandId, false, "invalid_cell", code);
            return;
        }
        if (commandLedger == null) {
            sendCommandAck(commandId, false, "internal_error", code);
            return;
        }
        try {
            if (!commandLedger.markIfNew(commandId)) {
                sendCommandAck(commandId, true, "duplicate_ignored", code);
                return;
            }
        } catch (IOException | IllegalArgumentException error) {
            sendCommandAck(commandId, false, "internal_error", code);
            return;
        }

        ModbusIoGateway gateway = ioGateway;
        if (gateway == null) {
            sendCommandAck(commandId, false, "modbus_offline", code);
            return;
        }
        try {
            cancelOutputPulse(index);
            outputActive[index] = true;
            scheduleOutputPulseOff(index, gateway);
            gateway.writeCellOutput(index, true);
            cancelOutputPulse(index);
            scheduleOutputPulseOff(index, gateway);
            final int cellIndex = index;
            runOnUiThread(() -> {
                updateCell(cellIndex, doorFeedback.getState(cellIndex));
                appendLog("Команда open_cell D" + code + " · импульс Y"
                        + CELL_MAP.getCell(cellIndex).getOutputChannel() + " на 2 секунды; ждём X");
            });
            sendCommandAck(commandId, true, "open_command_accepted", code);
        } catch (Exception error) {
            sendCommandAck(commandId, false, "internal_error", code);
            appendLogOnUi("Ошибка команды D" + code + ": Modbus не выполнил запись");
        }
    }

    private void cancelOutputPulse(int index) {
        ScheduledFuture<?> task = outputPulseOffTasks[index];
        if (task != null) {
            task.cancel(false);
            outputPulseOffTasks[index] = null;
        }
    }

    private void scheduleOutputPulseOff(int index, ModbusIoGateway gateway) {
        outputPulseOffTasks[index] = io.schedule(() -> {
            try {
                if (!outputActive[index]) {
                    return;
                }
                ModbusIoGateway activeGateway = ioGateway == null ? gateway : ioGateway;
                activeGateway.writeCellOutput(index, false);
                outputActive[index] = false;
                outputPulseOffTasks[index] = null;
                runOnUiThread(() -> {
                    updateCell(index, doorFeedback.getState(index));
                    appendLog("Импульс завершён: Y" + CELL_MAP.getCell(index).getOutputChannel()
                            + " OFF · D" + CELL_MAP.getCell(index).getCode());
                });
                sendHeartbeat();
            } catch (Exception error) {
                modbusOnline = false;
                outputPulseOffTasks[index] = null;
                runOnUiThread(() -> appendLog("Ошибка отключения реле Y"
                        + CELL_MAP.getCell(index).getOutputChannel() + ": " + safeMessage(error)));
                sendHeartbeat();
            }
        }, OUTPUT_PULSE_MILLIS, TimeUnit.MILLISECONDS);
    }

    private int findCellIndex(String code) {
        if (code == null || code.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < CELL_COUNT; index++) {
            if (CELL_MAP.getCell(index).getCode().equals(code)) {
                return index;
            }
        }
        return -1;
    }

    private String normalizeCellCode(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }
        try {
            int number = Integer.parseInt(digits);
            return number >= 1 && number <= CELL_COUNT
                    ? String.format(Locale.ROOT, "%02d", number) : "";
        } catch (NumberFormatException error) {
            return "";
        }
    }

    private boolean isExpired(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        try {
            Date expiresAt = format.parse(value);
            return expiresAt == null || expiresAt.getTime() <= System.currentTimeMillis();
        } catch (java.text.ParseException error) {
            return true;
        }
    }

    private void sendCommandAck(String commandId, boolean ok, String resultKind, String cellCode) {
        try {
            JSONObject ack = new JSONObject();
            ack.put("type", "ack");
            ack.put("commandId", commandId);
            ack.put("ok", ok);
            if (ok) {
                JSONObject result = new JSONObject().put("kind", resultKind);
                if (cellCode != null && !cellCode.isEmpty()) {
                    result.put("cellCode", cellCode);
                }
                ack.put("result", result);
            } else {
                ack.put("error", new JSONObject().put("code", resultKind));
            }
            sendBackendMessage(ack);
        } catch (JSONException error) {
            appendLogOnUi("Backend: не удалось сформировать ack");
        }
    }

    private void sendDoorEvent(int index, String kind, String source) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "event");
            event.put("eventId", UUID.randomUUID().toString());
            event.put("kind", kind);
            event.put("postamatId", activePostamatId);
            event.put("cellCode", CELL_MAP.getCell(index).getCode());
            event.put("occurredAt", utcNow());
            event.put("detail", new JSONObject().put("source", source).put("simulated", true));
            sendBackendMessage(event);
        } catch (JSONException error) {
            appendLogOnUi("Backend: не удалось сформировать событие двери");
        }
    }

    private void sendBackendMessage(JSONObject message) {
        DeviceBackendClient client = backendClient;
        if (client == null || !client.isOpen()) {
            return;
        }
        client.send(message.toString());
    }

    private String utcNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private void cancelHeartbeat() {
        if (heartbeat != null) {
            heartbeat.cancel(false);
            heartbeat = null;
        }
    }

    private void setBackendStatus(String value, boolean online) {
        runOnUiThread(() -> {
            if (backendStatus != null) {
                backendStatus.setText("Backend WebSocket: " + value);
                backendStatus.setTextColor(online
                        ? Color.rgb(22, 163, 74) : Color.rgb(100, 116, 139));
            }
        });
    }

    private void appendLogOnUi(String message) {
        runOnUiThread(() -> appendLog(message));
    }

    private void findAndConnect() {
        if (modbus != null) {
            appendLog("Modbus уже подключён");
            return;
        }
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber()
                .findAllDrivers(usbManager);
        if (drivers.isEmpty()) {
            setUsbStatus("USB-RS485 не найден", false);
            appendLog("Поддерживаемый USB-Serial адаптер не найден");
            return;
        }
        pendingDriver = selectDriver(drivers);
        UsbDevice device = pendingDriver.getDevice();
        appendLog(String.format(Locale.ROOT, "Найден USB VID=%04X PID=%04X",
                device.getVendorId(), device.getProductId()));
        if (usbManager.hasPermission(device)) {
            openDriver(pendingDriver);
            return;
        }
        Intent intent = new Intent(USB_PERMISSION).setPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        PendingIntent permission = PendingIntent.getBroadcast(this, 0, intent, flags);
        usbManager.requestPermission(device, permission);
        setUsbStatus("Ожидание разрешения USB", false);
    }

    private UsbSerialDriver selectDriver(List<UsbSerialDriver> drivers) {
        for (UsbSerialDriver driver : drivers) {
            if (driver.getDevice().getVendorId() == 0x067B) {
                return driver;
            }
        }
        return drivers.get(0);
    }

    private void openDriver(UsbSerialDriver driver) {
        io.execute(() -> {
            try {
                UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
                if (connection == null) {
                    throw new IOException("Android не открыл USB устройство");
                }
                UsbSerialPort openedPort = driver.getPorts().get(0);
                openedPort.open(connection);
                openedPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE);
                openedPort.setDTR(true);
                openedPort.setRTS(true);
                serialPort = openedPort;
                modbus = new ModbusRtuClient(new UsbSerialTransport(openedPort));
                ioGateway = new ModbusIoGateway(modbus, CELL_MAP);
                invalidateDoorFeedback();
                modbusOnline = true;
                runOnUiThread(() -> {
                    setUsbStatus("Modbus RTU подключён · 9600 8N1", true);
                    setButtonsEnabled(true);
                    appendLog("RS-485 открыт, запущен опрос " + CELL_COUNT + " ячеек");
                });
                startPolling();
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setUsbStatus("Ошибка подключения: " + safeMessage(error), false);
                    appendLog("Ошибка USB: " + safeMessage(error));
                });
                closePort();
            }
        });
    }

    private void startPolling() {
        if (polling != null && !polling.isDone()) {
            return;
        }
        polling = io.scheduleWithFixedDelay(() -> {
            ModbusIoGateway gateway = ioGateway;
            if (gateway == null) {
                return;
            }
            try {
                boolean[] inputs = gateway.readCellInputs();
                if (gateway != ioGateway) {
                    return;
                }
                boolean wasOffline = !modbusOnline;
                DoorFeedbackTracker.Snapshot snapshot = doorFeedback.update(inputs);
                modbusOnline = true;
                if (wasOffline) {
                    runOnUiThread(() -> {
                        setUsbStatus("RTU подключён · 9600 8N1", true);
                        appendLog("Modbus: связь восстановлена; состояния перечитаны");
                    });
                }
                for (int index = 0; index < CELL_COUNT; index++) {
                    if (!CELL_MAP.getCell(index).hasDoorInput()) {
                        continue;
                    }
                    if (snapshot.hasChanged(index)) {
                        final int cell = index;
                        String nextState = snapshot.getState(index);
                        if (snapshot.hasTransition(index)) {
                            String eventKind = "closed".equals(nextState)
                                    ? "door_closed" : "door_opened";
                            runOnUiThread(() -> {
                                updateCell(cell, nextState);
                                appendLog("D" + CELL_MAP.getCell(cell).getCode()
                                        + " " + nextState + " · подтверждено входом X"
                                        + CELL_MAP.getCell(cell).getInputChannel());
                            });
                            sendDoorEvent(index, eventKind, "demo_modbus_x");
                        } else {
                            runOnUiThread(() -> updateCell(cell, nextState));
                        }
                    }
                }
            } catch (Exception error) {
                if (gateway != ioGateway) {
                    return;
                }
                boolean wasOnline = modbusOnline;
                modbusOnline = false;
                invalidateDoorFeedback();
                if (wasOnline) {
                    runOnUiThread(() -> {
                        setUsbStatus("Ошибка: " + safeMessage(error), false);
                        appendLog("Modbus: " + safeMessage(error)
                                + " · состояние датчиков теперь unknown");
                    });
                }
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
    }

    private void toggleOutput(int index) {
        io.execute(() -> {
            try {
                ModbusIoGateway gateway = ioGateway;
                if (gateway == null) {
                    throw new IOException("Modbus не подключён");
                }
                cancelOutputPulse(index);
                outputActive[index] = true;
                scheduleOutputPulseOff(index, gateway);
                gateway.writeCellOutput(index, true);
                cancelOutputPulse(index);
                scheduleOutputPulseOff(index, gateway);
                ModbusCellMap.Cell mapping = CELL_MAP.getCell(index);
                runOnUiThread(() -> {
                    updateCell(index, doorFeedback.getState(index));
                    appendLog("Тестовый импульс Y" + mapping.getOutputChannel()
                            + " · 2 секунды; дверца — только по X-входу");
                });
            } catch (Exception error) {
                int channel = CELL_MAP.getCell(index).getOutputChannel();
                runOnUiThread(() -> appendLog("Y" + channel + ": "
                        + safeMessage(error)));
            }
        });
    }

    private void updateCell(int index, String state) {
        ModbusCellMap.Cell mapping = CELL_MAP.getCell(index);
        boolean open = "open".equals(state);
        String detail = mapping.hasDoorInput()
                ? "X" + mapping.getInputChannel() + " · Y" + mapping.getOutputChannel()
                : "вход не подключён · Y" + mapping.getOutputChannel();
        String relay = outputActive[index] ? "ON" : "OFF";
        cellStates[index].setText("Дверь: " + state + " · " + detail + " · реле " + relay);
        cellStates[index].setTextColor(open ? Color.rgb(22, 163, 74) : Color.rgb(71, 85, 105));
        cellButtons[index].setText("Тест: импульс Y" + mapping.getOutputChannel() + " · 2 с");
    }

    private void setUsbStatus(String value, boolean online) {
        runOnUiThread(() -> {
            usbStatus.setText("Modbus: " + value);
            usbStatus.setTextColor(online ? Color.rgb(22, 163, 74) : Color.rgb(185, 28, 28));
        });
    }

    private void invalidateDoorFeedback() {
        DoorFeedbackTracker.Snapshot snapshot = doorFeedback.markUnavailable();
        if (shuttingDown) {
            return;
        }
        for (int index = 0; index < CELL_COUNT; index++) {
            if (snapshot.hasChanged(index)) {
                final int cell = index;
                runOnUiThread(() -> updateCell(cell, snapshot.getState(cell)));
            }
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Button button : cellButtons) {
            button.setEnabled(enabled);
        }
    }

    private void appendLog(String message) {
        if (logView == null) {
            return;
        }
        String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
        String current = logView.getText().toString();
        String next = current + "\n[" + time + "] " + message;
        String[] lines = next.split("\n");
        if (lines.length > 40) {
            StringBuilder trimmed = new StringBuilder();
            for (int index = lines.length - 40; index < lines.length; index++) {
                if (trimmed.length() > 0) {
                    trimmed.append('\n');
                }
                trimmed.append(lines[index]);
            }
            next = trimmed.toString();
        }
        logView.setText(next);
    }

    private void closePort() {
        if (polling != null) {
            polling.cancel(false);
            polling = null;
        }
        modbus = null;
        modbusOnline = false;
        invalidateDoorFeedback();
        ioGateway = null;
        UsbSerialPort port = serialPort;
        serialPort = null;
        if (port != null) {
            try {
                port.close();
            } catch (IOException ignored) {
                // Closing an already detached USB device is best effort.
            }
        }
        runOnUiThread(() -> setButtonsEnabled(false));
    }

    @Override
    protected void onDestroy() {
        shuttingDown = true;
        backendRequested = false;
        backendConnected = false;
        backendGeneration++;
        cancelHeartbeat();
        DeviceBackendClient client = backendClient;
        backendClient = null;
        if (client != null) {
            client.close();
        }
        activeDeviceToken = "";
        closePort();
        unregisterReceiver(usbPermissionReceiver);
        io.shutdownNow();
        backendTimer.shutdownNow();
        super.onDestroy();
    }

    private LinearLayout column(int spacingDp) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        if (Build.VERSION.SDK_INT >= 29) {
            layout.setGravity(Gravity.NO_GRAVITY);
        }
        return layout;
    }

    private TextView sectionTitle(String value) {
        TextView view = label(value, 12, Color.rgb(100, 116, 139));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private TextView label(String value, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setTextColor(primary ? Color.WHITE : Color.rgb(30, 64, 175));
        button.setBackgroundColor(primary ? Color.rgb(37, 99, 235) : Color.rgb(239, 246, 255));
        button.setAllCaps(false);
        button.setMinHeight(dp(50));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap(int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
