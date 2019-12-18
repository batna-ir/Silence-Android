package saba;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.smssecure.smssecure.ConversationListActivity;
import org.smssecure.smssecure.R;

import java.io.File;

public class AppManager {


    public static void clearData(Context context, AppCompatActivity appCompatActivity) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, context.getString(R.string.reconfiguring_app), Toast.LENGTH_LONG).show();
            }
        });
        deleteCache(context);
        deletePref();
        deleteDbs(context);
        appCompatActivity.finish();
        context.startActivity(new Intent(context, ConversationListActivity.class));
    }

    private static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private static void deletePref() {
        try {
            File sharedPreferenceFile = new File("/data/data/com.sabaos.secureSMS/shared_prefs/");
            File[] listFiles = sharedPreferenceFile.listFiles();
            for (File file : listFiles) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteDbs(Context context) {
        try {
            context.deleteDatabase("_jobqueue-SilenceJobs.db");
            context.deleteDatabase("canonical_address.db");
            context.deleteDatabase("messages.db");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
