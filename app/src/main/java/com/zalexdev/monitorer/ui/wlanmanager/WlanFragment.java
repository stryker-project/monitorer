package com.zalexdev.monitorer.ui.wlanmanager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.zalexdev.monitorer.SuUtils.customCommand;
import static com.zalexdev.monitorer.SuUtils.isMonitorModeEnabled;
import static com.zalexdev.monitorer.SuUtils.iw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.zalexdev.monitorer.MainActivity;
import com.zalexdev.monitorer.Prefs;
import com.zalexdev.monitorer.R;
import com.zalexdev.monitorer.SuUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;


public class WlanFragment extends Fragment {

    public Context context;
    public View view;
    public Activity activity;
    public ArrayList<String> wlanList = new ArrayList<>();
    public AutoCompleteTextView ifc = null;
    public AutoCompleteTextView command = null;
    public ArrayList<String> commands = new ArrayList<>();
    public MaterialButton execute = null;
    public TextView mode = null;


    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        // Inflating the layout and getting the views.
        View view = inflater.inflate(R.layout.fragment_wlan, container, false);
        LottieAnimationView animationView = view.findViewById(R.id.lottie_wlan);
        LottieAnimationView animationView2 = view.findViewById(R.id.lottie_wlan2);
        ifc = view.findViewById(R.id.select_interface);
        command = view.findViewById(R.id.select_cmd);
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.refresh);
        execute = view.findViewById(R.id.execute);
        mode = view.findViewById(R.id.mode);


        // Getting the context and activity of the fragment.
        context = getContext();
        activity = getActivity();

        // Checking if the interface is in monitor mode, if it is, it will disable it, if it isn't, it
        // will enable it.
        execute.setOnClickListener(v -> {
            execute.setEnabled(false);
            execute.setText("Executing...");
            new Thread(() -> {
                if (isMonitorModeEnabled(ifc.getText().toString())){

                    ArrayList<String> o = customCommand(command.getText().toString().replace("$ifc", ifc.getText().toString()).replace(";iw ", ";"+iw+" "));
                    Log.e("OUTPUT", o.toString());
                    activity.runOnUiThread(() -> execute.setText("Checking..."));
                    boolean isMonitorModeEnabled = isMonitorModeEnabled(ifc.getText().toString());
                    activity.runOnUiThread(() -> {
                        if (isMonitorModeEnabled){
                            ifc.setError("Failed to disable monitor mode!");
                            setDisableMonitorMode();

                        } else {
                            setEnableMonitorMode();
                            Toast.makeText(context, "Disabled monitor mode successfully!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    ArrayList<String> o = customCommand(command.getText().toString().replace("$ifc", ifc.getText().toString()).replace(";iw ", ";"+iw+" "));
                    Log.e("OUTPUT", o.toString());
                    activity.runOnUiThread(() -> execute.setText("Checking..."));
                    boolean isMonitorModeEnabled = isMonitorModeEnabled(ifc.getText().toString());
                    activity.runOnUiThread(() -> {
                        if (isMonitorModeEnabled){
                            setDisableMonitorMode();
                            Toast.makeText(context, "Enabled monitor mode successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            ifc.setError("Failed to enable monitor mode!");
                            setEnableMonitorMode();
                        }
                    });
                }
            }).start();
        });
        // Checking if the user has root access.
        Prefs prefs = new Prefs(context);
        if (prefs.getBoolean("noroot")){
            final Dialog rootDialog = new Dialog(context);
            rootDialog.setContentView(R.layout.root_dialog);
            rootDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            rootDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rootDialog.show();
        }

        setDisableMonitorMode();
        // This is checking if the wlanList is empty.
        if(wlanList.isEmpty()) {
            // It's playing the animation.
            animationView.setAnimation(R.raw.wifi);
            animationView2.setAnimation(R.raw.wifi);
            animationView.setMaxFrame(116);
            animationView2.setMaxFrame(116);
            animationView.playAnimation();
            animationView2.playAnimation();
        // It's running a command in a new thread, and then adding the output to the wlanList
        // ArrayList.
        new Thread(() -> {

            // Getting a list of all the wireless interfaces on the device.
            ArrayList<String> output = customCommand("ip link show | grep wlan");
            wlanList = new ArrayList<>();
            for (String s : output){
                if (s.contains("wlan")){
                    wlanList.add(s.split(":")[1].trim());
                }
            }
            // It's checking if the first wlan interface is in monitor mode, and then setting the
            // command to enable or disable monitor mode.
            if(wlanList.size() > 0){
                if (isMonitorModeEnabled(wlanList.get(0))){
                    setDisableMonitorMode();
                }else{
                    setEnableMonitorMode();
                }
            }
            // It's setting the adapter for the AutoCompleteTextView, and setting the text to the first
            // item in the wlanList ArrayList.
            activity.runOnUiThread(() -> {
                if(wlanList.size() > 0){
                    ifc.setText(wlanList.get(0));
                    ifc.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, wlanList));
                }else{
                    ifc.setText("wlan0");
                    ifc.setError("No wlan interfaces found!");
                }
                // Setting an onItemClickListener to the spinner. When an item is clicked, it will get
                // the name of the interface and check if it is in monitor mode. If it is, it will
                // disable monitor mode. If it is not, it will enable monitor mode.
                ifc.setOnItemClickListener((parent, view1, position, id) -> {
                    String ifcName = ifc.getText().toString();
                    Log.e("Selected interface:", ifcName);
                    new Thread(() -> {
                        if (isMonitorModeEnabled(ifcName)){
                            setDisableMonitorMode();
                        }else{
                            setEnableMonitorMode();
                        }
                    }).start();

                });
            });

        }).start();
        // It's checking if the WLAN list is empty. If it is, it's setting the animation to the first
        // frame, and then playing it.
        }else{
            // It's setting the animation to the last frame, and then playing it.
            Log.e("WLAN", "WLAN list is not empty. Using cached list.");
            animationView.setMaxFrame(116);
            animationView2.setMaxFrame(116);
            animationView.setMinFrame(116);
            animationView2.setMinFrame(116);
            animationView.playAnimation();
            animationView2.playAnimation();
            new Thread(() -> {
                if(wlanList.size() > 0){
                    if (isMonitorModeEnabled(wlanList.get(0))){
                        setDisableMonitorMode();
                    }else{
                        setEnableMonitorMode();
                    }
                }
            }).start();
        }


        // It's setting the onRefreshListener for the SwipeRefreshLayout.
        swipeRefreshLayout.setOnRefreshListener(() -> {
            restart(context);

        });
        return view;
    }

    /**
     * It sets the command to be executed to enable monitor mode, sets the current mode to managed, and
     * enables the execute button
     */
    public void setEnableMonitorMode(){
        activity.runOnUiThread(() -> {
            commands = new ArrayList<>();
            commands.add("ip link set $ifc down;echo '4' > /sys/module/wlan/parameters/con_mode;ip link set $ifc up");
            commands.add("ip link set $ifc down;iw dev $ifc set type monitor;ip link set $ifc up");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, commands);
            adapter.getFilter().filter(null);
            command.setAdapter(adapter);
            if (ifc.getText().toString().contains("wlan0")|| ifc.getText().toString().length() <3) {

                command.setText(commands.get(0), false);
            }else {

                command.setText(commands.get(1), false);
            }

            mode.setText("Current mode: Managed");
            execute.setText("Enable Monitor Mode");
            execute.setEnabled(true);
        });

    }

    /**
     * It sets the command to disable monitor mode, sets the current mode to monitor, and enables the
     * execute button
     */
    public void setDisableMonitorMode(){
        activity.runOnUiThread(() -> {
        commands = new ArrayList<>();
        commands.add("ip link set $ifc down;echo '0' > /sys/module/wlan/parameters/con_mode;ip link set $ifc up;svc wifi enable");
        commands.add("ip link set $ifc down;iw dev $ifc set type managed;ip link set $ifc up");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, commands);
        adapter.getFilter().filter(null);
        command.setAdapter(adapter);
        if (ifc.getText().toString().contains("wlan0") || ifc.getText().toString().length() <3) {
            command.setText(commands.get(0), false);}
        else {

            command.setText(commands.get(1), false);
        }
        mode.setText("Current Mode: Monitor");
        execute.setText("Disable Monitor Mode");
        execute.setEnabled(true);
        });
    }

    public static void restart(Context context) {

            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            Runtime.getRuntime().exit(0);

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}