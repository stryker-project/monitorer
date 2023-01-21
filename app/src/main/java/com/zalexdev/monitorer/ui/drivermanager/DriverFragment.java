package com.zalexdev.monitorer.ui.drivermanager;

import static com.zalexdev.monitorer.SuUtils.contains;
import static com.zalexdev.monitorer.SuUtils.customCommand;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.zalexdev.monitorer.ModulesAdapter;
import com.zalexdev.monitorer.Prefs;
import com.zalexdev.monitorer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class DriverFragment extends Fragment {

    public boolean isAndroidOk;
    public boolean isRootOk;
    public boolean isMonModeAvailable;

    public boolean isModulesAvailable;
    public boolean isOtgOk;
    public ArrayList<String> dList;
    public int status = 0;
    public String not = "❌";
    public Context context;
    public Activity activity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_driver, container, false);
        context = getContext();
        activity = getActivity();
        TextView title2 = view.findViewById(R.id.title2);
        LottieAnimationView anim = view.findViewById(R.id.animIcon);
        TextView title = view.findViewById(R.id.title);

        TextView android = view.findViewById(R.id.isAndroidOk);
        TextView root = view.findViewById(R.id.isRootOk);
        TextView monMode = view.findViewById(R.id.isMonitModeOk);

        TextView modules = view.findViewById(R.id.isModulesOk);

        TextView driverListText = view.findViewById(R.id.driverList);
        TextView usbOtg = view.findViewById(R.id.isUsbOtgOk);
        new Thread(() -> {
            // Checking if the device is rooted, if the device is running Android 6.0 or higher, if the
            // device has a monitor mode, if the device has the modules, and if the device has OTG.
            startCheck();
            activity.runOnUiThread(() -> {
                setChecked(android,isAndroidOk);
                setChecked(root,isRootOk);
                setChecked(monMode,isMonModeAvailable);
                setChecked(modules,isModulesAvailable);
                setChecked(usbOtg,isOtgOk);
                anim.setAnimation(R.raw.comp);
                anim.playAnimation();
                if (isAndroidOk){
                    if (isRootOk){
                        if (!isMonModeAvailable){status = 1;}
                        if (isOtgOk){
                            if (!isModulesAvailable){ status = 1; }}
                        else{
                            if (!isMonModeAvailable){status = 2;}
                            else{status = 1;} }
                    }else{ status = 2;}
                }else{
                    status = 2;
                }
                if (status == 1){
                    anim.setAnimation(R.raw.warns);
                    anim.playAnimation();
                    title.setText(R.string.warn);

                }else if (status == 2){
                    anim.setAnimation(R.raw.notcomp);
                    anim.playAnimation();
                    title.setText(R.string.not);
                }
                if (isModulesAvailable && !dList.isEmpty()){
                    StringBuilder list = new StringBuilder();
                    for (String driver: dList){
                        list.append(driver).append("\n");
                    }
                    driverListText.setText(list.toString());
                }else{
                    title2.setVisibility(View.GONE);
                    driverListText.setVisibility(View.GONE);
                }


            });
        }).start();
        // Setting up the recycler view and the checkbox.
        RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        Prefs prefs = new Prefs(context);
        CheckBox checkBox = view.findViewById(R.id.boot);
        checkBox.setChecked(new Prefs(context).getBoolean("boot"));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.putBoolean("boot", isChecked);
        });

        if (prefs.getBoolean("noroot")){
            // A dialog box that is shown when the app is not rooted.
            final Dialog rootDialog = new Dialog(context);
            rootDialog.setContentView(R.layout.root_dialog);
            rootDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            rootDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rootDialog.show();
        }

        // Getting the driver list and setting up the recycler view.
        new Thread(() -> {
            ArrayList<String> pathList = customCommand("ls /sys/bus/usb/drivers");
            ArrayList<String> driverList1 = customCommand("ls /system/lib/modules/");
            if (!pathList.isEmpty() && !contains(pathList,"not rooted") && !contains(pathList,"no such") && !contains(pathList,"Permission")){
                Log.e("DriverFragment", pathList + " " + driverList1);

            ModulesAdapter adapter = new ModulesAdapter(context,activity,driverList1,pathList);
            activity.runOnUiThread(() -> {
                recyclerView.setAdapter(adapter);
                driverListText.setText("");
                for (String s : pathList){
                    driverListText.append(s + "\n");
                }
            });}
            else{
                activity.runOnUiThread(() -> {
                    title2.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.GONE);
                    checkBox.setVisibility(View.GONE);
                    driverListText.setText("");
                    for (String s : pathList){
                        driverListText.append(s + "\n");
                    }
                    if (pathList.isEmpty()){
                        driverListText.setText(R.string.not);
                    }
                    driverListText.setVisibility(View.VISIBLE);

                });

            }
        }).start();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * If the boolean b is false, then set the text of the TextView text to the string not
     *
     * @param text The TextView you want to change
     * @param b boolean value
     */
    public void setChecked(TextView text, boolean b){if (!b){text.setText(not);}}

    /**
     * It checks if the device is rooted, if it has OTG support, if it has Android 6.0 or higher, if it
     * has monitor mode support, if it has modules support, and if it has drivers.
     */
    public void startCheck(){
        isOtgOk = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        isAndroidOk = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        isRootOk = execCmdBoolean("");
        isMonModeAvailable = execCmdBoolean("ls /sys/module/wlan/parameters/con_mode");
        isModulesAvailable = execCmdBoolean("ls /proc/modules");
        dList = getDriverList();
    }

    /**
     * Runs the command 'ls /sys/bus/usb/drivers' as root and returns the output as an ArrayList of
     * Strings.
     *
     * @return A list of drivers
     */
    private ArrayList<String> getDriverList(){
        ArrayList<String> out = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("su -mm");
            String line;
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
            stdin.write(("ls /sys/bus/usb/drivers" + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) { out.add(line); }
            br.close();
            process.waitFor();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * It executes a command and returns true if the command was successful
     *
     * @param cmd The command you want to execute.
     * @return The return value of the command.
     */
    private boolean execCmdBoolean(String cmd){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su -mm");
            OutputStream stdin = process.getOutputStream();
            stdin.write((cmd + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            process.waitFor();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        try{
        if (process != null) {
            return process.exitValue() == 0;
        }else{
            return false;
        }}
        catch (Exception e){
            return false;
        }
    }
}