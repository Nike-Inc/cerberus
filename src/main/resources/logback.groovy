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

import static ch.qos.logback.classic.Level.*
import static ch.qos.logback.core.spi.ContextAware.addInfo

private String getEnvironmentString() {
    def environment = System.getProperty("@environment")
    if (environment != null)
        return environment

    return System.getProperty("archaius.deployment.environment")
}

private boolean isLocalEnvironment() {
    def serviceName = getEnvironmentString()
    return (serviceName == null) || ("local".equals(serviceName))
}

private boolean booleanSystemPropertyExtractionHelper(String systemPropertyToSearchFor, boolean defaultIfSystemPropertyNotFound) {
    // If the property is defined then return true unless the value is explicitly false.
    def outputSystemProp = System.getProperty(systemPropertyToSearchFor)
    if (outputSystemProp != null) {
        if (outputSystemProp.equalsIgnoreCase("false"))
            return false

        return true
    }

    // Not explicitly defined. Return the default argument passed in.
    return defaultIfSystemPropertyNotFound
}

private boolean shouldOutputToConsole() {
    // If not explicitly requested then we only want to output to console if you're running on your local box as per API-3204
    return booleanSystemPropertyExtractionHelper("logToConsole", isLocalEnvironment())
}

private boolean shouldOutputToLogFile() {
    // If not explicitly requested then we only want to output to log file if you're *NOT* running on your local box as per API-3204
    return booleanSystemPropertyExtractionHelper("logToLocalFile", !isLocalEnvironment())
}

private boolean shouldOutputAccessLogsToConsole() {
    // If not explicitly requested then we only want to output to console if you're running on your local box as per API-3204
    return booleanSystemPropertyExtractionHelper("logAccessLogToConsole", isLocalEnvironment())
}

private boolean shouldOutputAccessLogsToLogFile() {
    // If not explicitly requested then we only want to output to log file if you're *NOT* running on your local box as per API-3204
    return booleanSystemPropertyExtractionHelper("logAccessLogToLocalFile", !isLocalEnvironment())
}

addInfo("Processing logback.groovy, environment: " + getEnvironmentString() + "...")
println("Processing logback.groovy, environment: " + getEnvironmentString() + "...")

def SERVICE_ENV_NAME = getEnvironmentString() == null? "NA" : getEnvironmentString()

def encoderPattern = "traceId=%X{traceId} %date{\"yyyy-MM-dd'T'HH:mm:ss,SSSXXX\"} [%thread] appname=@@APPNAME@@ environment=${SERVICE_ENV_NAME} version=@@RELEASE@@ |-%-5level %logger{36} - %msg%n"
def accessLogEncoderPattern = "%msg%n"
def defaultAsyncQueueSize = 16000

def allAsyncAppendersArray = []
def allAsyncAccessLogAppendersArray = []

addInfo("******Outputting app logs to console: " + shouldOutputToConsole())
println("******Outputting app logs to console: " + shouldOutputToConsole())

def setupConsoleAppender(String appenderName, String encoderPatternToUse, List allAsyncAppendersListToUse, int defaultAsyncQueueSize) {
    def Appender coreAppender = null
    def asyncAppenderName = "Async" + appenderName

    appender(appenderName, ConsoleAppender) {
        encoder(PatternLayoutEncoder) {
            pattern = encoderPatternToUse
        }

        coreAppender = component
    }

    appender(asyncAppenderName, AsyncAppender) {
        queueSize = defaultAsyncQueueSize
        component.addAppender(coreAppender)
    }

    allAsyncAppendersListToUse.add(asyncAppenderName)
}

if (shouldOutputToConsole()) {
    setupConsoleAppender("ConsoleAppender", encoderPattern, allAsyncAppendersArray, defaultAsyncQueueSize)
}

addInfo("******Outputting access logs to console: " + shouldOutputAccessLogsToConsole())
println("******Outputting access logs to console: " + shouldOutputAccessLogsToConsole())

if (shouldOutputAccessLogsToConsole()) {
    setupConsoleAppender("AccessLogConsoleAppender", accessLogEncoderPattern, allAsyncAccessLogAppendersArray, defaultAsyncQueueSize)
}

