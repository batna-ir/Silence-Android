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
import org.smssecure.smssecure.util.SilencePreferences;
import org.whispersystems.jobqueue.JobManager;
import org.whispersystems.jobqueue.dependencies.DependencyInjector;
import org.whispersystems.jobqueue.requirements.NetworkRequirementProvider;
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider;
import org.whispersystems.libsignal.util.AndroidSignalProtocolLogger;

import dagger.ObjectGraph;
import io.sentry.core.Sentry;
import saba.AppManager;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import static org.smssecure.smssecure.ConversationListActivity.appCompatActivity;


/**
 * Will be called once when the Silence process is created.
 * <p>
 * We're using this as an insertion point to patch up the Android PRNG disaster
 * and to initialize the job manager.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends Application implements DependencyInjector {

    private JobManager jobManager;
    private ObjectGraph objectGraph;
    public static Context globalContext;

    private MediaNetworkRequirementProvider mediaNetworkRequirementProvider = new MediaNetworkRequirementProvider();

    public static ApplicationContext getInstance(Context context) {
        return (ApplicationContext) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            globalContext = getApplicationContext();

            if (SilencePreferences.isFirstRun(globalContext)) {
                Sentry.captureMessage("Startup");
                SilencePreferences.setSmsDeliveryReportsEnabled(globalContext);
                SilencePreferences.setPromptedDeliveryReportsReminder(globalContext);
            }

            initializeRandomNumberFix();
            initializeLogging();
            initializeJobManager();
            NotificationChannels.create(this);
            CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                    .setDefaultFontPath("fonts/shabnam.ttf")
                    .setFontAttrId(R.attr.fontPath)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            AppManager.clearData(globalContext, appCompatActivity);
        }
    }


    @Override
    public void injectDependencies(Object object) {
        try {
            if (object instanceof InjectableType) {
                objectGraph.inject(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AppManager.clearData(globalContext, appCompatActivity);
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
            AppManager.clearData(globalContext, appCompatActivity);
        }
    }

    private void initializeLogging() {
        try {
            SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
        } catch (Exception e) {
            e.printStackTrace();
            AppManager.clearData(globalContext, appCompatActivity);
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
            AppManager.clearData(globalContext, appCompatActivity);
        }
    }

    public void notifyMediaControlEvent() {
        try {
            mediaNetworkRequirementProvider.notifyMediaControlEvent();
        } catch (Exception e) {
            e.printStackTrace();
            AppManager.clearData(globalContext, appCompatActivity);
        }
    }

}
