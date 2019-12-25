/*
 * Copyright (C) 2013 Open Whisper Systems
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

import android.app.Application;
import android.content.Context;

import org.smssecure.smssecure.crypto.PRNGFixes;
import org.smssecure.smssecure.dependencies.InjectableType;
import org.smssecure.smssecure.jobs.persistence.EncryptingJobSerializer;
import org.smssecure.smssecure.jobs.requirements.MasterSecretRequirementProvider;
import org.smssecure.smssecure.jobs.requirements.MediaNetworkRequirementProvider;
import org.smssecure.smssecure.jobs.requirements.ServiceRequirementProvider;
import org.smssecure.smssecure.notifications.NotificationChannels;
import org.whispersystems.jobqueue.JobManager;
import org.whispersystems.jobqueue.dependencies.DependencyInjector;
import org.whispersystems.jobqueue.requirements.NetworkRequirementProvider;
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider;
import org.whispersystems.libsignal.util.AndroidSignalProtocolLogger;

import dagger.ObjectGraph;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


/**
 * Will be called once when the Silence process is created.
 *
 * We're using this as an insertion point to patch up the Android PRNG disaster
 * and to initialize the job manager.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends Application implements DependencyInjector {

  private JobManager  jobManager;
  private ObjectGraph objectGraph;
  public static Context globalContext;

  private MediaNetworkRequirementProvider mediaNetworkRequirementProvider = new MediaNetworkRequirementProvider();

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
  }

  @Override
  public void onCreate() {
    super.onCreate();
    globalContext = getApplicationContext();
    initializeRandomNumberFix();
    initializeLogging();
    initializeJobManager();
    NotificationChannels.create(this);
    CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/shabnam.ttf")
            .setFontAttrId(R.attr.fontPath)
            .build()
    );
  }



  @Override
  public void injectDependencies(Object object) {
    if (object instanceof InjectableType) {
      objectGraph.inject(object);
    }
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeRandomNumberFix() {
    try {
      PRNGFixes.apply();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeLogging() {
    try {
      SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeJobManager() {
    try {
      this.jobManager = JobManager.newBuilder(this)
                                  .withName("SilenceJobs")
                                  .withDependencyInjector(this)
                                  .withJobSerializer(new EncryptingJobSerializer())
                                  .withRequirementProviders(new MasterSecretRequirementProvider(this),
                                                            new ServiceRequirementProvider(this),
                                                            new NetworkRequirementProvider(this),
                                                            mediaNetworkRequirementProvider)
                                  .withConsumerThreads(5)
                                  .build();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void notifyMediaControlEvent() {
    mediaNetworkRequirementProvider.notifyMediaControlEvent();
  }

}
