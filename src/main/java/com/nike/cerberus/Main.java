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

package com.nike.cerberus;

import com.nike.riposte.server.config.ServerConfig;
import com.nike.riposte.typesafeconfig.TypesafeConfigServer;
import com.nike.cerberus.server.config.CmsConfig;
import com.typesafe.config.Config;

/**
 * Main class entry point for this app. Sets up Typesafe Config and initializes a new {@link com.nike.riposte.server.Server}
 * with the application's {@link com.nike.riposte.server.config.ServerConfig}.
 *
 * @author Nic Munroe
 */
public class Main extends TypesafeConfigServer {

    @Override
    protected ServerConfig getServerConfig(Config appConfig) {
        return new CmsConfig(appConfig);
    }

    public static void main(String[] args) throws Exception {
        new Main().launchServer(args);
    }
}