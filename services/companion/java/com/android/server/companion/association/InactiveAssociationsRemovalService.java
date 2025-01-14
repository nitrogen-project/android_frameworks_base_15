/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.companion.association;

import static java.util.concurrent.TimeUnit.DAYS;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.companion.CompanionDeviceManagerServiceInternal;

/**
 * A Job Service responsible for clean up self-managed associations if it's idle for 90 days.
 *
 * The job will be executed only if the device is charging and in idle mode due to the application
 * will be killed if association/role are revoked. See {@link DisassociationProcessor}
 */
public class InactiveAssociationsRemovalService extends JobService {

    private static final String TAG = "CDM_InactiveAssociationsRemovalService";
    private static final String JOB_NAMESPACE = "companion";
    private static final int JOB_ID = 1;
    private static final long ONE_DAY_INTERVAL = DAYS.toMillis(1);

    @Override
    public boolean onStartJob(final JobParameters params) {
        Slog.i(TAG, "Execute the Association Removal job");

        LocalServices.getService(CompanionDeviceManagerServiceInternal.class)
                .removeInactiveSelfManagedAssociations();

        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        Slog.i(TAG, "Association removal job stopped; id=" + params.getJobId()
                + ", reason="
                + JobParameters.getInternalReasonCodeDescription(
                params.getInternalStopReasonCode()));
        return false;
    }

    /**
     * Schedule this job.
     */
    public static void schedule(Context context) {
        Slog.i(TAG, "Scheduling the Association Removal job");
        final JobScheduler jobScheduler =
                context.getSystemService(JobScheduler.class).forNamespace(JOB_NAMESPACE);
        final JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, InactiveAssociationsRemovalService.class))
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(ONE_DAY_INTERVAL)
                .build();
        jobScheduler.schedule(job);
    }
}
