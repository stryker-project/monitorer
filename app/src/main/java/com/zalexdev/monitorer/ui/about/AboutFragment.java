package com.zalexdev.monitorer.ui.about;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.zalexdev.monitorer.R;


public class AboutFragment extends Fragment {

    public Context context;
    public Activity activity;
    public boolean connected = false;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_about, container, false);
        context = getContext();
        activity = getActivity();

        TextView info = view.findViewById(R.id.device_info);
        MaterialCardView tg = view.findViewById(R.id.telegram);
        MaterialCardView web = view.findViewById(R.id.web);
        MaterialCardView github = view.findViewById(R.id.github);

        // Opening links in the browser.
        web.setOnClickListener(view1 -> openLink("https://strykerdef.com"));
        tg.setOnClickListener(view1 -> openLink("https://t.me/strykerapp"));
        github.setOnClickListener(view1 -> openLink("https://github.com/stryker-project/monitorer"));
        info.setText(getDeviceName() + "\n" + Build.BOARD + "\n" + "Android SDK: " + Build.VERSION.SDK);

        return view;
    }

    /**
     * If the model name starts with the manufacturer name, return the model name. Otherwise, return
     * the manufacturer name followed by the model name
     *
     * @return The name of the device.
     */
    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    /**
     * If the first character of the string is already uppercase, return the string, otherwise return
     * the first character uppercase followed by the rest of the string
     *
     * @param s The string to capitalize.
     * @return The first letter of the string is being capitalized.
     */
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * It opens a link in the browser
     *
     * @param url The URL to open.
     */
    public void openLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }




}