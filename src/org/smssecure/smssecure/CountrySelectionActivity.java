package org.smssecure.smssecure;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;


public class CountrySelectionActivity extends BaseActivity
    implements CountrySelectionFragment.CountrySelectedListener

{
  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    this.setContentView(R.layout.country_selection);
  }

  @Override
  public void countrySelected(String countryName, int countryCode) {
    Intent result = getIntent();
    result.putExtra("country_name", countryName);
    result.putExtra("country_code", countryCode);

    this.setResult(RESULT_OK, result);
    this.finish();
  }
}