def LOG_FILE_DIRECTORY_PATH = isLocalEnvironment() ? "logs" : "/var/log/@@APPNAME@@"
addInfo("******Outputting app logs to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputToLogFile())
println("******Outputting app logs to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputToLogFile())

def setupLogFileAppender(String appenderName, String encoderPatternToUse, List allAsyncAppendersListToUse, String logFileDirectoryPath, String baseFilename, int numRolloverFilesToKeep, String maxFileSizeAsString, int defaultAsyncQueueSize) {
    def Appender coreAppender = null
    def asyncAppenderName = "Async" + appenderName

    appender(appenderName, RollingFileAppender) {
        file = "${logFileDirectoryPath}/${baseFilename}.log"

        rollingPolicy(FixedWindowRollingPolicy) {
            // NOTE: To have it archive into a zip or gzip file end the fileNamePattern with .zip or .gz
            fileNamePattern = "${logFileDirectoryPath}/${baseFilename}.%i.log.zip"
            minIndex = 1
            maxIndex = numRolloverFilesToKeep
        }

        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = maxFileSizeAsString
        }

        encoder(PatternLayoutEncoder) {
            pattern = encoderPatternToUse
        }

        coreAppender = component
    }

    appender(asyncAppenderName, AsyncAppender) {
        queueSize = defaultAsyncQueueSize
        component.addAppender(coreAppender)
    }

    allAsyncAppendersListToUse.add(asyncAppenderName)
}

if (shouldOutputToLogFile()) {
    // Make sure we stay under 10GB for the amount of app log history we keep (in reality size on disk will be lower due to zipping the archived files, but we don't have a lot of space to play with so better safe than sorry)
    setupLogFileAppender("FileAppender", encoderPattern, allAsyncAppendersArray, LOG_FILE_DIRECTORY_PATH, "@@APPNAME@@", 20, "500MB", defaultAsyncQueueSize)
}

addInfo("******Outputting access logs to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputAccessLogsToLogFile())
println("******Outputting access logs to log file in directory ${LOG_FILE_DIRECTORY_PATH}: " + shouldOutputAccessLogsToLogFile())

if (shouldOutputAccessLogsToLogFile()) {
    // Make sure we stay under 500MB for the amount of access log history we keep (in reality size on disk will be lower due to zipping the archived files, but we don't have a lot of space to play with so better safe than sorry)
    setupLogFileAppender("AccessLogFileAppender", accessLogEncoderPattern, allAsyncAccessLogAppendersArray, LOG_FILE_DIRECTORY_PATH, "@@APPNAME@@-access", 10, "50MB", defaultAsyncQueueSize)
}

// CUSTOM LOGGER SETTINGS (setting output levels for various classes)
logger("com.nike.riposte.server.handler.RequestContentDeserializerHandler", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.server.handler.DTraceStartHandler", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.metrics.codahale.CodahaleMetricsListener", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.server.http.ResponseSender", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.server.handler.ProxyRouterEndpointExecutionHandler\$StreamingCallbackForCtx", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.client.asynchttp.netty.StreamingAsyncHttpClient", INFO, allAsyncAppendersArray, false)
logger("com.nike.riposte.server.handler.SecurityValidationHandler", INFO, allAsyncAppendersArray, false)

logger("com.netflix.config.util.OverridingPropertiesConfiguration", INFO, allAsyncAppendersArray, false) // Part of Archaius - set this to debug if you want a little more info into what archaius is doing
logger("org.hibernate.validator", INFO, allAsyncAppendersArray, false)
logger("com.nike.trace.Tracer", INFO, allAsyncAppendersArray, false)
logger("com.nike.trace.http.HttpRequestTracingUtils", INFO, allAsyncAppendersArray, false)
logger("org.apache.cassandra", INFO, allAsyncAppendersArray, false)

logger("com.ning.http", INFO, allAsyncAppendersArray, false)
logger("org.apache.http", INFO, allAsyncAppendersArray, false)
logger("com.netflix", INFO, allAsyncAppendersArray, false)
logger("com.newrelic", INFO, allAsyncAppendersArray, false)

logger("com.nike.metrics.newrelic.NewRelicReporter", WARN, allAsyncAppendersArray, false)

logger("com.nike.cerberus", INFO, allAsyncAppendersArray, false)

logger("com.nike.cerberus.mapper.AuthTokenMapper", DEBUG, allAsyncAppendersArray, false)
logger("com.nike.cerberus.mapper.PermissionsMapper", DEBUG, allAsyncAppendersArray, false)
logger("com.nike.cerberus.mapper.SecureDataMapper", DEBUG, allAsyncAppendersArray, false)
logger("com.nike.cerberus.service.SecureDataService", DEBUG, allAsyncAppendersArray, false)
logger("com.nike.cerberus.service.PermissionsService", DEBUG, allAsyncAppendersArray, false)

logger("VALID_WINGTIPS_SPANS", OFF, allAsyncAppendersArray, false)

// ACCESS LOG SETTINGS
def disableAccessLog = booleanSystemPropertyExtractionHelper("disableAccessLog", false)
def accessLogLevel = OFF
addInfo("******Access logs disabled: " + disableAccessLog)
println("******Access logs disabled: " + disableAccessLog)
logger("ACCESS_LOG", accessLogLevel, allAsyncAccessLogAppendersArray, false)

// Root logger.
root(INFO, allAsyncAppendersArray)

// Auto-scan this config file for changes and reload the logging config if it changes.
// NOTE: Due to performance concerns, the scanner is not only time based (which we set here), it is also sampled
//       so the time check only happens once every x logging attempts (x is dynamically determined by logback based
//       on how often the app logs). Both the sample check and the time check must pass before the log file will be
//       reprocessed. See http://logback.qos.ch/manual/configuration.html#autoScan for more details.
scan("60 seconds")

addInfo("...logback.groovy processing finished.")
println("...logback.groovy processing finished.")