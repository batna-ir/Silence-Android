package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Workaround for Android bug:
 * https://code.google.com/p/android/issues/detail?id=53313
 */
public class DummyActivity extends Activity {
  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
  }
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    finish();
  }
}
