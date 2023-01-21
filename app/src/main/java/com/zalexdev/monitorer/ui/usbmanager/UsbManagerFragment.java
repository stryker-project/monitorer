package com.zalexdev.monitorer.ui.usbmanager;

import static com.zalexdev.monitorer.SuUtils.customCommand;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zalexdev.monitorer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UsbManagerFragment extends Fragment {

    public Context context;
    public Activity activity;
    private View view;
    public Timer usbCheck = new Timer();
    public boolean connected = false;
    public SQLiteDatabase dbСodename;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_usb, container, false);
        context = getContext();
        activity = getActivity();
        startUsbObserve();
        deviceDisconnected();
        // It hides the badge on the USB icon in the bottom navigation bar.
        BottomNavigationView navView = activity.findViewById(R.id.nav_view);
        BadgeDrawable badge = navView.getOrCreateBadge(R.id.navigation_usb);
        badge.setVisible(false);
        return view;
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

    /**
     * Every 300 milliseconds, check if the device is connected, and if it is, call deviceConnected().
     */
    public void startUsbObserve(){
        usbCheck = new Timer();
        usbCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!getPid().equals("Unknown") && !connected){
                    connected = true;
                    activity.runOnUiThread(() -> deviceConnected());
                }else if (getPid().equals("Unknown") && connected){
                    connected = false;
                    activity.runOnUiThread(() -> deviceDisconnected());
                }
            }
        },0,300);
    }

    /**
     * It checks if the driver is installed for the device.
     */
    public void deviceConnected(){
        String device = getDeviceByCodeNameFromDB(getPid().split(":")[0],getPid().split(":")[1]);
        BottomNavigationView navView = activity.findViewById(R.id.nav_view);
        BadgeDrawable badge = navView.getOrCreateBadge(R.id.navigation_usb);
        badge.setVisible(false);
        LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottie_usb);
        lottieAnimationView.setAnimation(R.raw.usb_done);
        lottieAnimationView.loop(false);
        lottieAnimationView.setMinFrame(15);
        lottieAnimationView.playAnimation();
        TextView driver  = view.findViewById(R.id.usb_driver);
        Matcher matcher = Pattern.compile("\\[.*\\]").matcher(device);
        final String[] driverAdapter = {""};
        if (matcher.find()){
            driverAdapter[0] = matcher.group();
        }
        driver.setVisibility(View.VISIBLE);
        TextView waiting = view.findViewById(R.id.usb_waiting);
        waiting.setText(device);
        waiting.setVisibility(View.VISIBLE);
        String finalDriverAdapter = driverAdapter[0];
        // It checks if the driver is installed for the device.
        new Thread(() -> {
            boolean isDriverInstalled = false;
            ArrayList<String> output = customCommand("ip link show | grep wlan");
            ArrayList<String> inf = new ArrayList<>();
            for (String s : output){
                if (s.contains("wlan")){
                    inf.add(s.split(":")[1].trim());
                }
            }
            for (String iface : inf) {

                    ArrayList<String> temp = customCommand("cat /sys/class/net/" + iface + "/device/uevent");
                    Log.d("USB", temp + iface);
                    for (String s : temp) {
                        if (s.contains("PRODUCT=" + getPid().split(":")[0] + "/" + getPid().split(":")[1])) {
                            isDriverInstalled = true;
                        }else if(s.contains("PRODUCT=" + getPid().split(":")[0])){
                            isDriverInstalled = true;
                        }
                        if (s.contains("DRIVER=")){
                            driverAdapter[0] = s.replace("DRIVER=","").trim().toUpperCase(Locale.ROOT);
                        }
                    }
                    if (isDriverInstalled){
                        break;
                    }


            }
            if (isDriverInstalled) {
                activity.runOnUiThread(() -> {
                    driver.setText("Driver found: "+driverAdapter[0]);
                    driver.setTextColor(Color.parseColor("#689F38"));
                });
            }else{
                activity.runOnUiThread(() -> {
                    driver.setText("Driver not found");
                    driver.setTextColor(Color.parseColor("#D32F2F"));
                });
            }

        }).start();


    }

    /**
     * It sets the animation to the waiting animation and sets the text to "Waiting for USB Device"
     */
    public void deviceDisconnected(){
        LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottie_usb);
        lottieAnimationView.setAnimation(R.raw.usb_wait);
        lottieAnimationView.loop(true);
        lottieAnimationView.setMinFrame(15);
        lottieAnimationView.playAnimation();
        TextView driver  = view.findViewById(R.id.usb_driver);
        driver.setVisibility(View.GONE);
        TextView waiting = view.findViewById(R.id.usb_waiting);
        waiting.setText("Waiting for USB Device");
        waiting.setVisibility(View.VISIBLE);
    }

    /**
     * It returns the name of the device by its VID and PID.
     *
     * @param vid Vendor ID
     * @param pid the product ID of the device
     * @return The name of the device.
     */
    public String getDeviceByCodeNameFromDB(String vid, String pid){
        String model = "Unknown";
        try {
            if (dbСodename == null || !dbСodename.isOpen()){
                dbСodename = SQLiteDatabase.openDatabase("/data/data/com.zalexdev.monitorer/files/adapters.db", null, SQLiteDatabase.OPEN_READONLY);
            }
            Cursor cursor = dbСodename.rawQuery("SELECT product_name FROM data WHERE vendor_id = '"+vid+"' AND product_id='"+pid+"';", null);

            if (cursor.moveToFirst()) {
                model = cursor.getString(0);
            }
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }




}