package kz.arendo.device;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kz.arendo.device.modbus.ModbusRtuClient;
import kz.arendo.device.modbus.UsbSerialTransport;

/** Foundation UI for the Android gateway installed in the postamat. */
public final class MainActivity extends Activity {
    private static final String USB_PERMISSION = "kz.arendo.device.USB_PERMISSION";
    private static final int CELL_COUNT = 4;
    private static final int SLAVE_ID = 1;
    private static final int INPUT_START = 0;
    private static final int OUTPUT_START = 0;
    private static final int BAUD_RATE = 9600;

    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final boolean[] previousInputs = new boolean[CELL_COUNT];
    private final boolean[] outputActive = new boolean[CELL_COUNT];
    private final TextView[] cellStates = new TextView[CELL_COUNT];
    private final Button[] cellButtons = new Button[CELL_COUNT];

    private UsbManager usbManager;
    private UsbSerialDriver pendingDriver;
    private UsbSerialPort serialPort;
    private ModbusRtuClient modbus;
    private ScheduledFuture<?> polling;
    private boolean inputsInitialized;
    private TextView usbStatus;
    private TextView backendStatus;
    private TextView logView;

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
        registerUsbReceiver();
        setContentView(buildUi());
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
        backendStatus = label("Backend: будет подключён на следующем этапе", 14,
                Color.rgb(100, 116, 139));
        statusCard.addView(usbStatus);
        statusCard.addView(backendStatus);
        Button connect = button("Подключить USB-RS485", true);
        connect.setOnClickListener(view -> findAndConnect());
        statusCard.addView(connect, matchWrap(8));
        root.addView(statusCard, matchWrap(0));

        root.addView(sectionTitle("ЯЧЕЙКИ · ДИАГНОСТИКА СТЕНДА"), matchWrap(22));
        root.addView(label("X1–X4 — обратный сигнал, Y1–Y4 — команда на замок/LED.",
                13, Color.rgb(100, 116, 139)), matchWrap(0));

        for (int index = 0; index < CELL_COUNT; index++) {
            final int cell = index;
            LinearLayout card = column(8);
            card.setPadding(dp(18), dp(14), dp(18), dp(14));
            card.setBackgroundColor(Color.WHITE);
            TextView cellTitle = label(String.format(Locale.ROOT, "Ячейка %02d", index + 1),
                    17, Color.rgb(15, 23, 42));
            cellTitle.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(cellTitle);
            cellStates[index] = label("Состояние: неизвестно", 14, Color.rgb(100, 116, 139));
            card.addView(cellStates[index]);
            cellButtons[index] = button("Тест: включить Y" + (index + 1), false);
            cellButtons[index].setEnabled(false);
            cellButtons[index].setOnClickListener(view -> toggleOutput(cell));
            card.addView(cellButtons[index], matchWrap(4));
            root.addView(card, matchWrap(10));
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
                inputsInitialized = false;
                runOnUiThread(() -> {
                    setUsbStatus("Modbus RTU подключён · 9600 8N1", true);
                    setButtonsEnabled(true);
                    appendLog("RS-485 открыт, запущен опрос X1–X4");
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
            ModbusRtuClient client = modbus;
            if (client == null) {
                return;
            }
            try {
                boolean[] inputs = client.readDiscreteInputs(SLAVE_ID, INPUT_START, CELL_COUNT);
                for (int index = 0; index < CELL_COUNT; index++) {
                    boolean activated = inputs[index];
                    if (inputsInitialized && activated && !previousInputs[index]) {
                        client.writeSingleCoil(SLAVE_ID, OUTPUT_START + index, false);
                        outputActive[index] = false;
                        final int cell = index;
                        runOnUiThread(() -> {
                            updateCell(cell, "closed");
                            appendLog("D" + (cell + 1) + " closed");
                        });
                    }
                    previousInputs[index] = activated;
                }
                inputsInitialized = true;
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setUsbStatus("Ошибка Modbus: " + safeMessage(error), false);
                    appendLog("Modbus: " + safeMessage(error));
                });
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
    }

    private void toggleOutput(int index) {
        boolean next = !outputActive[index];
        io.execute(() -> {
            try {
                ModbusRtuClient client = modbus;
                if (client == null) {
                    throw new IOException("Modbus не подключён");
                }
                client.writeSingleCoil(SLAVE_ID, OUTPUT_START + index, next);
                outputActive[index] = next;
                runOnUiThread(() -> {
                    updateCell(index, next ? "open" : "closed");
                    appendLog("D" + (index + 1) + (next ? " open" : " closed"));
                });
            } catch (Exception error) {
                runOnUiThread(() -> appendLog("Y" + (index + 1) + ": "
                        + safeMessage(error)));
            }
        });
    }

    private void updateCell(int index, String state) {
        boolean open = "open".equals(state);
        cellStates[index].setText("Дверь: " + state + " · X" + (index + 1)
                + " / Y" + (index + 1));
        cellStates[index].setTextColor(open ? Color.rgb(22, 163, 74) : Color.rgb(71, 85, 105));
        cellButtons[index].setText((open ? "Выключить Y" : "Тест: включить Y") + (index + 1));
    }

    private void setUsbStatus(String value, boolean online) {
        runOnUiThread(() -> {
            usbStatus.setText("Modbus: " + value);
            usbStatus.setTextColor(online ? Color.rgb(22, 163, 74) : Color.rgb(185, 28, 28));
        });
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
        closePort();
        unregisterReceiver(usbPermissionReceiver);
        io.shutdownNow();
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
