
package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;


public class ExitActivity extends Activity {

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= 21) {
      finishAndRemoveTask();
    } else {
      finish();
    }

    System.exit(0);
  }

  public static void exitAndRemoveFromRecentApps(Activity activity) {
    Intent intent = new Intent(activity, ExitActivity.class);

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            | Intent.FLAG_ACTIVITY_NO_ANIMATION);

    activity.startActivity(intent);
  }
}
