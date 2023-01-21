package com.zalexdev.monitorer;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class SuUtils {
    // The path to the iw binary.
    public static final String iw = "./data/data/com.zalexdev.monitorer/files/iw";


    /**
     * It takes a command as a string, runs it as root, and returns the output as an ArrayList of
     * strings
     *
     * @param command The command you want to run.
     * @return An ArrayList of Strings.
     */
    public static ArrayList<String> customCommand(String command){
        ArrayList<String> result = new ArrayList<>();
        Process process = generateSuProcess();
        try {
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();
            stdin.write((command + '\n').getBytes());
            stdin.write(("exit\n").getBytes());
            stdin.flush();
            stdin.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            String line;
            while ((line = br.readLine()) != null) {result.add(line);}
            br.close();
            BufferedReader br2 = new BufferedReader(new InputStreamReader(stderr));
            String lineError;
            while ((lineError = br2.readLine()) != null) {result.add(lineError);}
            br2.close();
        } catch (IOException e) {
            Log.d("Debug: ", "An IOException was caught: " + e.getMessage());
        }

        process.destroy();
        return result;
    }
    /**
     * It tries to run the command "su" and if it fails, it tries to run the command "echo Device is
     * not rooted"
     *
     * @return A process object.
     */
    public static Process generateSuProcess(){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();

            try {
                process = Runtime.getRuntime().exec("echo Device is not rooted");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return  process;
    }
    /**
     * It checks if the interface is in monitor mode by running the command `iw dev <interfaceName>
     * info` and checking if the output contains the string `type monitor`
     *
     * @param interfaceName The name of the interface you want to check.
     * @return A boolean value.
     */
    public static boolean isMonitorModeEnabled(String interfaceName){
        ArrayList<String> temp = customCommand(iw+" dev "+interfaceName+" info");
        for (String t :temp){
            if (t.contains("type monitor")){
                return true;
            }
        }
        Log.e("Debug: ", "Monitor mode is disabled for "+interfaceName);
        Log.e("Output: ", temp.toString());
        return false;

    }
    /**
     * If any of the strings in the list contain the item, return true, otherwise return false.
     *
     * @param list The list you want to search through.
     * @param item The item to search for in the list.
     * @return The method returns true if the item is in the list, and false if it is not.
     */
    public static boolean contains(ArrayList<String> list, String item){
        for (String s : list){if (s.contains(item)){return true;}}
        return false;
    }

    /**
     * It checks if the device is rooted.
     *
     * @return The return value is a boolean.
     */
    public static  boolean checkRoot(){
        return contains(customCommand("id"),"uid=0");
    }
}
