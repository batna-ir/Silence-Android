/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.notifications.MessageNotifier;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.service.KeyCachingService;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;
import org.smssecure.smssecure.util.SilencePreferences;

import java.util.Objects;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import saba.AppManager;


import static org.smssecure.smssecure.ApplicationContext.globalContext;

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{
  private static final String TAG = ConversationListActivity.class.getSimpleName();

  private final DynamicTheme    dynamicTheme    = new DynamicTheme   ();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ConversationListFragment fragment;
  private ContentObserver observer;
  private MasterSecret masterSecret;
  @SuppressLint("StaticFieldLeak")
  public static AppCompatActivity appCompatActivity;

  @Override
  protected void onPreCreate() {

    try {
      dynamicTheme.onCreate(this);
      dynamicLanguage.onCreate(this);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }


  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  @Override
  protected void onCreate(Bundle icicle, @NonNull MasterSecret masterSecret) {
    try {
      globalContext = getApplicationContext();
      appCompatActivity = this;
      this.masterSecret = masterSecret;
      Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
      getSupportActionBar().setTitle(R.string.app_name);
      fragment = initFragment(android.R.id.content, new ConversationListFragment(), masterSecret, dynamicLanguage.getCurrentLocale());
      initializeContactUpdatesReceiver();

    } catch (Exception e) {
      try {
        Permissions.with(this)
                .request(Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS)
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.WelcomeActivity_silence_requires_the_phone_and_sms_permissions_in_order_to_work_but_it_has_been_permanently_denied))
                .onSomeGranted((permissions) -> {
                })
                .execute();
      } catch (Exception ex) {
        ex.printStackTrace();
        AppManager.clearData(globalContext, appCompatActivity);
      }
    }
  }

  @Override
  public void onResume() {
    try {
      super.onResume();
      dynamicTheme.onResume(this);
      dynamicLanguage.onResume(this);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  @Override
  public void onDestroy() {
    if (observer != null) getContentResolver().unregisterContentObserver(observer);
    super.onDestroy();
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
      try {
          MenuInflater inflater = this.getMenuInflater();
          menu.clear();

          inflater.inflate(R.menu.text_secure_normal, menu);

          menu.findItem(R.id.menu_clear_passphrase).setVisible(!SilencePreferences.isPasswordDisabled(this));

          inflater.inflate(R.menu.conversation_list, menu);
          MenuItem menuItem = menu.findItem(R.id.menu_search);
          initializeSearch(menuItem);

          super.onPrepareOptionsMenu(menu);
      } catch (Exception e) {
          e.printStackTrace();
          AppManager.clearData(globalContext, appCompatActivity);
      }
      return true;
  }

  private void initializeSearch(MenuItem searchViewItem) {
    SearchView searchView = (SearchView)MenuItemCompat.getActionView(searchViewItem);
    searchView.setQueryHint(getString(R.string.ConversationListActivity_search));
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        if (fragment != null) {
          fragment.setQueryFilter(query);
          return true;
        }

        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return onQueryTextSubmit(newText);
      }
    });

    MenuItemCompat.setOnActionExpandListener(searchViewItem, new MenuItemCompat.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem menuItem) {
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        if (fragment != null) {
          fragment.resetQueryFilter();
        }

        return true;
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
    case R.id.menu_archived_conversations: handleSwitchToArchive();        return true;
    case R.id.menu_new_group:              createGroup();                  return true;
    case R.id.menu_settings:               handleDisplaySettings();        return true;
    case R.id.menu_clear_passphrase:       handleClearPassphrase();        return true;
    case R.id.menu_mark_all_read:          handleMarkAllRead();            return true;
    case R.id.menu_import_export:          handleImportExport();           return true;
    case R.id.menu_my_identity:            handleMyIdentity();             return true;
    }

    return false;
  }

  @Override
  public void onCreateConversation(long threadId, Recipients recipients, int distributionType, long lastSeen) {
    try {
      Intent intent = new Intent(this, ConversationActivity.class);
      intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, recipients.getIds());
      intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
      intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
      intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());
      intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeen);

      startActivity(intent);
      overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  @Override
  public void onSwitchToArchive() {
    try {
      Intent intent = new Intent(this, ConversationListArchiveActivity.class);
      startActivity(intent);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  private void createGroup() {
    try {
      Intent intent = new Intent(this, GroupCreateActivity.class);
      startActivity(intent);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  private void handleSwitchToArchive() {
    onSwitchToArchive();
  }

  private void handleDisplaySettings() {
    Intent preferencesIntent = new Intent(this, ApplicationPreferencesActivity.class);
    startActivity(preferencesIntent);
  }

  private void handleClearPassphrase() {
    try {
      Intent intent = new Intent(this, KeyCachingService.class);
      intent.setAction(KeyCachingService.CLEAR_KEY_ACTION);
      startService(intent);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  private void handleImportExport() {
    try {
      startActivity(new Intent(this, ImportExportActivity.class));
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  private void handleMyIdentity() {
    try {
      startActivity(new Intent(this, ViewLocalIdentityActivity.class));
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void handleMarkAllRead() {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        try {
          DatabaseFactory.getThreadDatabase(ConversationListActivity.this).setAllThreadsRead();
          MessageNotifier.updateNotification(ConversationListActivity.this, masterSecret);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    }.execute();
  }

  private void initializeContactUpdatesReceiver() {
    try {
      observer = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
          super.onChange(selfChange);
          Log.w(TAG, "Detected android contact data changed, refreshing cache");
          RecipientFactory.clearCache();
          ConversationListActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                fragment.getListAdapter().notifyDataSetChanged();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
        }
      };
      getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI,true, observer);
    } catch (Exception e) {
      e.printStackTrace();
      AppManager.clearData(globalContext, appCompatActivity);
    }
  }
}
