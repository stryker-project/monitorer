package com.zalexdev.monitorer;


import static com.zalexdev.monitorer.SuUtils.contains;
import static com.zalexdev.monitorer.SuUtils.customCommand;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ViewHolder> {
    public ArrayList<String> pathList;
    public ArrayList<String> driverList;
    public Context context;
    public Activity activity;
    public Prefs prefs;

    public int id = 0;

    public ModulesAdapter(Context context2, Activity mActivity, ArrayList<String> path, ArrayList<String> driver) {
        context = context2;
        pathList = path;
        activity = mActivity;
        driverList = driver;
        prefs = new Prefs(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.module_item, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder adapter, @SuppressLint("RecyclerView") final int position) {
        adapter.name.setText(pathList.get(position).replace(".ko", ""));
        adapter.switchMaterial.setChecked(contains(driverList, pathList.get(position).replace(".ko", "")));
        // Setting the onClickListener for the switchMaterial.
        adapter.switchMaterial.setOnClickListener(v -> {
            boolean checked = adapter.switchMaterial.isChecked();
            adapter.switchMaterial.setEnabled(false);
            if (checked){
            new Thread(() -> {
                customCommand("insmod /system/lib/modules/"+pathList.get(position));
                ArrayList<String> driverList = customCommand("ls /sys/bus/usb/drivers");
                if (contains(driverList, pathList.get(position).replace(".ko", ""))){
                    toaster("Module loaded successfully");
                    prefs.addModule(pathList.get(position));
                }else{
                    toaster("Module failed to load");
                    activity.runOnUiThread(() -> adapter.switchMaterial.setChecked(false));
                }
                activity.runOnUiThread(() -> {
                    adapter.switchMaterial.setEnabled(true);
                    TextView list = activity.findViewById(R.id.driverList);
                    list.setText("");
                    for (String s : driverList){
                        list.append(s + "\n");
                    }
                });

            }).start();
            }else{
                new Thread(() -> {
                customCommand("rmmod /system/lib/modules/"+pathList.get(position));
                ArrayList<String> driverList = customCommand("ls /sys/bus/usb/drivers");
                if (!contains(driverList, pathList.get(position).replace(".ko", ""))){
                    toaster("Module unloaded successfully");
                    prefs.removeModule(pathList.get(position));
                }else{
                    toaster("Module failed to unload");
                    activity.runOnUiThread(() -> adapter.switchMaterial.setChecked(true));
                }
                activity.runOnUiThread(() -> {
                    adapter.switchMaterial.setEnabled(true);
                    TextView list = activity.findViewById(R.id.driverList);
                    list.setText("");
                    for (String s : driverList){
                        list.append(s + "\n");
                    }
                });

            }).start();
            }
        });
    }

    @Override
    public int getItemCount() {

        return pathList.size();
    }

    /**
     * > This function takes a string and displays it as a toast message on the screen
     *
     * @param msg The message you want to display
     */
    public void toaster(String msg) {
        activity.runOnUiThread(() -> {
            Toast toast = Toast.makeText(context,
                    msg, Toast.LENGTH_SHORT);
            toast.show();
        });

    }



    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public SwitchMaterial switchMaterial;

        public ViewHolder(View v) {
            super(v);
            switchMaterial = v.findViewById(R.id.module_switch);
            name = v.findViewById(R.id.module_name);

        }

    }


}
