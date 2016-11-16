


/*
 * Copyright (c) 2016 Nike, Inc.
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

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.gaffer.ConfigurationDelegate.appender
import static ch.qos.logback.classic.gaffer.ConfigurationDelegate.logger
import static ch.qos.logback.classic.gaffer.ConfigurationDelegate.root
import static ch.qos.logback.classic.gaffer.ConfigurationDelegate.scan
import static ch.qos.logback.core.spi.ContextAware.addInfo

private String getEnvironmentString() {
    def environment = System.getProperty("@environment")
    if (environment != null)
        return environment

    return System.getProperty("archaius.deployment.environment")
}

private boolean isLocalEnvironment() {
    // This logback config file is for unit testing - the answer is always true
    return true
}

private boolean shouldOutputToConsole() {
    // This logback config file is for unit testing - the answer is always true
    return true
}

private boolean shouldOutputToLogFile() {
    // This logback config file is for unit testing - the answer is always false
    return false
}

addInfo("Processing logback.groovy, environment: " + getEnvironmentString() + "...")
println("Processing logback.groovy, environment: " + getEnvironmentString() + "...")

def SERVICE_ENV_NAME = getEnvironmentString() == null? "NA" : getEnvironmentString()

def encoderPattern = "traceId=%X{traceId} %date{\"yyyy-MM-dd'T'HH:mm:ss,SSSXXX\"} [%thread] appname=@@APPNAME@@ environment=${SERVICE_ENV_NAME} version=@@RELEASE@@ |-%-5level %logger{36} - %msg%n"
def defaultAsyncQueueSize = 16000

def Appender consoleAppender = null
def Appender fileAppender = null
def allAsyncAppendersArray = []

addInfo("******Outputting to console: " + shouldOutputToConsole())
println("******Outputting to console: " + shouldOutputToConsole())

if (shouldOutputToConsole()) {
    appender("ConsoleAppender", ConsoleAppender) {
        encoder(PatternLayoutEncoder) {
            pattern = encoderPattern
        }

        consoleAppender = component
    }

    appender("AsyncConsoleAppender", AsyncAppender) {
        queueSize = defaultAsyncQueueSize
        component.addAppender(consoleAppender)
    }

    allAsyncAppendersArray.add("AsyncConsoleAppender")
}

def LOG_FILE_DIRECTORY_PATH = isLocalEnvironment() ? "logs" : "/var/log/@@APPNAME@@"
addInfo("******Outputting to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputToLogFile())
println("******Outputting to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputToLogFile())

if (shouldOutputToLogFile()) {
    appender("FileAppender", RollingFileAppender) {
        file = "${LOG_FILE_DIRECTORY_PATH}/@@APPNAME@@.log"

        rollingPolicy(FixedWindowRollingPolicy) {
            // NOTE: To have it archive into a zip or gzip file end the fileNamePattern with .zip or .gz
            fileNamePattern = "${LOG_FILE_DIRECTORY_PATH}/@@APPNAME@@.%i.log.zip"
            // Make sure we stay under 10GB for the amount of history we keep (in reality size on disk will be lower due to zipping the archived files, but we don't have a lot of space to play with so better safe than sorry)
            minIndex = 1
            maxIndex = 20
        }

        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = "500MB"
        }

        encoder(PatternLayoutEncoder) {
            pattern = encoderPattern
        }

        fileAppender = component
    }

    appender("AsyncFileAppender", AsyncAppender) {
        queueSize = defaultAsyncQueueSize
        component.addAppender(fileAppender)
    }

    allAsyncAppendersArray.add("AsyncFileAppender")
}

//logger("com.blah.SomethingSpammy", WARN, allAsyncAppendersArray, false)
logger("org.apache.http", INFO, allAsyncAppendersArray, false)
logger("com.jayway.restassured", INFO, allAsyncAppendersArray, false)
logger("com.ning.http.client", INFO, allAsyncAppendersArray, false)

logger("com.nike.trace.Tracer", INFO, allAsyncAppendersArray, false)

logger("com.codahale.metrics.JmxReporter", INFO, allAsyncAppendersArray, false)

// Root logger.
root(DEBUG, allAsyncAppendersArray)

// Auto-scan this config file for changes and reload the logging config if it changes.
// NOTE: Due to performance concerns, the scanner is not only time based (which we set here), it is also sampled
//       so the time check only happens once every x logging attempts (x is dynamically determined by logback based
//       on how often the app logs). Both the sample check and the time check must pass before the log file will be
//       reprocessed. See http://logback.qos.ch/manual/configuration.html#autoScan for more details.
scan("60 seconds")

addInfo("...logback.groovy processing finished.")
println("...logback.groovy processing finished.")