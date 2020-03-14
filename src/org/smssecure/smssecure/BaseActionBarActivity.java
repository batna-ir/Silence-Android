package org.smssecure.smssecure;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import org.smssecure.smssecure.util.SilencePreferences;

import java.lang.reflect.Field;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import saba.AppManager;

import static org.smssecure.smssecure.ApplicationContext.globalContext;
import static org.smssecure.smssecure.ConversationListActivity.appCompatActivity;


public abstract class BaseActionBarActivity extends AppCompatActivity {
  private static final String TAG = BaseActionBarActivity.class.getSimpleName();

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    try {
      if (BaseActivity.isMenuWorkaroundRequired()) {
        forceOverflowMenu();
      }
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    try {
      initializeScreenshotSecurity();
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) {
      openOptionsMenu();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  private void initializeScreenshotSecurity() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
              SilencePreferences.isScreenSecurityEnabled(this))
      {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
      } else {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
      }
  }

  /**
   * Modified from: http://stackoverflow.com/a/13098824
   */
  private void forceOverflowMenu() {
    try {
      ViewConfiguration config       = ViewConfiguration.get(this);
      Field             menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
      if(menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (IllegalAccessException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    } catch (NoSuchFieldException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  protected void startActivitySceneTransition(Intent intent, View sharedView, String transitionName) {
    try {
      Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedView, transitionName)
                                           .toBundle();
      ActivityCompat.startActivity(this, intent, bundle);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }
}
