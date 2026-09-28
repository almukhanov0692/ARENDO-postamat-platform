package com.example.postamatmodbus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import android.os.ParcelUuid;

import org.json.JSONArray;
import org.json.JSONObject;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String ACTION_USB_PERMISSION =
            "com.example.postamatmodbus.USB_PERMISSION";
    private static final int REQUEST_BLE_PERMISSION = 701;
    private static final int REQUEST_BLE_ENABLE = 702;
    private static final int REQUEST_BLE_SERVER_PERMISSION = 703;
    private static final UUID BLE_SERVICE_UUID = UUID.fromString(
            "8f6d1000-6b35-4e76-9b0d-2d7f6a9a1000");
    private static final UUID BLE_STATUS_UUID = UUID.fromString(
            "8f6d1001-6b35-4e76-9b0d-2d7f6a9a1000");
    private static final UUID BLE_AUTH_UUID = UUID.fromString(
            "8f6d1002-6b35-4e76-9b0d-2d7f6a9a1000");
    private static final UUID BLE_COMMAND_UUID = UUID.fromString(
            "8f6d1003-6b35-4e76-9b0d-2d7f6a9a1000");
    private static final int COLOR_BACKGROUND = Color.rgb(7, 12, 18);
    private static final int COLOR_NAVY = Color.rgb(10, 16, 24);
    private static final int COLOR_BLUE = Color.rgb(57, 136, 246);
    private static final int COLOR_TEXT = Color.rgb(232, 237, 244);
    private static final int COLOR_MUTED = Color.rgb(120, 134, 151);
    private static final int COLOR_CARD = Color.rgb(16, 23, 33);
    private static final int COLOR_BORDER = Color.rgb(38, 49, 64);
    private static final int COLOR_GREEN = Color.rgb(52, 211, 153);
    private static final int COLOR_AMBER = Color.rgb(245, 183, 60);
    private static final int COLOR_RED = Color.rgb(248, 93, 110);
    private static final StringBuilder LOG_BUFFER = new StringBuilder(
            "Готово. Адреса Modbus указаны с нуля.");

    private final ScheduledExecutorService ioExecutor = Executors.newScheduledThreadPool(2);
    private final Object modbusLock = new Object();
    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private ModbusRtuClient modbus;
    private UsbSerialDriver pendingDriver;

    private EditText slaveIdEdit;
    private EditText inputStartEdit;
    private EditText outputStartEdit;
    private EditText websocketUrlEdit;
    private EditText postamatIdEdit;
    private EditText tokenEdit;
    private CheckBox insecureTlsCheck;
    private Spinner baudSpinner;
    private TextView connectionText;
    private TextView backendText;
    private TextView overallStatusText;
    private TextView bluetoothNodeText;
    private LinearLayout settingsPanel;
    private View mainScreen;
    private View settingsScreen;
    private Button backendActionButton;
    private Button modbusActionButton;
    private Button bleScanActionButton;
    private final TextView[] summaryCountTexts = new TextView[4];
    private TextView logText;
    private final TextView[] inputTexts = new TextView[4];
    private final TextView[] relayTexts = new TextView[4];
    private final boolean[] relayStates = new boolean[4];
    private final String[] cellBusinessStates = {
            "Доступна", "Доступна", "Доступна", "Доступна"
    };
    private final String[] cellToolNames = {"—", "—", "—", "—"};
    private final String[] cellToolUnits = {"—", "—", "—", "—"};
    private final String[] cellRentalIds = {"—", "—", "—", "—"};
    private final String[] lastOpenedAt = {"—", "—", "—", "—"};
    private final String[] lastClosedAt = {"—", "—", "—", "—"};
    private final long[] openedAtMillis = new long[4];
    private boolean[] previousInputs = new boolean[4];
    private boolean inputsInitialized;
    private BackendWebSocketClient backendClient;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> pollingTask;
    private int modbusErrorStreak;
    private String lastModbusError;
    private long lastModbusErrorAt;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private EditText bleNameEdit;
    private TextView bleStatusText;
    private TextView bleSessionText;
    private LinearLayout bleDevicesContainer;
    private TextView bleServerStatusText;
    private TextView bleServerSessionText;
    private EditText bleClientCodeEdit;
    private LinearLayout bleClientAuthContainer;
    private LinearLayout bleClientCommandContainer;
    private final Map<String, BluetoothDevice> bleDevices = new LinkedHashMap<>();
    private boolean bleScanning;
    private String bleSessionId;
    private String bleSessionCode;
    private BluetoothLeAdvertiser bleAdvertiser;
    private BluetoothGattServer bleGattServer;
    private BluetoothGattCharacteristic bleServerStatusCharacteristic;
    private BluetoothGattCharacteristic bleServerAuthCharacteristic;
    private BluetoothGattCharacteristic bleServerCommandCharacteristic;
    private BluetoothDevice bleServerClient;
    private boolean bleServerMode;
    private boolean bleServerAuthenticated;
    private String bleServerSessionId;
    private String bleServerCode;
    private String bleServerStatusValue = "OFF";
    private long bleServerSessionExpiresAt;
    private boolean pendingBleServerStart;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final AdvertiseCallback bleAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            bleServerMode = true;
            runOnUiThread(() -> {
                if (bleServerStatusText != null) {
                    bleServerStatusText.setText("BLE-сервис: ожидает подключение");
                }
            });
            appendLog("BLE: сервис обслуживания запущен");
        }

        @Override
        public void onStartFailure(int errorCode) {
            bleServerMode = false;
            runOnUiThread(() -> {
                if (bleServerStatusText != null) {
                    bleServerStatusText.setText("BLE-сервис: ошибка запуска · " + errorCode);
                }
            });
            appendLog("BLE: ошибка запуска сервиса · код " + errorCode);
        }
    };

    private final BluetoothGattServerCallback bleGattServerCallback =
            new BluetoothGattServerCallback() {
                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {
                    if (status == BluetoothGatt.GATT_SUCCESS
                            && BLE_SERVICE_UUID.equals(service.getUuid())) {
                        startBleAdvertising();
                    } else {
                        appendLog("BLE: не удалось добавить сервис · код " + status);
                    }
                }

                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status,
                                                     int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        bleServerClient = device;
                        bleServerAuthenticated = false;
                        generateBleServerSession();
                        runOnUiThread(() -> {
                            if (bleServerStatusText != null) {
                                bleServerStatusText.setText(
                                        "BLE-сервис: клиент подключён · введите код");
                            }
                        });
                        appendLog("BLE: сервисный клиент подключён");
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED
                            && (bleServerClient == null || device.equals(bleServerClient))) {
                        bleServerClient = null;
                        bleServerAuthenticated = false;
                        runOnUiThread(() -> {
                            if (bleServerStatusText != null) {
                                bleServerStatusText.setText(
                                        "BLE-сервис: ожидает подключение");
                            }
                            if (bleServerSessionText != null) {
                                bleServerSessionText.setText(
                                        "Сессия сервиса: не создана");
                            }
                        });
                        appendLog("BLE: сервисный клиент отключён");
                    }
                }

                @Override
                public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                         int offset,
                                                         BluetoothGattCharacteristic characteristic) {
                    if (!BLE_STATUS_UUID.equals(characteristic.getUuid()) || offset != 0) {
                        sendBleServerResponse(device, requestId,
                                BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null);
                        return;
                    }
                    sendBleServerResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                            currentBleServerStatus().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                @Override
                public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                          BluetoothGattCharacteristic characteristic,
                                                          boolean preparedWrite,
                                                          boolean responseNeeded, int offset,
                                                          byte[] value) {
                    if (responseNeeded) {
                        sendBleServerResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                                offset, null);
                    }
                    if (preparedWrite || offset != 0 || value == null) {
                        return;
                    }
                    String payload = new String(value, java.nio.charset.StandardCharsets.UTF_8);
                    if (BLE_AUTH_UUID.equals(characteristic.getUuid())) {
                        handleBleServerAuth(device, payload);
                    } else if (BLE_COMMAND_UUID.equals(characteristic.getUuid())) {
                        handleBleServerCommand(device, payload);
                    }
                }
            };

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null || device.getAddress() == null) {
                return;
            }
            bleDevices.put(device.getAddress(), device);
            runOnUiThread(MainActivity.this::renderBleDevices);
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                bleScanning = false;
                setBleStatus("ошибка поиска: " + errorCode);
                appendLog("BLE: ошибка сканирования · код " + errorCode);
            });
        }
    };

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                generateBleSession();
                runOnUiThread(() -> setBleStatus("подключено · поиск сервисов"));
                appendLog("BLE: устройство подключено");
                if (hasBleConnectPermission()) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    setBleStatus("отключено");
                    bleSessionText.setText("Сессия: не создана");
                    showBleClientAuth(false);
                    showBleClientCommands(false);
                });
                appendLog("BLE: устройство отключено");
                if (bluetoothGatt == gatt) {
                    bluetoothGatt = null;
                }
                gatt.close();
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> setBleStatus("ошибка подключения: " + status));
                appendLog("BLE: ошибка подключения · код " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(BLE_SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic statusCharacteristic =
                            service.getCharacteristic(BLE_STATUS_UUID);
                    BluetoothGattCharacteristic authCharacteristic =
                            service.getCharacteristic(BLE_AUTH_UUID);
                    BluetoothGattCharacteristic commandCharacteristic =
                            service.getCharacteristic(BLE_COMMAND_UUID);
                    if (statusCharacteristic != null && authCharacteristic != null
                            && commandCharacteristic != null) {
                        runOnUiThread(() -> {
                            setBleStatus("подключено · ARENDO-сервис найден");
                            showBleClientAuth(true);
                            showBleClientCommands(false);
                        });
                        appendLog("BLE: ARENDO-сервис обнаружен · ожидается код");
                        readBleClientStatus(gatt, statusCharacteristic);
                    } else {
                        runOnUiThread(() -> setBleStatus(
                                "подключено · сервис неполный"));
                        appendLog("BLE: найденный сервис не поддерживает MVP-протокол");
                    }
                } else {
                    runOnUiThread(() -> setBleStatus(
                            "подключено · сервисов найдено: " + gatt.getServices().size()));
                    appendLog("BLE: сервисы обнаружены · " + gatt.getServices().size());
                }
            } else {
                runOnUiThread(() -> setBleStatus("ошибка сервисов: " + status));
                appendLog("BLE: не удалось получить сервисы · код " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS
                    && BLE_STATUS_UUID.equals(characteristic.getUuid())) {
                handleBleClientStatus(new String(characteristic.getValue(),
                        java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendLog("BLE: ошибка записи · код " + status);
                return;
            }
            BluetoothGattService service = gatt.getService(BLE_SERVICE_UUID);
            BluetoothGattCharacteristic statusCharacteristic = service == null ? null
                    : service.getCharacteristic(BLE_STATUS_UUID);
            if (statusCharacteristic != null) {
                mainHandler.postDelayed(() -> readBleClientStatus(gatt, statusCharacteristic), 180);
            }
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (granted && pendingDriver != null
                    && device != null && device.equals(pendingDriver.getDevice())) {
                openDriver(pendingDriver);
            } else {
                setConnection("USB-разрешение не получено");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep the top system area visually continuous with the navy header.
        // The camera cutout itself is controlled by Android/HyperOS, but the
        // surrounding status-bar background is part of the app presentation.
        getWindow().setStatusBarColor(COLOR_NAVY);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
        }
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        registerUsbReceiver();
        setContentView(buildUiArendoMobile());
    }

    private void registerUsbReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSION) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                granted &= result == PackageManager.PERMISSION_GRANTED;
            }
            if (granted) {
                startBleScan();
            } else {
                setBleStatus("разрешение Nearby devices не получено");
                appendLog("BLE: пользователь не дал разрешение Nearby devices");
            }
        } else if (requestCode == REQUEST_BLE_SERVER_PERMISSION) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                granted &= result == PackageManager.PERMISSION_GRANTED;
            }
            if (granted) {
                startBleServer();
            } else {
                setBleServerStatus("нет разрешения на Bluetooth-сервис");
                appendLog("BLE: разрешение на рекламу не получено");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLE_ENABLE && resultCode == RESULT_OK) {
            if (pendingBleServerStart) {
                pendingBleServerStart = false;
                startBleServer();
            } else {
                startBleScan();
            }
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 16);

        ScrollView page = new ScrollView(this);
        page.addView(root);

        TextView title = text("Postamat Modbus MVP", 22);
        root.addView(title);
        root.addView(text("X1 → Y1, X2 → Y2, X3 → Y3, X4 → Y4", 15));

        LinearLayout backendSettings = new LinearLayout(this);
        backendSettings.setOrientation(LinearLayout.VERTICAL);
        backendSettings.setPadding(0, 14, 0, 6);
        websocketUrlEdit = field("WebSocket URL",
                "wss://37.143.15.123/v1/device/socket");
        postamatIdEdit = field("Postamat ID", "map-7");
        // Test token supplied for the development postamat. Replace it with a
        // new token before any non-test deployment.
        tokenEdit = field("Device token", "pd_998c93a5d7b9eeba1b05f050a27847b359a09a5618661090");
        backendSettings.addView(websocketUrlEdit);
        backendSettings.addView(postamatIdEdit);
        backendSettings.addView(tokenEdit);
        insecureTlsCheck = new CheckBox(this);
        insecureTlsCheck.setText("Тестовый TLS: принять сертификат сервера без проверки");
        insecureTlsCheck.setChecked(true);
        backendSettings.addView(insecureTlsCheck);
        root.addView(backendSettings);

        Button backendConnect = new Button(this);
        backendConnect.setText("Подключить backend WebSocket");
        backendConnect.setOnClickListener(v -> connectBackend());
        root.addView(backendConnect);
        backendText = text("Backend: не подключён", 15);
        backendText.setPadding(0, 8, 0, 8);
        root.addView(backendText);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setPadding(0, 14, 0, 6);
        slaveIdEdit = field("Адрес устройства", "1");
        inputStartEdit = field("Первый адрес X (0 или 1)", "0");
        outputStartEdit = field("Первый адрес Y (0 или 1)", "0");
        settings.addView(slaveIdEdit);
        settings.addView(inputStartEdit);
        settings.addView(outputStartEdit);

        baudSpinner = new Spinner(this);
        String[] baudRates = {"9600", "19200", "38400", "115200"};
        baudSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, baudRates));
        settings.addView(baudSpinner);
        root.addView(settings);

        Button connect = new Button(this);
        connect.setText("Подключить USB–RS-485");
        connect.setOnClickListener(v -> findAndConnect());
        root.addView(connect);

        connectionText = text("Статус: не подключено", 15);
        connectionText.setPadding(0, 8, 0, 8);
        root.addView(connectionText);

        LinearLayout cells = new LinearLayout(this);
        cells.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 6, 0, 6);

            TextView cell = text("Ячейка " + (i + 1), 17);
            row.addView(cell, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            inputTexts[i] = text("X" + (i + 1) + ": —", 14);
            row.addView(inputTexts[i]);
            relayTexts[i] = text("Y" + (i + 1) + ": OFF", 14);
            row.addView(relayTexts[i]);
            Button open = new Button(this);
            open.setText("LED ON");
            open.setOnClickListener(v -> setRelay(index, true));
            row.addView(open);
            cells.addView(row);
        }
        root.addView(cells);

        Button allOff = new Button(this);
        allOff.setText("Выключить Y1–Y4");
        allOff.setOnClickListener(v -> ioExecutor.execute(() -> {
            for (int i = 0; i < 4; i++) {
                try {
                    writeRelay(i, false);
                } catch (Exception e) {
                    appendLog("Ошибка Y" + (i + 1) + ": " + e.getMessage());
                }
            }
        }));
        root.addView(allOff);

        root.addView(text("Лог", 17));
        logText = text("Готово. Адреса Modbus указаны с нуля.", 13);
        ScrollView logScroll = new ScrollView(this);
        logScroll.addView(logText);
        root.addView(logScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        return page;
    }

    private View buildUiArendoMobile() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(36), dp(14), dp(28));
        root.setBackgroundColor(COLOR_BACKGROUND);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(COLOR_BACKGROUND);
        page.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(15), dp(16), dp(15));
        header.setBackground(outlinedRounded(COLOR_NAVY, COLOR_BORDER, 14));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        TextView postamatTitle = text("ARENDO-042", 20);
        postamatTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        identity.addView(postamatTitle);
        TextView postamatSubtitle = text("Постамат · локальная панель", 12);
        postamatSubtitle.setTextColor(COLOR_MUTED);
        identity.addView(postamatSubtitle, blockParams(0));
        headerRow.addView(identity, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button refreshButton = actionButton("Обновить", COLOR_CARD, COLOR_TEXT);
        refreshButton.setTextSize(12);
        refreshButton.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        refreshButton.setOnClickListener(v -> requestManualRefresh(refreshButton));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(104), dp(48));
        refreshParams.rightMargin = dp(7);
        headerRow.addView(refreshButton, refreshParams);
        Button settingsButton = actionButton("⚙", COLOR_CARD, COLOR_TEXT);
        settingsButton.setTextSize(19);
        headerRow.addView(settingsButton, new LinearLayout.LayoutParams(dp(52), dp(48)));
        header.addView(headerRow, blockParams(12));

        overallStatusText = text("● Ожидает подключения", 13);
        overallStatusText.setTextColor(COLOR_AMBER);
        overallStatusText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        overallStatusText.setPadding(dp(12), dp(9), dp(12), dp(9));
        overallStatusText.setBackground(outlinedRounded(
                Color.rgb(28, 25, 18), Color.rgb(104, 76, 24), 9));
        header.addView(overallStatusText);
        LinearLayout.LayoutParams headerParams = blockParams(22);
        headerParams.topMargin = dp(52);
        root.addView(header, headerParams);

        TextView storageCode = sectionLabel("STORAGE-01");
        root.addView(storageCode, blockParams(5));
        LinearLayout cellsTitleRow = new LinearLayout(this);
        cellsTitleRow.setOrientation(LinearLayout.HORIZONTAL);
        cellsTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView cellsTitle = text("Физические ячейки", 25);
        cellsTitle.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        cellsTitleRow.addView(cellsTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cellsCount = text("4 ЯЧЕЙКИ", 11);
        cellsCount.setTextColor(COLOR_MUTED);
        cellsCount.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        cellsTitleRow.addView(cellsCount);
        root.addView(cellsTitleRow, blockParams(9));

        LinearLayout summaryTop = new LinearLayout(this);
        summaryTop.setOrientation(LinearLayout.HORIZONTAL);
        summaryTop.addView(summaryChip(0, "ЗАБЛОКИРОВАНЫ", COLOR_RED),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams attentionParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        attentionParams.leftMargin = dp(6);
        summaryTop.addView(summaryChip(1, "ВНИМАНИЕ", COLOR_AMBER), attentionParams);
        root.addView(summaryTop, blockParams(6));
        LinearLayout summaryBottom = new LinearLayout(this);
        summaryBottom.setOrientation(LinearLayout.HORIZONTAL);
        summaryBottom.addView(summaryChip(2, "В АРЕНДЕ", COLOR_BLUE),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams normalParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        normalParams.leftMargin = dp(6);
        summaryBottom.addView(summaryChip(3, "НОРМА", COLOR_GREEN), normalParams);
        root.addView(summaryBottom, blockParams(12));

        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout cellRow = new LinearLayout(this);
            cellRow.setOrientation(LinearLayout.HORIZONTAL);
            cellRow.setGravity(Gravity.CENTER_VERTICAL);
            cellRow.setPadding(dp(12), dp(12), dp(10), dp(12));
            cellRow.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 10));
            cellRow.setOnClickListener(v -> showCellDetails(index));

            TextView number = text(cellCodeForIndex(i), 17);
            number.setGravity(Gravity.CENTER);
            number.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            number.setBackground(rounded(Color.rgb(25, 35, 49), 7));
            cellRow.addView(number, new LinearLayout.LayoutParams(dp(50), dp(50)));

            LinearLayout physical = new LinearLayout(this);
            physical.setOrientation(LinearLayout.VERTICAL);
            TextView physicalLabel = text("ФИЗИЧЕСКИ", 9);
            physicalLabel.setLetterSpacing(0.12f);
            physicalLabel.setTextColor(COLOR_MUTED);
            physical.addView(physicalLabel, blockParams(3));
            inputTexts[i] = text("Закрыта", 15);
            inputTexts[i].setTextColor(COLOR_GREEN);
            physical.addView(inputTexts[i]);
            LinearLayout.LayoutParams physicalParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            physicalParams.leftMargin = dp(12);
            cellRow.addView(physical, physicalParams);

            LinearLayout business = new LinearLayout(this);
            business.setOrientation(LinearLayout.VERTICAL);
            TextView businessLabel = text("ARENDO", 9);
            businessLabel.setLetterSpacing(0.12f);
            businessLabel.setTextColor(COLOR_MUTED);
            business.addView(businessLabel, blockParams(3));
            relayTexts[i] = text(cellBusinessStates[i], 14);
            relayTexts[i].setTextColor(COLOR_TEXT);
            business.addView(relayTexts[i]);
            cellRow.addView(business, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = text("›", 28);
            arrow.setTextColor(COLOR_BLUE);
            cellRow.addView(arrow, new LinearLayout.LayoutParams(dp(24),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(cellRow, blockParams(8));
        }

        root.addView(sectionLabel("СОСТОЯНИЕ СИСТЕМЫ"), blockParams(6));
        LinearLayout statusCard = cardView();
        statusCard.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 12));
        backendText = text("Offline", 14);
        backendText.setTextColor(COLOR_RED);
        statusCard.addView(systemNode("Backend", backendText), blockParams(8));
        connectionText = text("Offline", 14);
        connectionText.setTextColor(COLOR_RED);
        statusCard.addView(systemNode("Controller · RS-485", connectionText), blockParams(8));
        bluetoothNodeText = text("Выключен", 14);
        bluetoothNodeText.setTextColor(COLOR_MUTED);
        statusCard.addView(systemNode("Bluetooth service", bluetoothNodeText), blockParams(8));
        TextView powerText = text("OK", 14);
        powerText.setTextColor(COLOR_GREEN);
        statusCard.addView(systemNode("Power · Android", powerText));
        root.addView(statusCard, blockParams(12));

        Button openLog = actionButton("Открыть журнал событий", COLOR_CARD, COLOR_TEXT);
        openLog.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 10));
        openLog.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        root.addView(openLog, blockParams(18));

        FrameLayout shell = new FrameLayout(this);
        mainScreen = page;
        shell.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView settingsPage = new ScrollView(this);
        settingsPage.setFillViewport(true);
        settingsPage.setBackgroundColor(COLOR_BACKGROUND);
        settingsScreen = settingsPage;
        settingsPage.setVisibility(View.GONE);
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setPadding(dp(14), dp(64), dp(14), dp(28));
        settingsPanel.setBackgroundColor(COLOR_BACKGROUND);
        LinearLayout settingsHeader = new LinearLayout(this);
        settingsHeader.setOrientation(LinearLayout.HORIZONTAL);
        settingsHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button back = actionButton("‹", COLOR_CARD, COLOR_TEXT);
        back.setTextSize(28);
        back.setOnClickListener(v -> showMainScreen());
        settingsHeader.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));
        TextView settingsTitle = text("Настройки", 24);
        settingsTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams settingsTitleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        settingsTitleParams.leftMargin = dp(10);
        settingsHeader.addView(settingsTitle, settingsTitleParams);
        settingsPanel.addView(settingsHeader, blockParams(20));
        settingsPanel.addView(sectionLabel("НАСТРОЙКИ BACKEND"), blockParams(6));
        settingsPanel.addView(buildBackendSettingsCard(), blockParams(12));
        settingsPanel.addView(sectionLabel("НАСТРОЙКИ MODBUS"), blockParams(6));
        settingsPanel.addView(buildModbusSettingsCard(), blockParams(12));
        settingsPanel.addView(sectionLabel("BLUETOOTH"), blockParams(6));
        settingsPanel.addView(buildBluetoothSettingsCard());
        settingsPage.addView(settingsPanel);
        shell.addView(settingsPage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        settingsButton.setOnClickListener(v -> showSettingsScreen());
        initBluetooth();
        refreshSummaryCounts();
        startDoorClock();
        return shell;
    }

    private LinearLayout summaryChip(int index, String label, int color) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(10), dp(8), dp(10), dp(8));
        chip.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 8));
        TextView dot = text("●", 10);
        dot.setTextColor(color);
        chip.addView(dot);
        TextView title = text(label, 9);
        title.setTextColor(COLOR_MUTED);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(5);
        chip.addView(title, titleParams);
        summaryCountTexts[index] = text("0", 13);
        summaryCountTexts[index].setTextColor(color);
        summaryCountTexts[index].setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        chip.addView(summaryCountTexts[index]);
        return chip;
    }

    private void showSettingsScreen() {
        if (settingsScreen != null) {
            settingsScreen.setVisibility(View.VISIBLE);
            settingsScreen.bringToFront();
        }
    }

    private void showMainScreen() {
        if (settingsScreen != null) {
            settingsScreen.setVisibility(View.GONE);
        }
    }

    private void requestManualRefresh(Button button) {
        if (backendClient == null || !backendClient.isOpen()) {
            button.setText("Backend offline");
            appendLog("Обновление не выполнено · Backend offline");
            Toast.makeText(this, "Сначала подключите Backend", Toast.LENGTH_SHORT).show();
            mainHandler.postDelayed(() -> button.setText("Обновить"), 1600);
            return;
        }
        button.setEnabled(false);
        button.setText("Обновление...");
        ioExecutor.execute(() -> {
            try {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("type", "heartbeat");
                heartbeat.put("cells", cellsSnapshot());
                heartbeat.put("net", new JSONObject().put("kind", "wifi"));
                heartbeat.put("problems", new JSONArray());
                heartbeat.put("source", "manual_refresh");
                sendBackend(heartbeat);
                appendLog("Состояние постамата обновлено вручную");
                runOnUiThread(() -> {
                    button.setText("Обновлено");
                    refreshSummaryCounts();
                });
            } catch (Exception error) {
                appendLog("Ошибка обновления: " + error.getMessage());
                runOnUiThread(() -> button.setText("Ошибка"));
            }
            mainHandler.postDelayed(() -> {
                button.setText("Обновить");
                button.setEnabled(true);
            }, 1600);
        });
    }

    private void startDoorClock() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < relayStates.length; i++) {
                    if (inputTexts[i] != null) {
                        inputTexts[i].setText(formatDoorState(i));
                        inputTexts[i].setTextColor(relayStates[i] ? COLOR_AMBER : COLOR_GREEN);
                    }
                }
                mainHandler.postDelayed(this, 1000);
            }
        });
    }

    private String formatDoorState(int index) {
        if (!relayStates[index]) {
            return "Закрыта";
        }
        long elapsed = openedAtMillis[index] == 0L
                ? 0L : Math.max(0L, (System.currentTimeMillis() - openedAtMillis[index]) / 1000L);
        return String.format(Locale.US, "Открыта · %02d:%02d", elapsed / 60L, elapsed % 60L);
    }

    private void refreshSummaryCounts() {
        int blocked = 0;
        int attention = 0;
        int rented = 0;
        int normal = 0;
        for (int i = 0; i < cellBusinessStates.length; i++) {
            String state = cellBusinessStates[i].toLowerCase(Locale.ROOT);
            boolean isBlocked = state.contains("блок");
            boolean needsAttention = relayStates[i] || state.contains("вниман");
            boolean isRented = state.contains("аренд");
            if (isBlocked) {
                blocked++;
            }
            if (needsAttention) {
                attention++;
            }
            if (isRented) {
                rented++;
            }
            if (!isBlocked && !needsAttention) {
                normal++;
            }
        }
        int[] values = {blocked, attention, rented, normal};
        for (int i = 0; i < summaryCountTexts.length; i++) {
            if (summaryCountTexts[i] != null) {
                summaryCountTexts[i].setText(String.valueOf(values[i]));
            }
        }
    }

    private LinearLayout systemNode(String label, TextView valueView) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(label, 12);
        labelView.setTextColor(COLOR_MUTED);
        row.addView(labelView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        valueView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        row.addView(valueView);
        return row;
    }

    private LinearLayout buildBackendSettingsCard() {
        LinearLayout card = cardView();
        card.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 12));
        TextView title = text("Backend WebSocket", 17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, blockParams(10));
        websocketUrlEdit = field("WebSocket URL",
                "wss://37.143.15.123/v1/device/socket");
        postamatIdEdit = field("Postamat ID", "map-7");
        tokenEdit = field("Device token",
                "pd_998c93a5d7b9eeba1b05f050a27847b359a09a5618661090");
        card.addView(websocketUrlEdit, blockParams(7));
        card.addView(postamatIdEdit, blockParams(7));
        card.addView(tokenEdit, blockParams(7));
        insecureTlsCheck = new CheckBox(this);
        insecureTlsCheck.setText("Тестовый TLS без проверки сертификата");
        insecureTlsCheck.setTextColor(COLOR_MUTED);
        insecureTlsCheck.setChecked(true);
        card.addView(insecureTlsCheck, blockParams(7));
        Button localBridgeButton = actionButton("Локальный сервер ноутбука (USB)", COLOR_CARD, COLOR_BLUE);
        localBridgeButton.setOnClickListener(v -> {
            websocketUrlEdit.setText("ws://127.0.0.1:8765/v1/device/socket");
            tokenEdit.setText("local-dev");
            insecureTlsCheck.setChecked(false);
            appendLog("Backend: выбран локальный сервер ноутбука через USB");
            Toast.makeText(this, "URL локального сервера подставлен", Toast.LENGTH_SHORT).show();
        });
        card.addView(localBridgeButton, blockParams(7));
        Button wifiBridgeButton = actionButton("Локальный сервер по Wi‑Fi", COLOR_CARD, COLOR_BLUE);
        wifiBridgeButton.setOnClickListener(v -> {
            websocketUrlEdit.setText("ws://172.20.10.3:8765/v1/device/socket");
            tokenEdit.setText("local-dev");
            insecureTlsCheck.setChecked(false);
            appendLog("Backend: выбран локальный сервер ноутбука по Wi‑Fi");
            Toast.makeText(this, "Wi‑Fi URL ноутбука подставлен", Toast.LENGTH_SHORT).show();
        });
        card.addView(wifiBridgeButton, blockParams(7));
        backendActionButton = actionButton("Подключить backend", COLOR_BLUE, Color.WHITE);
        backendActionButton.setOnClickListener(v -> {
            backendActionButton.setText("Подключение...");
            backendActionButton.setEnabled(false);
            connectBackend();
        });
        card.addView(backendActionButton);
        return card;
    }

    private LinearLayout buildModbusSettingsCard() {
        LinearLayout card = cardView();
        card.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 12));
        TextView title = text("Modbus RTU · RS-485", 17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, blockParams(10));
        LinearLayout addresses = new LinearLayout(this);
        addresses.setOrientation(LinearLayout.HORIZONTAL);
        slaveIdEdit = field("Адрес", "1");
        inputStartEdit = field("X старт", "0");
        outputStartEdit = field("Y старт", "0");
        addresses.addView(slaveIdEdit, smallFieldParams(0));
        addresses.addView(inputStartEdit, smallFieldParams(7));
        addresses.addView(outputStartEdit, smallFieldParams(7));
        card.addView(addresses, blockParams(8));
        baudSpinner = new Spinner(this);
        String[] baudRates = {"9600", "19200", "38400", "115200"};
        baudSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, baudRates));
        baudSpinner.setBackground(outlinedRounded(Color.rgb(21, 29, 40), COLOR_BORDER, 9));
        card.addView(baudSpinner, blockParams(8));
        modbusActionButton = actionButton("Подключить USB · RS-485", COLOR_BLUE, Color.WHITE);
        modbusActionButton.setOnClickListener(v -> {
            modbusActionButton.setText("Поиск контроллера...");
            modbusActionButton.setEnabled(false);
            findAndConnect();
        });
        card.addView(modbusActionButton);
        return card;
    }

    private LinearLayout buildBluetoothSettingsCard() {
        LinearLayout card = cardView();
        card.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 12));
        TextView title = text("Bluetooth service", 17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, blockParams(5));
        TextView hint = text("Локальное подключение второго телефона с одноразовым кодом.", 12);
        hint.setTextColor(COLOR_MUTED);
        card.addView(hint, blockParams(9));

        bleNameEdit = field("Имя устройства", "ARENDO-LOCKER");
        card.addView(bleNameEdit, blockParams(7));
        bleStatusText = text("BLE: не подключён", 12);
        bleStatusText.setTextColor(COLOR_MUTED);
        card.addView(bleStatusText, blockParams(4));
        bleSessionText = text("Сессия: не создана", 12);
        bleSessionText.setTextColor(COLOR_MUTED);
        card.addView(bleSessionText, blockParams(7));

        LinearLayout clientButtons = new LinearLayout(this);
        clientButtons.setOrientation(LinearLayout.HORIZONTAL);
        bleScanActionButton = actionButton("Сканировать", COLOR_BLUE, Color.WHITE);
        bleScanActionButton.setOnClickListener(v -> startBleScan());
        clientButtons.addView(bleScanActionButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button disconnect = actionButton("Отключить", COLOR_CARD, COLOR_TEXT);
        disconnect.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        disconnect.setOnClickListener(v -> disconnectBle());
        LinearLayout.LayoutParams disconnectParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        disconnectParams.leftMargin = dp(7);
        clientButtons.addView(disconnect, disconnectParams);
        card.addView(clientButtons, blockParams(8));

        bleDevicesContainer = new LinearLayout(this);
        bleDevicesContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(bleDevicesContainer, blockParams(9));

        bleServerStatusText = text("BLE-сервис: выключен", 12);
        bleServerStatusText.setTextColor(COLOR_MUTED);
        card.addView(bleServerStatusText, blockParams(4));
        bleServerSessionText = text("Сессия сервиса: не создана", 12);
        bleServerSessionText.setTextColor(COLOR_MUTED);
        card.addView(bleServerSessionText, blockParams(7));
        LinearLayout serverButtons = new LinearLayout(this);
        serverButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button start = actionButton("Запустить сервис", COLOR_BLUE, Color.WHITE);
        start.setOnClickListener(v -> startBleServer());
        serverButtons.addView(start, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button stop = actionButton("Остановить", COLOR_CARD, COLOR_TEXT);
        stop.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        stop.setOnClickListener(v -> stopBleServer());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        stopParams.leftMargin = dp(7);
        serverButtons.addView(stop, stopParams);
        card.addView(serverButtons, blockParams(8));

        bleClientAuthContainer = new LinearLayout(this);
        bleClientAuthContainer.setOrientation(LinearLayout.VERTICAL);
        bleClientAuthContainer.setVisibility(View.GONE);
        bleClientCodeEdit = field("Одноразовый код", "");
        bleClientAuthContainer.addView(bleClientCodeEdit, blockParams(6));
        Button auth = actionButton("Подтвердить код", COLOR_BLUE, Color.WHITE);
        auth.setOnClickListener(v -> authenticateBleClient());
        bleClientAuthContainer.addView(auth);
        card.addView(bleClientAuthContainer, blockParams(8));

        bleClientCommandContainer = new LinearLayout(this);
        bleClientCommandContainer.setOrientation(LinearLayout.VERTICAL);
        bleClientCommandContainer.setVisibility(View.GONE);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            Button open = actionButton("Открыть D" + (i + 1), COLOR_CARD, COLOR_BLUE);
            open.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
            open.setOnClickListener(v -> sendBleClientCommand(index));
            bleClientCommandContainer.addView(open, blockParams(5));
        }
        card.addView(bleClientCommandContainer);
        return card;
    }

    private void showCellDetails(int index) {
        LinearLayout content = dialogPanel();
        TextView eyebrow = sectionLabel("ПОДРОБНЕЕ");
        content.addView(eyebrow, blockParams(5));
        TextView title = text("Ячейка " + cellCodeForIndex(index), 26);
        content.addView(title, blockParams(14));

        TextView physicalHeader = sectionLabel("ФИЗИЧЕСКОЕ СОСТОЯНИЕ");
        content.addView(physicalHeader, blockParams(8));
        TextView doorValue = text(relayStates[index] ? "OPEN" : "CLOSED", 15);
        doorValue.setTextColor(relayStates[index] ? COLOR_AMBER : COLOR_GREEN);
        content.addView(systemNode("Door sensor", doorValue), blockParams(8));
        TextView channelValue = text("LOCK-" + cellCodeForIndex(index), 14);
        content.addView(systemNode("Lock channel", channelValue), blockParams(8));
        TextView openedValue = text(lastOpenedAt[index], 14);
        content.addView(systemNode("Последнее открытие", openedValue), blockParams(8));
        TextView closedValue = text(lastClosedAt[index], 14);
        content.addView(systemNode("Последнее закрытие", closedValue), blockParams(14));

        content.addView(sectionLabel("ARENDO"), blockParams(8));
        TextView rentalValue = text(cellBusinessStates[index], 14);
        rentalValue.setTextColor(COLOR_BLUE);
        content.addView(systemNode("Статус", rentalValue), blockParams(8));
        TextView toolValue = text(cellToolNames[index], 14);
        content.addView(systemNode("Инструмент", toolValue), blockParams(8));
        TextView unitValue = text(cellToolUnits[index], 14);
        content.addView(systemNode("ToolUnit", unitValue), blockParams(8));
        TextView rentalIdValue = text(cellRentalIds[index], 14);
        content.addView(systemNode("Rental ID", rentalIdValue), blockParams(16));

        Button open = actionButton("Открыть ячейку", COLOR_BLUE, Color.WHITE);
        content.addView(open, blockParams(8));
        TextView note = text("Сервисная команда · аренду не изменяет", 11);
        note.setTextColor(COLOR_MUTED);
        content.addView(note, blockParams(12));
        Button diagnostics = actionButton("Диагностика ячейки", COLOR_CARD, COLOR_TEXT);
        diagnostics.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        diagnostics.setOnClickListener(v -> runCellDiagnostics(index));
        content.addView(diagnostics, blockParams(7));
        boolean blocked = cellBusinessStates[index].toLowerCase(Locale.ROOT).contains("блок");
        Button block = actionButton(blocked ? "Разблокировать ячейку" : "Заблокировать ячейку",
                COLOR_CARD, COLOR_RED);
        block.setBackground(outlinedRounded(COLOR_CARD, COLOR_RED, 9));
        content.addView(block, blockParams(7));
        Button journal = actionButton("Последние события", COLOR_CARD, COLOR_TEXT);
        journal.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        journal.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        content.addView(journal, blockParams(7));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        Button close = actionButton("Закрыть", Color.TRANSPARENT, COLOR_MUTED);
        close.setOnClickListener(v -> dialog.dismiss());
        content.addView(close);
        open.setOnClickListener(v -> showOpenConfirmation(index, dialog));
        block.setOnClickListener(v -> showBlockConfirmation(index, dialog));
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(rounded(Color.TRANSPARENT, 0));
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showOpenConfirmation(int index, AlertDialog detailsDialog) {
        LinearLayout content = dialogPanel();
        TextView title = text("Открыть ячейку " + cellCodeForIndex(index) + "?", 22);
        content.addView(title, blockParams(8));
        TextView note = text("Это сервисная команда. Она физически откроет замок и не изменит аренду или тарификацию.", 12);
        note.setTextColor(COLOR_MUTED);
        content.addView(note, blockParams(12));
        TextView reasonLabel = text("Причина *", 13);
        reasonLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(reasonLabel, blockParams(5));
        EditText reason = field("Например: проверка замка", "");
        reason.setSingleLine(false);
        reason.setMinHeight(dp(92));
        reason.setGravity(Gravity.TOP);
        reason.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(reason, blockParams(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        AlertDialog confirmation = new AlertDialog.Builder(this).setView(content).create();
        Button cancel = actionButton("Отмена", COLOR_CARD, COLOR_TEXT);
        cancel.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
        cancel.setOnClickListener(v -> confirmation.dismiss());
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button confirm = actionButton("Открыть", COLOR_BLUE, Color.WHITE);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        confirmParams.leftMargin = dp(7);
        actions.addView(confirm, confirmParams);
        content.addView(actions);
        confirm.setOnClickListener(v -> {
            String reasonText = reason.getText().toString().trim();
            if (reasonText.isEmpty()) {
                Toast.makeText(this, "Укажите причину открытия", Toast.LENGTH_SHORT).show();
                return;
            }
            appendLog("Сервисное открытие D" + (index + 1) + " · " + reasonText);
            openCellFromService(index);
            confirmation.dismiss();
            detailsDialog.dismiss();
        });
        confirmation.show();
        if (confirmation.getWindow() != null) {
            confirmation.getWindow().setBackgroundDrawable(rounded(Color.TRANSPARENT, 0));
            confirmation.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void runCellDiagnostics(int index) {
        String door = relayStates[index] ? "OPEN" : "CLOSED";
        String controller = modbus == null ? "OFFLINE" : "ONLINE";
        String backend = backendClient != null && backendClient.isOpen()
                ? "ONLINE" : "OFFLINE";
        String result = "D" + (index + 1) + " · Door " + door
                + " · RS-485 " + controller + " · Backend " + backend;
        appendLog("Диагностика: " + result);
        new AlertDialog.Builder(this)
                .setTitle("Диагностика ячейки " + cellCodeForIndex(index))
                .setMessage(result)
                .setPositiveButton("Готово", null)
                .show();
    }

    private void showBlockConfirmation(int index, AlertDialog detailsDialog) {
        boolean blocked = cellBusinessStates[index].toLowerCase(Locale.ROOT).contains("блок");
        String action = blocked ? "Разблокировать" : "Заблокировать";
        new AlertDialog.Builder(this)
                .setTitle(action + " ячейку " + cellCodeForIndex(index) + "?")
                .setMessage(blocked
                        ? "Ячейка снова станет доступной для операций."
                        : "Новые операции с ячейкой будут запрещены до разблокировки.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton(action, (dialog, which) -> {
                    cellBusinessStates[index] = blocked ? "Доступна" : "Заблокирована";
                    if (relayTexts[index] != null) {
                        relayTexts[index].setText(cellBusinessStates[index]);
                        relayTexts[index].setTextColor(blocked ? COLOR_TEXT : COLOR_RED);
                    }
                    refreshSummaryCounts();
                    appendLog("D" + (index + 1) + (blocked
                            ? " разблокирована" : " заблокирована"));
                    sendEvent(blocked ? "cell_unblocked" : "cell_blocked",
                            cellCodeForIndex(index), "admin_console");
                    detailsDialog.dismiss();
                })
                .show();
    }

    private LinearLayout dialogPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(18));
        panel.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 14));
        return panel;
    }

    private void openCellFromService(int index) {
        ioExecutor.execute(() -> {
            boolean ok = writeRelayBlocking(index, true);
            if (ok) {
                appendLog("D" + (index + 1) + " open · service");
                sendEvent("door_opened", cellCodeForIndex(index), "service");
            } else {
                appendLog("D" + (index + 1) + " ошибка · Modbus недоступен");
            }
        });
    }

    private View buildUiModern() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(24));
        root.setBackgroundColor(COLOR_BACKGROUND);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(COLOR_BACKGROUND);
        page.addView(root);

        LinearLayout header = cardView();
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(rounded(COLOR_NAVY, 22));
        TextView headerText = text("ARENDO\nПостамат · панель администратора", 20);
        headerText.setTextColor(Color.WHITE);
        headerText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerText.setLineSpacing(0, 1.08f);
        header.addView(headerText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView headerBadge = text("ADMIN", 11);
        headerBadge.setTextColor(Color.rgb(191, 219, 254));
        headerBadge.setGravity(Gravity.CENTER);
        headerBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(headerBadge, new LinearLayout.LayoutParams(
                dp(60), ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams headerParams = blockParams(12);
        headerParams.topMargin = dp(64);
        root.addView(header, headerParams);

        root.addView(sectionLabel("СТАТУС СИСТЕМЫ"));
        LinearLayout backendCard = cardView();
        TextView backendTitle = text("Backend WebSocket", 17);
        backendTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        backendCard.addView(backendTitle);
        TextView backendHint = text("Сервер управляет командами открытия и получает статусы.", 12);
        backendHint.setTextColor(COLOR_MUTED);
        backendCard.addView(backendHint, blockParams(10));

        LinearLayout backendSettings = new LinearLayout(this);
        backendSettings.setOrientation(LinearLayout.VERTICAL);
        websocketUrlEdit = field("WebSocket URL", "wss://37.143.15.123/v1/device/socket");
        postamatIdEdit = field("Postamat ID", "map-7");
        tokenEdit = field("Device token", "pd_998c93a5d7b9eeba1b05f050a27847b359a09a5618661090");
        backendSettings.addView(websocketUrlEdit);
        backendSettings.addView(postamatIdEdit, blockParams(8));
        backendSettings.addView(tokenEdit, blockParams(8));
        insecureTlsCheck = new CheckBox(this);
        insecureTlsCheck.setText("Тестовый TLS без проверки сертификата");
        insecureTlsCheck.setTextSize(12);
        insecureTlsCheck.setTextColor(COLOR_MUTED);
        insecureTlsCheck.setChecked(true);
        backendSettings.addView(insecureTlsCheck, blockParams(8));
        backendSettings.setVisibility(View.GONE);
        Button backendSettingsToggle = actionButton("Показать настройки", Color.rgb(241, 245, 249), COLOR_NAVY);
        backendSettingsToggle.setOnClickListener(v -> {
            boolean show = backendSettings.getVisibility() != View.VISIBLE;
            backendSettings.setVisibility(show ? View.VISIBLE : View.GONE);
            backendSettingsToggle.setText(show ? "Скрыть настройки" : "Показать настройки");
        });
        backendCard.addView(backendSettingsToggle, blockParams(8));
        backendCard.addView(backendSettings);

        Button backendConnect = actionButton("Подключить к серверу", COLOR_BLUE, Color.WHITE);
        backendConnect.setOnClickListener(v -> connectBackend());
        backendCard.addView(backendConnect, blockParams(10));
        backendText = text("Backend: не подключён", 13);
        backendText.setTextColor(COLOR_MUTED);

        LinearLayout modbusCard = cardView();
        LinearLayout modbusHeader = new LinearLayout(this);
        modbusHeader.setOrientation(LinearLayout.HORIZONTAL);
        TextView modbusTitle = text("Modbus RTU", 17);
        modbusTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        modbusHeader.addView(modbusTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView modbusType = text("RS-485", 12);
        modbusType.setTextColor(COLOR_BLUE);
        modbusType.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        modbusHeader.addView(modbusType);
        modbusCard.addView(modbusHeader);

        connectionText = text("Статус: не подключено", 13);
        connectionText.setTextColor(COLOR_MUTED);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        slaveIdEdit = field("Адрес", "1");
        inputStartEdit = field("X старт", "0");
        outputStartEdit = field("Y старт", "0");
        settings.addView(slaveIdEdit, smallFieldParams(0));
        settings.addView(inputStartEdit, smallFieldParams(8));
        settings.addView(outputStartEdit, smallFieldParams(8));
        modbusCard.addView(settings, blockParams(8));

        baudSpinner = new Spinner(this);
        String[] baudRates = {"9600", "19200", "38400", "115200"};
        baudSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, baudRates));
        baudSpinner.setBackground(rounded(Color.rgb(248, 250, 252), 12));
        modbusCard.addView(baudSpinner, blockParams(10));

        settings.setVisibility(View.GONE);
        baudSpinner.setVisibility(View.GONE);
        Button modbusSettingsToggle = actionButton("Показать настройки Modbus",
                Color.rgb(241, 245, 249), COLOR_NAVY);
        modbusSettingsToggle.setOnClickListener(v -> {
            boolean show = settings.getVisibility() != View.VISIBLE;
            settings.setVisibility(show ? View.VISIBLE : View.GONE);
            baudSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
            modbusSettingsToggle.setText(show
                    ? "Скрыть настройки Modbus" : "Показать настройки Modbus");
        });
        modbusCard.addView(modbusSettingsToggle, blockParams(8));

        Button connect = actionButton("Подключить USB · RS-485", COLOR_NAVY, Color.WHITE);
        connect.setOnClickListener(v -> findAndConnect());
        modbusCard.addView(connect);

        LinearLayout quickStatus = cardView();
        quickStatus.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout backendQuick = new LinearLayout(this);
        backendQuick.setOrientation(LinearLayout.VERTICAL);
        TextView backendQuickTitle = text("BACKEND", 11);
        backendQuickTitle.setTextColor(COLOR_MUTED);
        backendQuickTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        backendQuick.addView(backendQuickTitle, blockParams(3));
        backendQuick.addView(backendText, blockParams(7));
        Button backendQuickButton = actionButton("Подключить", COLOR_BLUE, Color.WHITE);
        backendQuickButton.setTextSize(12);
        backendQuickButton.setMinHeight(dp(42));
        backendQuickButton.setOnClickListener(v -> connectBackend());
        backendQuick.addView(backendQuickButton);
        quickRow.addView(backendQuick, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout modbusQuick = new LinearLayout(this);
        modbusQuick.setOrientation(LinearLayout.VERTICAL);
        TextView modbusQuickTitle = text("MODBUS RTU", 11);
        modbusQuickTitle.setTextColor(COLOR_MUTED);
        modbusQuickTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        modbusQuick.addView(modbusQuickTitle, blockParams(3));
        modbusQuick.addView(connectionText, blockParams(7));
        Button modbusQuickButton = actionButton("Подключить", COLOR_NAVY, Color.WHITE);
        modbusQuickButton.setTextSize(12);
        modbusQuickButton.setMinHeight(dp(42));
        modbusQuickButton.setOnClickListener(v -> findAndConnect());
        modbusQuick.addView(modbusQuickButton);
        LinearLayout.LayoutParams modbusQuickParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        modbusQuickParams.leftMargin = dp(8);
        quickRow.addView(modbusQuick, modbusQuickParams);
        quickStatus.addView(quickRow);
        root.addView(quickStatus, blockParams(14));

        root.addView(sectionLabel("BLUETOOTH BLE"), blockParams(2));
        LinearLayout bleCard = cardView();
        TextView bleTitle = text("Bluetooth-контроллер", 17);
        bleTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bleCard.addView(bleTitle);
        TextView bleHint = text(
                "Поиск устройства, подключение и новая сессия при каждом соединении.", 12);
        bleHint.setTextColor(COLOR_MUTED);
        bleCard.addView(bleHint, blockParams(9));
        bleNameEdit = field("Ожидаемое имя устройства", "ARENDO-LOCKER");
        bleCard.addView(bleNameEdit, blockParams(8));
        bleStatusText = text("BLE: не подключён", 13);
        bleStatusText.setTextColor(COLOR_MUTED);
        bleCard.addView(bleStatusText, blockParams(4));
        bleSessionText = text("Сессия: не создана", 12);
        bleSessionText.setTextColor(COLOR_MUTED);
        bleCard.addView(bleSessionText, blockParams(9));

        LinearLayout bleButtons = new LinearLayout(this);
        bleButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button bleScanButton = actionButton("Сканировать BLE", COLOR_BLUE, Color.WHITE);
        bleScanButton.setOnClickListener(v -> startBleScan());
        bleButtons.addView(bleScanButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button bleDisconnectButton = actionButton("Отключить", Color.rgb(241, 245, 249), COLOR_NAVY);
        bleDisconnectButton.setOnClickListener(v -> disconnectBle());
        LinearLayout.LayoutParams disconnectParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        disconnectParams.leftMargin = dp(8);
        bleButtons.addView(bleDisconnectButton, disconnectParams);
        bleCard.addView(bleButtons, blockParams(9));

        TextView bleDevicesTitle = text("Найденные устройства", 12);
        bleDevicesTitle.setTextColor(COLOR_MUTED);
        bleCard.addView(bleDevicesTitle, blockParams(5));
        bleDevicesContainer = new LinearLayout(this);
        bleDevicesContainer.setOrientation(LinearLayout.VERTICAL);
        bleCard.addView(bleDevicesContainer);

        TextView bleServiceTitle = text("Временный сервисный режим", 14);
        bleServiceTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bleCard.addView(bleServiceTitle, blockParams(10));
        TextView bleServiceHint = text(
                "На этом Android можно временно разрешить подключение второго телефона. "
                        + "Сервис принимает команды только после одноразового кода.", 12);
        bleServiceHint.setTextColor(COLOR_MUTED);
        bleCard.addView(bleServiceHint, blockParams(7));
        bleServerStatusText = text("BLE-сервис: выключен", 12);
        bleServerStatusText.setTextColor(COLOR_MUTED);
        bleCard.addView(bleServerStatusText, blockParams(4));
        bleServerSessionText = text("Сессия сервиса: не создана", 12);
        bleServerSessionText.setTextColor(COLOR_MUTED);
        bleCard.addView(bleServerSessionText, blockParams(7));

        LinearLayout serverButtons = new LinearLayout(this);
        serverButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button startServerButton = actionButton("Запустить сервис", COLOR_NAVY, Color.WHITE);
        startServerButton.setOnClickListener(v -> startBleServer());
        serverButtons.addView(startServerButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button stopServerButton = actionButton("Остановить", Color.rgb(241, 245, 249), COLOR_NAVY);
        stopServerButton.setOnClickListener(v -> stopBleServer());
        LinearLayout.LayoutParams stopServerParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stopServerParams.leftMargin = dp(8);
        serverButtons.addView(stopServerButton, stopServerParams);
        bleCard.addView(serverButtons, blockParams(8));

        bleClientAuthContainer = new LinearLayout(this);
        bleClientAuthContainer.setOrientation(LinearLayout.VERTICAL);
        bleClientAuthContainer.setVisibility(View.GONE);
        TextView codeHint = text("Код с экрана Android-постамата", 12);
        codeHint.setTextColor(COLOR_MUTED);
        bleClientAuthContainer.addView(codeHint, blockParams(4));
        bleClientCodeEdit = field("Одноразовый код", "");
        bleClientAuthContainer.addView(bleClientCodeEdit, blockParams(6));
        Button authButton = actionButton("Подтвердить код", COLOR_BLUE, Color.WHITE);
        authButton.setOnClickListener(v -> authenticateBleClient());
        bleClientAuthContainer.addView(authButton);
        bleCard.addView(bleClientAuthContainer, blockParams(8));

        bleClientCommandContainer = new LinearLayout(this);
        bleClientCommandContainer.setOrientation(LinearLayout.VERTICAL);
        bleClientCommandContainer.setVisibility(View.GONE);
        TextView commandHint = text("Команды сервисного режима", 12);
        commandHint.setTextColor(COLOR_MUTED);
        bleClientCommandContainer.addView(commandHint, blockParams(4));
        for (int i = 0; i < 4; i++) {
            final int cellIndex = i;
            Button commandButton = actionButton("Открыть D" + (i + 1),
                    Color.rgb(239, 246, 255), COLOR_BLUE);
            commandButton.setOnClickListener(v -> sendBleClientCommand(cellIndex));
            bleClientCommandContainer.addView(commandButton, blockParams(5));
        }
        bleCard.addView(bleClientCommandContainer, blockParams(4));
        root.addView(bleCard, blockParams(14));
        initBluetooth();

        LinearLayout cellsHeader = new LinearLayout(this);
        cellsHeader.setOrientation(LinearLayout.HORIZONTAL);
        TextView cellsTitle = sectionLabel("ЯЧЕЙКИ");
        cellsHeader.addView(cellsTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView cellsHint = text("4 канала", 12);
        cellsHint.setTextColor(COLOR_MUTED);
        cellsHint.setGravity(Gravity.CENTER_VERTICAL);
        cellsHeader.addView(cellsHint);
        root.addView(cellsHeader, blockParams(2));
        TextView cellsSubtitle = text("LED = дверь открыта · нажатие кнопки = дверь закрыта", 12);
        cellsSubtitle.setTextColor(COLOR_MUTED);
        root.addView(cellsSubtitle, blockParams(8));

        GridLayout cells = new GridLayout(this);
        cells.setColumnCount(2);
        cells.setUseDefaultMargins(false);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout cellCard = cardView();
            cellCard.setPadding(dp(14), dp(14), dp(14), dp(14));
            TextView cell = text("Ячейка " + cellCodeForIndex(i), 16);
            cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            cellCard.addView(cell, blockParams(8));
            inputTexts[i] = text("Кнопка X" + (i + 1) + ": —", 12);
            inputTexts[i].setTextColor(COLOR_MUTED);
            cellCard.addView(inputTexts[i], blockParams(4));
            relayTexts[i] = text("Дверь: закрыта · LED OFF", 12);
            relayTexts[i].setTextColor(COLOR_MUTED);
            cellCard.addView(relayTexts[i], blockParams(10));
            Button open = actionButton("Включить LED", Color.rgb(239, 246, 255), COLOR_BLUE);
            open.setOnClickListener(v -> setRelay(index, true));
            cellCard.addView(open);

            GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 2, 1, 1f), GridLayout.spec(i % 2, 1, 1f));
            cellParams.width = 0;
            cellParams.setMargins(i % 2 == 0 ? 0 : dp(5), 0,
                    i % 2 == 0 ? dp(5) : 0, dp(10));
            cells.addView(cellCard, cellParams);
        }
        root.addView(cells, blockParams(4));

        Button allOff = actionButton("Выключить все LED", Color.rgb(254, 242, 242),
                Color.rgb(185, 28, 28));
        allOff.setOnClickListener(v -> ioExecutor.execute(() -> {
            for (int i = 0; i < 4; i++) {
                try {
                    writeRelay(i, false);
                } catch (Exception e) {
                    appendLog("Ошибка Y" + (i + 1) + ": " + e.getMessage());
                }
            }
        }));
        root.addView(allOff, blockParams(14));

        root.addView(sectionLabel("ЖУРНАЛ"));
        LinearLayout logCard = cardView();
        TextView logHint = text("Обратная связь Backend и Modbus доступна в полном журнале.", 12);
        logHint.setTextColor(COLOR_MUTED);
        logCard.addView(logHint, blockParams(10));
        Button openLog = actionButton("Открыть полный журнал", Color.rgb(241, 245, 249), COLOR_NAVY);
        openLog.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        logCard.addView(openLog);
        root.addView(logCard);

        root.addView(sectionLabel("НАСТРОЙКИ BACKEND"), blockParams(2));
        root.addView(backendCard, blockParams(14));
        root.addView(sectionLabel("НАСТРОЙКИ MODBUS"), blockParams(2));
        root.addView(modbusCard);

        return page;
    }

    private LinearLayout cardView() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setBackground(rounded(COLOR_CARD, 18));
        view.setElevation(dp(2));
        return view;
    }

    private TextView sectionLabel(String value) {
        TextView view = text(value, 12);
        view.setTextColor(COLOR_MUTED);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private Button actionButton(String label, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(backgroundColor, 12));
        return button;
    }

    private LinearLayout.LayoutParams blockParams(int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    private LinearLayout.LayoutParams smallFieldParams(int leftDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(52), 1f);
        params.leftMargin = dp(leftDp);
        return params;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable outlinedRounded(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = rounded(color, radiusDp);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private EditText field(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setSingleLine(true);
        edit.setTextSize(14);
        edit.setTextColor(COLOR_TEXT);
        edit.setHintTextColor(COLOR_MUTED);
        edit.setPadding(dp(13), 0, dp(13), 0);
        edit.setMinHeight(dp(52));
        edit.setBackground(outlinedRounded(Color.rgb(21, 29, 40), COLOR_BORDER, 9));
        return edit;
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(COLOR_TEXT);
        return view;
    }

    private void initBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager == null ? null : manager.getAdapter();
        if (bluetoothAdapter == null) {
            setBleStatus("BLE недоступен на этом телефоне");
        }
        renderBleDevices();
    }

    private boolean hasBleConnectPermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBleScanPermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBleAdvertisePermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBleServerPermission() {
        return Build.VERSION.SDK_INT < 31
                || (hasBleConnectPermission() && hasBleAdvertisePermission());
    }

    private void startBleServer() {
        if (bluetoothAdapter == null) {
            initBluetooth();
        }
        if (bluetoothAdapter == null) {
            setBleServerStatus("BLE недоступен на этом телефоне");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && !hasBleServerPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, REQUEST_BLE_SERVER_PERMISSION);
            return;
        }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                pendingBleServerStart = true;
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_BLE_ENABLE);
                setBleServerStatus("включите Bluetooth и повторите запуск");
                return;
            }
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                setBleServerStatus("этот телефон не поддерживает BLE-рекламу");
                appendLog("BLE: BluetoothLeAdvertiser недоступен");
                return;
            }
            stopBleServer();
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            bleGattServer = manager == null ? null
                    : manager.openGattServer(this, bleGattServerCallback);
            if (bleGattServer == null) {
                setBleServerStatus("не удалось открыть GATT-сервер");
                appendLog("BLE: GATT-сервер не открылся");
                return;
            }
            bleServerStatusCharacteristic = new BluetoothGattCharacteristic(
                    BLE_STATUS_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            bleServerAuthCharacteristic = new BluetoothGattCharacteristic(
                    BLE_AUTH_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            bleServerCommandCharacteristic = new BluetoothGattCharacteristic(
                    BLE_COMMAND_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            BluetoothGattService service = new BluetoothGattService(
                    BLE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            service.addCharacteristic(bleServerStatusCharacteristic);
            service.addCharacteristic(bleServerAuthCharacteristic);
            service.addCharacteristic(bleServerCommandCharacteristic);
            bleServerStatusValue = "OFF";
            generateBleServerSession();
            bleAdvertiser = advertiser;
            if (!bleGattServer.addService(service)) {
                setBleServerStatus("не удалось добавить GATT-сервис");
                appendLog("BLE: addService вернул false");
            } else {
                setBleServerStatus("запуск BLE-сервиса...");
            }
        } catch (SecurityException error) {
            setBleServerStatus("нет разрешения на BLE-сервис");
            appendLog("BLE: запуск сервиса запрещён системой");
        }
    }

    private void startBleAdvertising() {
        if (bleAdvertiser == null) {
            return;
        }
        try {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(true)
                    .setTimeout(0)
                    .build();
            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .addServiceUuid(new ParcelUuid(BLE_SERVICE_UUID))
                    .build();
            bleAdvertiser.startAdvertising(settings, data, bleAdvertiseCallback);
        } catch (SecurityException error) {
            setBleServerStatus("нет разрешения на рекламу BLE");
            appendLog("BLE: реклама запрещена системой");
        }
    }

    private void stopBleServer() {
        if (bleAdvertiser != null) {
            try {
                if (hasBleAdvertisePermission()) {
                    bleAdvertiser.stopAdvertising(bleAdvertiseCallback);
                }
            } catch (SecurityException ignored) {
            }
            bleAdvertiser = null;
        }
        if (bleGattServer != null) {
            try {
                if (hasBleConnectPermission()) {
                    bleGattServer.close();
                }
            } catch (SecurityException ignored) {
            }
            bleGattServer = null;
        }
        bleServerMode = false;
        bleServerClient = null;
        bleServerAuthenticated = false;
        bleServerSessionId = null;
        bleServerCode = null;
        bleServerStatusValue = "OFF";
        runOnUiThread(() -> {
            if (bleServerStatusText != null) {
                bleServerStatusText.setText("BLE-сервис: выключен");
            }
            if (bleServerSessionText != null) {
                bleServerSessionText.setText("Сессия сервиса: не создана");
            }
        });
    }

    private void generateBleServerSession() {
        bleServerSessionId = UUID.randomUUID().toString().substring(0, 8);
        bleServerCode = String.format(Locale.US, "%06d",
                secureRandom.nextInt(1000000));
        bleServerSessionExpiresAt = System.currentTimeMillis() + 120000L;
        bleServerAuthenticated = false;
        bleServerStatusValue = "READY|" + bleServerSessionId + "|"
                + bleServerSessionExpiresAt;
        updateBleServerStatusCharacteristic();
        runOnUiThread(() -> {
            if (bleServerSessionText != null) {
                bleServerSessionText.setText("Сессия: " + bleServerSessionId
                        + " · код: " + bleServerCode + " · 120 сек (демо)");
            }
        });
        appendLog("BLE: новая сервисная сессия создана");
    }

    private String currentBleServerStatus() {
        if (bleServerStatusValue == null || bleServerStatusValue.isEmpty()) {
            return "OFF";
        }
        return bleServerStatusValue;
    }

    private void updateBleServerStatusCharacteristic() {
        if (bleServerStatusCharacteristic != null) {
            bleServerStatusCharacteristic.setValue(currentBleServerStatus().getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void sendBleServerResponse(BluetoothDevice device, int requestId, int status,
                                       int offset, byte[] value) {
        try {
            if (bleGattServer != null && hasBleConnectPermission()) {
                bleGattServer.sendResponse(device, requestId, status, offset, value);
            }
        } catch (SecurityException ignored) {
        }
    }

    private void handleBleServerAuth(BluetoothDevice device, String payload) {
        if (bleServerClient == null || !device.equals(bleServerClient)
                || bleServerCode == null || System.currentTimeMillis() > bleServerSessionExpiresAt) {
            bleServerStatusValue = "AUTH_FAIL|expired";
            updateBleServerStatusCharacteristic();
            return;
        }
        String code = payload.startsWith("AUTH|") ? payload.substring(5).trim() : "";
        if (code.equals(bleServerCode)) {
            bleServerAuthenticated = true;
            bleServerStatusValue = "AUTH_OK|" + bleServerSessionId;
            updateBleServerStatusCharacteristic();
            setBleServerStatus("клиент авторизован");
            appendLog("BLE: сервисный клиент авторизован");
        } else {
            bleServerStatusValue = "AUTH_FAIL|bad_code";
            updateBleServerStatusCharacteristic();
            setBleServerStatus("неверный код клиента");
            appendLog("BLE: неверный код сервисного клиента");
        }
    }

    private void handleBleServerCommand(BluetoothDevice device, String payload) {
        if (bleServerClient == null || !device.equals(bleServerClient)
                || !bleServerAuthenticated
                || System.currentTimeMillis() > bleServerSessionExpiresAt) {
            bleServerStatusValue = "ERROR|not_authorized";
            updateBleServerStatusCharacteristic();
            return;
        }
        if (!payload.startsWith("OPEN|")) {
            bleServerStatusValue = "ERROR|unsupported_command";
            updateBleServerStatusCharacteristic();
            return;
        }
        int index;
        try {
            index = Integer.parseInt(payload.substring(5).trim()) - 1;
        } catch (NumberFormatException error) {
            index = -1;
        }
        if (index < 0 || index >= 4) {
            bleServerStatusValue = "ERROR|invalid_cell";
            updateBleServerStatusCharacteristic();
            return;
        }
        final int cellIndex = index;
        ioExecutor.execute(() -> {
            boolean ok = writeRelayBlocking(cellIndex, true);
            bleServerStatusValue = ok
                    ? "OPEN|D" + (cellIndex + 1) + "|OK"
                    : "OPEN|D" + (cellIndex + 1) + "|ERROR_MODBUS";
            updateBleServerStatusCharacteristic();
            setBleServerStatus(ok
                    ? "команда выполнена · D" + (cellIndex + 1) + " open"
                    : "ошибка команды · Modbus недоступен");
            appendLog("BLE: сервисная команда OPEN D" + (cellIndex + 1)
                    + (ok ? " выполнена" : " отклонена"));
            if (ok) {
                sendEvent("door_opened", cellCodeForIndex(cellIndex), "bluetooth_service");
            }
        });
    }

    private void setBleServerStatus(String message) {
        runOnUiThread(() -> {
            if (bleServerStatusText != null) {
                bleServerStatusText.setText("BLE-сервис: " + message);
            }
            if (bluetoothNodeText != null) {
                boolean online = bleServerMode || bleServerClient != null;
                bluetoothNodeText.setText(online ? "Service online" : "Выключен");
                bluetoothNodeText.setTextColor(online ? COLOR_GREEN : COLOR_MUTED);
            }
        });
    }

    private void readBleClientStatus(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic statusCharacteristic) {
        try {
            if (hasBleConnectPermission()) {
                gatt.readCharacteristic(statusCharacteristic);
            }
        } catch (SecurityException ignored) {
        }
    }

    private void authenticateBleClient() {
        String code = bleClientCodeEdit == null
                ? "" : bleClientCodeEdit.getText().toString().trim();
        if (code.isEmpty() || bluetoothGatt == null) {
            setBleStatus("подключитесь и введите код");
            return;
        }
        BluetoothGattService service = bluetoothGatt.getService(BLE_SERVICE_UUID);
        BluetoothGattCharacteristic auth = service == null ? null
                : service.getCharacteristic(BLE_AUTH_UUID);
        if (auth == null) {
            setBleStatus("сервис авторизации не найден");
            return;
        }
        auth.setValue(("AUTH|" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            if (hasBleConnectPermission() && bluetoothGatt.writeCharacteristic(auth)) {
                setBleStatus("проверка одноразового кода...");
            } else {
                setBleStatus("не удалось отправить код");
            }
        } catch (SecurityException error) {
            setBleStatus("нет разрешения на запись BLE");
        }
    }

    private void sendBleClientCommand(int index) {
        if (bluetoothGatt == null) {
            setBleStatus("BLE-контроллер не подключён");
            return;
        }
        BluetoothGattService service = bluetoothGatt.getService(BLE_SERVICE_UUID);
        BluetoothGattCharacteristic command = service == null ? null
                : service.getCharacteristic(BLE_COMMAND_UUID);
        if (command == null) {
            setBleStatus("командный канал не найден");
            return;
        }
        command.setValue(("OPEN|" + (index + 1)).getBytes(
                java.nio.charset.StandardCharsets.UTF_8));
        try {
            if (hasBleConnectPermission() && bluetoothGatt.writeCharacteristic(command)) {
                setBleStatus("команда D" + (index + 1) + " отправлена");
            } else {
                setBleStatus("не удалось отправить команду");
            }
        } catch (SecurityException error) {
            setBleStatus("нет разрешения на запись BLE");
        }
    }

    private void handleBleClientStatus(String status) {
        if (status == null || status.isEmpty()) {
            return;
        }
        runOnUiThread(() -> {
            if (status.startsWith("AUTH_OK|")) {
                setBleStatus("сервисный клиент авторизован");
                showBleClientCommands(true);
            } else if (status.startsWith("AUTH_FAIL|")) {
                setBleStatus("код отклонён");
                showBleClientCommands(false);
            } else if (status.startsWith("OPEN|")) {
                setBleStatus("ответ сервиса: " + status.replace('|', ' '));
            } else if (status.startsWith("READY|")) {
                setBleStatus("подключено · ожидается одноразовый код");
            } else if (status.startsWith("ERROR|")) {
                setBleStatus("сервис отклонил команду");
            }
        });
        if (!status.startsWith("READY|")) {
            appendLog("BLE: статус сервиса получен · " + status.split("\\|")[0]);
        }
    }

    private void showBleClientAuth(boolean visible) {
        if (bleClientAuthContainer != null) {
            bleClientAuthContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void showBleClientCommands(boolean visible) {
        if (bleClientCommandContainer != null) {
            bleClientCommandContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void startBleScan() {
        if (bluetoothAdapter == null) {
            setBleStatus("BLE недоступен на этом телефоне");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && (!hasBleScanPermission() || !hasBleConnectPermission())) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLE_PERMISSION);
            return;
        }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_BLE_ENABLE);
                setBleStatus("включите Bluetooth и повторите поиск");
                return;
            }
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                setBleStatus("BLE-сканер недоступен");
                return;
            }
            bleDevices.clear();
            renderBleDevices();
            bleScanning = true;
            setBleStatus("идёт поиск BLE-устройств...");
            appendLog("BLE: поиск устройств начат");
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanner.startScan(null, settings, bleScanCallback);
            mainHandler.removeCallbacks(this::stopBleScan);
            mainHandler.postDelayed(this::stopBleScan, 10000);
        } catch (SecurityException error) {
            setBleStatus("нет разрешения Nearby devices");
            appendLog("BLE: нет разрешения на Bluetooth");
        }
    }

    private void stopBleScan() {
        if (!bleScanning) {
            return;
        }
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeScanner() != null
                    && hasBleScanPermission()) {
                bluetoothAdapter.getBluetoothLeScanner().stopScan(bleScanCallback);
            }
        } catch (SecurityException ignored) {
        }
        bleScanning = false;
        if (bleStatusText != null && bluetoothGatt == null) {
            setBleStatus(bleDevices.isEmpty()
                    ? "устройства не найдены"
                    : "поиск завершён · найдено: " + bleDevices.size());
        }
        appendLog("BLE: поиск завершён · найдено " + bleDevices.size());
    }

    private void renderBleDevices() {
        if (bleDevicesContainer == null) {
            return;
        }
        bleDevicesContainer.removeAllViews();
        if (bleDevices.isEmpty()) {
            TextView empty = text("Устройства ещё не найдены", 12);
            empty.setTextColor(COLOR_MUTED);
            bleDevicesContainer.addView(empty);
            return;
        }
        String preferredName = bleNameEdit == null
                ? "" : bleNameEdit.getText().toString().trim();
        for (BluetoothDevice device : bleDevices.values()) {
            String name = bleDeviceName(device);
            String marker = !preferredName.isEmpty() && name.contains(preferredName)
                    ? "★ " : "";
            Button deviceButton = actionButton(marker + name + "\n" + device.getAddress(),
                    COLOR_CARD, COLOR_TEXT);
            deviceButton.setBackground(outlinedRounded(COLOR_CARD, COLOR_BORDER, 9));
            deviceButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            deviceButton.setTextSize(12);
            deviceButton.setMinHeight(dp(58));
            deviceButton.setOnClickListener(v -> connectBle(device));
            bleDevicesContainer.addView(deviceButton, blockParams(6));
        }
    }

    private String bleDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.isEmpty() ? "BLE-устройство" : name;
        } catch (SecurityException ignored) {
            return "BLE-устройство";
        }
    }

    private void connectBle(BluetoothDevice device) {
        stopBleScan();
        disconnectBle();
        try {
            setBleStatus("подключение: " + bleDeviceName(device));
            appendLog("BLE: подключение к " + bleDeviceName(device));
            if (Build.VERSION.SDK_INT >= 23) {
                bluetoothGatt = device.connectGatt(this, false, bleGattCallback,
                        BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = device.connectGatt(this, false, bleGattCallback);
            }
        } catch (SecurityException error) {
            setBleStatus("нет разрешения на подключение");
            appendLog("BLE: подключение запрещено системой");
        }
    }

    private void generateBleSession() {
        bleSessionId = UUID.randomUUID().toString().substring(0, 8);
        bleSessionCode = String.format(Locale.US, "%06d",
                secureRandom.nextInt(1000000));
        runOnUiThread(() -> {
            if (bleSessionText != null) {
                bleSessionText.setText("Сессия: " + bleSessionId
                        + " · одноразовый код: " + bleSessionCode + " (демо)");
            }
        });
        appendLog("BLE: новая одноразовая сессия создана");
    }

    private void disconnectBle() {
        stopBleScan();
        if (bluetoothGatt != null) {
            try {
                if (hasBleConnectPermission()) {
                    bluetoothGatt.disconnect();
                }
                bluetoothGatt.close();
            } catch (SecurityException ignored) {
            }
            bluetoothGatt = null;
        }
        if (bleSessionText != null) {
            bleSessionText.setText("Сессия: не создана");
        }
        if (bleStatusText != null) {
            setBleStatus("не подключён");
        }
    }

    private void setBleStatus(String message) {
        runOnUiThread(() -> {
            if (bleStatusText != null) {
                bleStatusText.setText("BLE: " + message);
            }
            if (bleScanActionButton != null) {
                String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
                boolean scanning = lower.contains("поиск") || lower.contains("скан");
                bleScanActionButton.setEnabled(!scanning);
                bleScanActionButton.setText(scanning ? "Поиск..." : "Сканировать");
            }
            if (bluetoothNodeText != null) {
                boolean online = bluetoothGatt != null;
                bluetoothNodeText.setText(online ? "Connected" : "Выключен");
                bluetoothNodeText.setTextColor(online ? COLOR_GREEN : COLOR_MUTED);
            }
        });
    }

    private void connectBackend() {
        String url = websocketUrlEdit.getText().toString().trim();
        String token = tokenEdit.getText().toString().trim();
        if (url.isEmpty()) {
            setBackendStatus("URL не указан");
            return;
        }
        if (backendClient != null) {
            backendClient.shutdown();
        }
        backendClient = new BackendWebSocketClient(new BackendWebSocketClient.Listener() {
            @Override
            public void onOpen() {
                setBackendStatus("подключён");
                appendLog("Backend: соединение установлено");
                sendHello();
                startHeartbeat();
            }

            @Override
            public void onMessage(String message) {
                handleBackendMessage(message);
            }

            @Override
            public void onClosed(String reason) {
                setBackendStatus("соединение закрыто: " + reason);
                appendLog("Backend: соединение закрыто"
                        + (reason == null || reason.isEmpty() ? "" : " · " + reason));
            }

            @Override
            public void onError(String message) {
                setBackendStatus("ошибка: " + message);
                appendLog("Backend: ошибка · " + message);
            }
        }, insecureTlsCheck != null && insecureTlsCheck.isChecked());
        setBackendStatus("подключение...");
        backendClient.connect(url, token);
    }

    private void sendHello() {
        if (backendClient == null || !backendClient.isOpen()) {
            return;
        }
        try {
            JSONObject hello = new JSONObject();
            hello.put("type", "hello");
            hello.put("postamatId", postamatIdEdit.getText().toString().trim());
            hello.put("appVersion", "0.2.1");
            hello.put("keyVersion", 0);
            hello.put("capabilities", new JSONArray()
                    .put("locks")
                    .put("door_sensors")
                    .put("service_open")
                    .put("modbus_rtu"));
            hello.put("cells", cellsSnapshot());
            hello.put("net", new JSONObject().put("kind", "wifi"));
            sendBackend(hello);
        } catch (Exception e) {
            appendLog("Не удалось собрать hello: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            return;
        }
        heartbeatTask = ioExecutor.scheduleWithFixedDelay(() -> {
            if (backendClient == null || !backendClient.isOpen()) {
                return;
            }
            try {
                JSONObject heartbeat = new JSONObject();
                heartbeat.put("type", "heartbeat");
                heartbeat.put("cells", cellsSnapshot());
                heartbeat.put("net", new JSONObject().put("kind", "wifi"));
                heartbeat.put("problems", new JSONArray());
                sendBackend(heartbeat);
            } catch (Exception e) {
                appendLog("Не удалось собрать heartbeat: " + e.getMessage());
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    private JSONArray cellsSnapshot() throws Exception {
        JSONArray cells = new JSONArray();
        for (int i = 0; i < 4; i++) {
            JSONObject cell = new JSONObject();
            cell.put("code", cellCodeForIndex(i));
            cell.put("door", relayStates[i] ? "open" : "closed");
            cell.put("lock", "unknown");
            cells.put(cell);
        }
        return cells;
    }

    private void handleBackendMessage(String message) {
        try {
            JSONObject object = new JSONObject(message);
            String type = object.optString("type");
            if ("welcome".equals(type)) {
                String serverPostamatId = object.optString("postamatId", "");
                JSONArray serverCells = object.optJSONArray("cells");
                int serverCellCount = serverCells == null ? 0 : serverCells.length();
                applyBackendCellMetadata(serverCells);
                appendLog("Backend: welcome получен · "
                        + (serverPostamatId.isEmpty()
                        ? "postamat не указан"
                        : serverPostamatId)
                        + " · ячеек на сервере: " + serverCellCount);
                return;
            }
            if (!"command".equals(type)) {
                return;
            }
            appendLog("Backend: команда " + object.optString("kind", "unknown")
                    + " · " + displayCell(commandCellCode(object)));
            ioExecutor.execute(() -> {
                try {
                    handleCommand(object);
                } catch (Exception error) {
                    appendLog("Ошибка обработки команды: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            appendLog("Неверное сообщение backend: " + e.getMessage());
        }
    }

    private void applyBackendCellMetadata(JSONArray cells) {
        if (cells == null) {
            return;
        }
        for (int i = 0; i < cells.length(); i++) {
            JSONObject cell = cells.optJSONObject(i);
            if (cell == null) {
                continue;
            }
            String code = cell.optString("code", cell.optString("cellCode", ""));
            String digits = code.replaceAll("[^0-9]", "");
            int index;
            try {
                index = Integer.parseInt(digits) - 1;
            } catch (Exception ignored) {
                continue;
            }
            if (index < 0 || index >= cellBusinessStates.length) {
                continue;
            }
            String status = cell.optString("rentalStatus",
                    cell.optString("availability", cell.optString("status", "")));
            if (status.isEmpty()) {
                status = cellBusinessStates[index];
            }
            cellBusinessStates[index] = displayBusinessStatus(status);
            cellToolNames[index] = cell.optString("toolName",
                    cell.optString("instrument", cellToolNames[index]));
            cellToolUnits[index] = cell.optString("toolUnit",
                    cell.optString("toolUnitId", cellToolUnits[index]));
            cellRentalIds[index] = cell.optString("rentalId", cellRentalIds[index]);
            final int cellIndex = index;
            runOnUiThread(() -> {
                if (relayTexts[cellIndex] != null) {
                    relayTexts[cellIndex].setText(cellBusinessStates[cellIndex]);
                    relayTexts[cellIndex].setTextColor(
                            "Заблокирована".equals(cellBusinessStates[cellIndex])
                                    ? COLOR_RED : COLOR_TEXT);
                }
                refreshSummaryCounts();
            });
        }
    }

    private String displayBusinessStatus(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains("rent") || value.contains("аренд")) {
            return "В аренде";
        }
        if (value.contains("block") || value.contains("блок")) {
            return "Заблокирована";
        }
        if (value.contains("maint") || value.contains("service")
                || value.contains("обслуж")) {
            return "На обслуживании";
        }
        if (value.contains("attention") || value.contains("вниман")) {
            return "Требует внимания";
        }
        if (value.contains("available") || value.contains("доступ")) {
            return "Доступна";
        }
        return raw;
    }

    private void handleCommand(JSONObject command) throws Exception {
        String commandId = command.optString("id", "");
        String kind = command.optString("kind", "");
        if (isExpired(command.opt("expiresAt"))) {
            appendLog("Команда просрочена · " + displayCell(commandCellCode(command)));
            sendAck(commandId, false, new JSONObject().put("error", "expired"));
            return;
        }

        int index = cellIndex(command);
        String cellCode = cellCode(command, index);
        if ("open_cell".equals(kind)) {
            if (index < 0) {
                appendLog("Backend: неизвестная ячейка · открыть нельзя");
                sendAck(commandId, false, new JSONObject().put("error", "invalid_cell"));
                return;
            }
            boolean ok = writeRelayBlocking(index, true);
            if (ok) {
                appendLog(displayCell(cellCode) + " open");
                sendAck(commandId, true, new JSONObject()
                        .put("kind", "door_open_ack")
                        .put("cellCode", cellCode));
                sendEvent("door_opened", cellCode, "lock_relay");
            } else {
                appendLog(displayCell(cellCode) + " ошибка · Modbus недоступен");
                sendAck(commandId, false, new JSONObject().put("error", "modbus_offline"));
            }
            return;
        }

        if ("confirm_closed".equals(kind)) {
            if (index < 0) {
                appendLog("Backend: неизвестная ячейка · закрытие не подтверждено");
                sendAck(commandId, false, new JSONObject().put("error", "invalid_cell"));
            } else if (!relayStates[index]) {
                appendLog(displayCell(cellCode) + " closed");
                sendAck(commandId, true, new JSONObject()
                        .put("kind", "door_close_ack")
                        .put("cellCode", cellCode));
            } else {
                appendLog(displayCell(cellCode) + " ошибка · дверь не закрыта");
                sendAck(commandId, false, new JSONObject().put("error", "door_not_closed"));
            }
            return;
        }

        if ("cell_report".equals(kind)) {
            sendAck(commandId, true, new JSONObject().put("cells", cellsSnapshot()));
            return;
        }

        appendLog("Backend: неподдерживаемая команда · " + kind);
        sendAck(commandId, false, new JSONObject().put("error", "unsupported_command")
                .put("kind", kind));
    }

    private String commandCellCode(JSONObject command) {
        int index = cellIndex(command);
        if (index >= 0) {
            return cellCode(command, index);
        }
        Object payload = command.opt("payload");
        if (payload instanceof JSONObject) {
            String code = ((JSONObject) payload).optString("cellCode", "");
            if (!code.isEmpty()) {
                return code;
            }
        }
        String code = command.optString("cellCode", "");
        return code.isEmpty() ? "?" : code;
    }

    private String displayCell(String code) {
        if (code == null || code.trim().isEmpty() || "?".equals(code)) {
            return "D?";
        }
        String digits = code.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "D" + code;
        }
        try {
            return "D" + Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return "D" + code;
        }
    }

    private int cellIndex(JSONObject command) {
        Object payload = command.opt("payload");
        String value = "";
        if (payload instanceof JSONObject) {
            JSONObject body = (JSONObject) payload;
            value = body.optString("cellCode", body.optString("cellId", ""));
        } else if (payload instanceof String) {
            value = (String) payload;
        }
        if (value.isEmpty()) {
            value = command.optString("cellCode", command.optString("cellId", ""));
        }
        String digits = value.replaceAll("[^0-9]", "");
        try {
            int number = Integer.parseInt(digits);
            if (number >= 1 && number <= 4) {
                return number - 1;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private String cellCode(JSONObject command, int index) {
        Object payload = command.opt("payload");
        if (payload instanceof JSONObject) {
            String code = ((JSONObject) payload).optString("cellCode", "");
            if (!code.isEmpty()) {
                return code;
            }
        }
        String code = command.optString("cellCode", "");
        return code.isEmpty() ? cellCodeForIndex(index) : code;
    }

    private String cellCodeForIndex(int index) {
        return String.format(Locale.US, "%02d", index + 1);
    }

    private boolean isExpired(Object expiresAt) {
        if (expiresAt == null || expiresAt == JSONObject.NULL) {
            return false;
        }
        try {
            if (expiresAt instanceof Number) {
                long value = ((Number) expiresAt).longValue();
                if (value < 100000000000L) {
                    value *= 1000L;
                }
                return value < System.currentTimeMillis();
            }
            String value = String.valueOf(expiresAt);
            try {
                long numeric = Long.parseLong(value);
                if (numeric < 100000000000L) {
                    numeric *= 1000L;
                }
                return numeric < System.currentTimeMillis();
            } catch (NumberFormatException ignored) {
                String[] formats = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"};
                for (String format : formats) {
                    try {
                        SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
                        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date date = parser.parse(value);
                        return date != null && date.getTime() < System.currentTimeMillis();
                    } catch (Exception ignoredFormat) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void sendAck(String commandId, boolean ok, JSONObject result) {
        try {
            JSONObject ack = new JSONObject();
            ack.put("type", "ack");
            ack.put("commandId", commandId);
            ack.put("ok", ok);
            ack.put("result", result);
            sendBackend(ack);
        } catch (Exception e) {
            appendLog("Не удалось отправить ack: " + e.getMessage());
        }
    }

    private void sendEvent(String kind, String cellCode, String source) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "event");
            event.put("kind", kind);
            event.put("cellCode", cellCode);
            event.put("detail", new JSONObject()
                    .put("source", source));
            sendBackend(event);
        } catch (Exception e) {
            appendLog("Не удалось отправить event: " + e.getMessage());
        }
    }

    private void sendBackend(JSONObject message) {
        if (backendClient != null && backendClient.isOpen()) {
            backendClient.send(message.toString());
            String type = message.optString("type", "");
            if ("hello".equals(type)) {
                appendLog("Backend: hello отправлен");
            } else if ("heartbeat".equals(type)) {
                appendLog("Связь: heartbeat");
            } else if (!"ack".equals(type) && !"event".equals(type)) {
                appendLog("Backend: сообщение отправлено · " + type);
            }
        }
    }

    private void setBackendStatus(String message) {
        runOnUiThread(() -> {
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (backendText != null) {
                boolean online = backendClient != null && backendClient.isOpen();
                backendText.setText(online ? "Online" : "Offline");
                backendText.setTextColor(online ? COLOR_GREEN : COLOR_RED);
            }
            if (backendActionButton != null) {
                boolean online = backendClient != null && backendClient.isOpen();
                boolean connecting = !online && lower.contains("подключение");
                backendActionButton.setEnabled(!connecting);
                backendActionButton.setText(online
                        ? "Переподключить backend"
                        : connecting ? "Подключение..." : "Подключить backend");
            }
            updateOverallStatus();
        });
    }

    private void updateOverallStatus() {
        if (overallStatusText == null) {
            return;
        }
        boolean backendOnline = backendClient != null && backendClient.isOpen();
        boolean controllerOnline = modbus != null;
        if (backendOnline && controllerOnline) {
            overallStatusText.setText("● Система работает");
            overallStatusText.setTextColor(COLOR_GREEN);
            overallStatusText.setBackground(outlinedRounded(
                    Color.rgb(15, 35, 30), Color.rgb(29, 92, 71), 9));
        } else if (backendOnline || controllerOnline) {
            overallStatusText.setText("● Работает с ограничениями");
            overallStatusText.setTextColor(COLOR_AMBER);
            overallStatusText.setBackground(outlinedRounded(
                    Color.rgb(28, 25, 18), Color.rgb(104, 76, 24), 9));
        } else {
            overallStatusText.setText("● Нет подключения");
            overallStatusText.setTextColor(COLOR_RED);
            overallStatusText.setBackground(outlinedRounded(
                    Color.rgb(38, 20, 25), Color.rgb(104, 37, 50), 9));
        }
    }

    private void findAndConnect() {
        if (modbus != null || (pollingTask != null && !pollingTask.isDone()
                && !pollingTask.isCancelled())) {
            setConnection("Уже подключено");
            appendLog("Modbus: соединение уже открыто");
            return;
        }
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()) {
            setConnection("USB-Serial устройство не найдено");
            return;
        }
        // The PLC can expose several serial USB devices (for example, a modem and
        // the RS-485 adapter). Prefer the common Prolific PL2303 adapter when it
        // is present; otherwise fall back to the first supported serial device.
        pendingDriver = drivers.get(0);
        for (UsbSerialDriver candidate : drivers) {
            if (candidate.getDevice().getVendorId() == 0x067B) {
                pendingDriver = candidate;
                break;
            }
        }
        UsbDevice device = pendingDriver.getDevice();
        if (!usbManager.hasPermission(device)) {
            setConnection("Ожидание USB-разрешения");
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
            usbManager.requestPermission(device, permissionIntent);
        } else {
            setConnection("Подключение к контроллеру");
            openDriver(pendingDriver);
        }
    }

    private void openDriver(UsbSerialDriver driver) {
        ioExecutor.execute(() -> {
            try {
                serialPort = driver.getPorts().get(0);
                serialPort.open(usbManager.openDevice(driver.getDevice()));
                int baud = Integer.parseInt(baudSpinner.getSelectedItem().toString());
                serialPort.setParameters(baud, 8, UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE);
                serialPort.setDTR(true);
                serialPort.setRTS(true);
                modbus = new ModbusRtuClient(serialPort);
                setConnection("Подключено: " + driver.getClass().getSimpleName()
                        + " VID=" + Integer.toHexString(driver.getDevice().getVendorId())
                        + " PID=" + Integer.toHexString(driver.getDevice().getProductId()));
                appendLog("RS-485 открыт, Modbus RTU " + baud + " 8N1");
                inputsInitialized = false;
                modbusErrorStreak = 0;
                startPolling();
            } catch (Exception e) {
                setConnection("Ошибка подключения: " + e.getMessage());
                closePort();
            }
        });
    }

    private void startPolling() {
        if (pollingTask != null && !pollingTask.isDone() && !pollingTask.isCancelled()) {
            return;
        }
        pollingTask = ioExecutor.scheduleWithFixedDelay(() -> {
            if (modbus == null) {
                return;
            }
            try {
                int slave = number(slaveIdEdit, 1);
                int inputStart = number(inputStartEdit, 0);
                ModbusRtuClient client = modbus;
                boolean[] inputs;
                synchronized (modbusLock) {
                    if (modbus != client) {
                        return;
                    }
                    inputs = client.readDiscreteInputs(slave, inputStart, 4);
                }
                if (modbusErrorStreak > 0) {
                    appendLog("Modbus: связь восстановлена");
                    modbusErrorStreak = 0;
                    lastModbusError = null;
                }
                for (int i = 0; i < inputs.length; i++) {
                    final boolean value = inputs[i];
                    if (inputsInitialized && value && !previousInputs[i]) {
                        if (writeRelayBlocking(i, false)) {
                            appendLog(displayCell(cellCodeForIndex(i)) + " closed");
                            sendEvent("door_closed", cellCodeForIndex(i), "door_sensor");
                        } else {
                            appendLog(displayCell(cellCodeForIndex(i))
                                    + " ошибка · не удалось подтвердить закрытие");
                        }
                    }
                    previousInputs[i] = value;
                }
                inputsInitialized = true;
            } catch (Exception e) {
                modbusErrorStreak++;
                logModbusError("Ошибка чтения X", e);
                if (modbusErrorStreak >= 5 && modbus != null) {
                    setConnection("Ошибка связи с контроллером");
                    appendLog("Modbus: опрос остановлен · подключите заново");
                    closePort();
                }
            }
        }, 0, 250, TimeUnit.MILLISECONDS);
    }

    private void setRelay(int index, boolean enabled) {
        ioExecutor.execute(() -> writeRelay(index, enabled));
    }

    private void writeRelay(int index, boolean enabled) {
        writeRelayBlocking(index, enabled);
    }

    private boolean writeRelayBlocking(int index, boolean enabled) {
        if (modbus == null) {
            appendLog("Нет подключения к Modbus");
            return false;
        }
        try {
            int slave = number(slaveIdEdit, 1);
            int outputStart = number(outputStartEdit, 0);
            ModbusRtuClient client = modbus;
            synchronized (modbusLock) {
                if (modbus != client) {
                    return false;
                }
                client.writeSingleCoil(slave, outputStart + index, enabled);
            }
            relayStates[index] = enabled;
            String stamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            if (enabled) {
                lastOpenedAt[index] = stamp;
                openedAtMillis[index] = System.currentTimeMillis();
            } else {
                lastClosedAt[index] = stamp;
                openedAtMillis[index] = 0L;
            }
            runOnUiThread(() -> {
                if (inputTexts[index] != null) {
                    inputTexts[index].setText(formatDoorState(index));
                    inputTexts[index].setTextColor(enabled ? COLOR_AMBER : COLOR_GREEN);
                }
                refreshSummaryCounts();
            });
            return true;
        } catch (Exception e) {
            logModbusError("Ошибка Y" + (index + 1), e);
            return false;
        }
    }

    private void logModbusError(String prefix, Exception error) {
        String detail = error.getMessage();
        String message = prefix + ": "
                + (detail == null || detail.isEmpty()
                ? error.getClass().getSimpleName() : detail);
        long now = System.currentTimeMillis();
        if (!message.equals(lastModbusError) || now - lastModbusErrorAt >= 5000) {
            appendLog(message);
            lastModbusError = message;
            lastModbusErrorAt = now;
        }
    }

    private int number(EditText edit, int fallback) {
        try {
            return Integer.parseInt(edit.getText().toString().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void setConnection(String message) {
        runOnUiThread(() -> {
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            boolean online = modbus != null
                    && !lower.contains("ошиб") && !lower.contains("не подключ")
                    && !lower.contains("не найден");
            if (connectionText != null) {
                connectionText.setText(online ? "Online" : "Offline");
                connectionText.setTextColor(online ? COLOR_GREEN : COLOR_RED);
            }
            if (modbusActionButton != null) {
                boolean waiting = lower.contains("разрешен") || lower.contains("подключение");
                modbusActionButton.setEnabled(!waiting || online);
                modbusActionButton.setText(online
                        ? "Контроллер подключён"
                        : waiting ? "Ожидание USB..." : "Подключить USB · RS-485");
            }
            updateOverallStatus();
        });
    }

    private void appendLog(String message) {
        String entry = "[" + new SimpleDateFormat("HH:mm:ss", Locale.US)
                .format(new Date()) + "] " + message;
        String updated;
        synchronized (LOG_BUFFER) {
            LOG_BUFFER.append("\n").append(entry);
            updated = LOG_BUFFER.toString();
        }
        runOnUiThread(() -> {
            if (logText != null) {
                logText.setText(updated);
            }
        });
        LogActivity.notifyLogUpdated();
    }

    static String getLogText() {
        synchronized (LOG_BUFFER) {
            return LOG_BUFFER.toString();
        }
    }

    static void clearLog() {
        synchronized (LOG_BUFFER) {
            LOG_BUFFER.setLength(0);
            LOG_BUFFER.append("Журнал очищен.");
        }
        LogActivity.notifyLogUpdated();
    }

    private void closePort() {
        ScheduledFuture<?> task = pollingTask;
        pollingTask = null;
        if (task != null) {
            task.cancel(false);
        }
        synchronized (modbusLock) {
            if (serialPort != null) {
                try {
                    serialPort.close();
                } catch (IOException ignored) {
                }
            }
            serialPort = null;
            modbus = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (settingsScreen != null && settingsScreen.getVisibility() == View.VISIBLE) {
            showMainScreen();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        stopBleServer();
        disconnectBle();
        mainHandler.removeCallbacksAndMessages(null);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }
        if (backendClient != null) {
            backendClient.shutdown();
        }
        closePort();
        ioExecutor.shutdownNow();
        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }
}
