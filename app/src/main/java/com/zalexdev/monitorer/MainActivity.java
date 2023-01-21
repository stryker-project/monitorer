package com.zalexdev.monitorer;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {




    public Context context;
    public Activity activity;
    public Timer usbCheck = new Timer();
    public boolean connected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_wlan, R.id.navigation_usb, R.id.navigation_driver, R.id.navigation_about)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        context = this;
        activity = this;
        Prefs prefs = new Prefs(context);

        // Checking if the file `/data/data/com.zalexdev.monitorer/files/adapters.db` exists. If it
        // doesn't, it creates the directory `/data/data/com.zalexdev.monitorer/files` and copies the
        // file `adapters.db` from the assets folder to the directory. It then sets the permissions of
        // the file `/data/data/com.zalexdev.monitorer/files/iw` to 777.
        new Thread(() -> {
            File file = new File("/data/data/com.zalexdev.monitorer/files/adapters.db");
            File dir = new File("/data/data/com.zalexdev.monitorer/files");
            if(!file.exists()){
                Log.e("File", "File not found");
                dir.mkdirs();
                copyAssets();

            }
            SuUtils.customCommand("chmod 777 /data/data/com.zalexdev.monitorer/files/iw");
            if (!SuUtils.checkRoot() && !prefs.getBoolean("root")){
                prefs.putBoolean("root", true);
                prefs.putBoolean("noroot", true);
            }else if (SuUtils.checkRoot()){
                prefs.putBoolean("noroot", false);
            }
        }).start();

        // Checking if the device is connected to the usb.
        startUsbObserve();



    }


    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        assert files != null;
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);

                String outDir = "/data/data/com.zalexdev.monitorer/files/" ;

                File outFile = new File(outDir, filename);

                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }


    /**
     * Copy the contents of the input stream to the output stream.
     *
     * @param in The input stream to read from.
     * @param out The output stream to write the file to.
     */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
    /**
     * It checks if the usb is connected or not.
     */
    public void startUsbObserve(){
        usbCheck = new Timer();
        usbCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!getPid().equals("Unknown") && !connected){
                    connected = true;
                    activity.runOnUiThread(() -> {
                        BottomNavigationView navView = findViewById(R.id.nav_view);
                        BadgeDrawable badge = navView.getOrCreateBadge(R.id.navigation_usb);
                        badge.setVisible(true);
                        Toast.makeText(context, "Detected usb", Toast.LENGTH_SHORT).show();
                    });
                }else if (getPid().equals("Unknown") && connected){
                    connected = false;
                    activity.runOnUiThread(() -> {
                        BottomNavigationView navView = findViewById(R.id.nav_view);
                        BadgeDrawable badge = navView.getOrCreateBadge(R.id.navigation_usb);
                        badge.setVisible(false);
                    });
                }
            }
        },0,300);
    }
    /**
     * It returns the USB device ID of the device.
     *
     * @return The vendor and product ID of the USB device.
     */
    public String getPid() {
        String deviceId = "Unknown";
        try {
            UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> devices = manager.getDeviceList();
            for (String deviceName : devices.keySet()) {
                UsbDevice device = devices.get(deviceName);
                assert device != null;
                StringBuilder string2 = new StringBuilder(Integer.toHexString(device.getVendorId()));
                while (string2.length() < 4) {
                    string2.insert(0, "0");
                }
                StringBuilder string3 = new StringBuilder(Integer.toHexString(device.getProductId()));
                while (string3.length() < 4) {
                    string3.insert(0, "0");
                }
                deviceId = string2 + ":" + string3;
            }
            return deviceId;
        } catch (Exception e) {
            return "Unknown";


        }
    }

}