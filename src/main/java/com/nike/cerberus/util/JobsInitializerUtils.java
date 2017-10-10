/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.util;

import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import org.knowm.sundial.SundialJobScheduler;
import org.quartz.jobs.Job;
import org.quartz.jobs.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JobsInitializerUtils {

    public static void initializeJobs(Config appConfig,
                                      Injector appInjector) {

        Logger log = LoggerFactory.getLogger("com.nike.cerberus.util.JobsInitializerUtils");

        if (! appConfig.getBoolean("cms.jobs.enabled")) {
            return;
        }
        try {
            SundialJobScheduler.createScheduler(10, null);
            log.info("Creating Guice based job factory");
            SundialJobScheduler.getScheduler().setJobFactory((bundle, scheduler) -> {
                JobDetail jobDetail = bundle.getJobDetail();
                Class<? extends Job> jobClass = jobDetail.getJobClass();
                return appInjector.getInstance(jobClass);
            });

            ConfigList jobs = appConfig.getList("cms.jobs.configuredJobs");

            jobs.forEach(jobConfig -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> jobData = (Map<String, Object>) jobConfig.unwrapped();

                String jobClassName = String.valueOf(jobData.get("jobClassName"));
                Integer repeatCount = (Integer) jobData.get("repeatCount");
                Integer repeatInterval = (Integer) jobData.get("repeatInterval");
                String repeatTimeUnit = String.valueOf(jobData.get("repeatTimeUnit"));

                String triggerName = jobClassName + "-simple";
                String jobFullClassPath = "com.nike.cerberus.jobs." + jobClassName;
                long millis = TimeUnit.valueOf(repeatTimeUnit.toUpperCase()).toMillis(repeatInterval);

                SundialJobScheduler.addJob(jobClassName, jobFullClassPath, null, false);
                SundialJobScheduler.addSimpleTrigger(triggerName,
                        jobClassName,
                        repeatCount,
                        millis);

                log.info("Registered Job: {} to run every {} milliseconds {} times",
                        jobFullClassPath, millis, repeatCount);
            });

            final long initialDelayInMillis = TimeUnit.valueOf(
                    appConfig.getString("cms.jobs.initialDelayTimeUnits").toUpperCase())
                    .toMillis(appConfig.getLong("cms.jobs.initialDelay"));

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Thread.sleep(initialDelayInMillis);
                } catch (InterruptedException e) {
                    log.error("Job scheduler delay thread interrupted", e);
                }

                log.info("Starting scheduler to allow jobs to run");
                SundialJobScheduler.startScheduler();
            });
        } catch (Exception e) {
            log.error("Failed to parse job information from the config and schedule Job", e);
        }
    }

}
