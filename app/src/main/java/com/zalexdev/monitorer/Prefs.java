package com.zalexdev.monitorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class Prefs {
    public Context context;
    public SharedPreferences preferences;
    public Prefs(Context context1) {
        context = context1;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    public void putListString(String key, ArrayList<String> stringList) {
        isNull(key);
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
    private void isNull(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
    }
    public ArrayList<String> getListString(String key) {
        return new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }
    public void addModule(String module) {
        ArrayList<String> modules = getListString("modules");
        modules.add(module);
        putListString("modules", modules);
    }
    public void removeModule(String module) {
        ArrayList<String> modules = getListString("modules");
        modules.remove(module);
        putListString("modules", modules);
    }
    public ArrayList<String> getModules() {
        return getListString("modules");
    }
    public void putBoolean(String key, boolean value) {
        isNull(key);
        preferences.edit().putBoolean(key, value).apply();
    }
    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }
}
