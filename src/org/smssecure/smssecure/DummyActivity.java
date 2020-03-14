package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;


/**
 * Workaround for Android bug:
 * https://code.google.com/p/android/issues/detail?id=53313
 */
public class DummyActivity extends Activity {
  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    finish();
  }
}
