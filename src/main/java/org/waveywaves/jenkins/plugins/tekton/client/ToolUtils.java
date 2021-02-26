/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.waveywaves.jenkins.plugins.tekton.client;

import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class for accessing the jx-pipeline-effective binary
 */
public class ToolUtils {
    private static final Logger logger = Logger.getLogger(ToolUtils.class.getName());

    public static String jxPipelineFile;

    /**
     * @return the file name location of the jx-pipeline-effective binary
     * @throws IOException
     */
    public static synchronized String getJXPipelineBinary() throws IOException {
        if (jxPipelineFile == null) {
            File f = File.createTempFile("jx-pipeline-effective-", "");
            f.delete();
            f.deleteOnExit();

            String platform = "linux";
            if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
                platform = "mac";
            } else if (SystemUtils.IS_OS_WINDOWS) {
                platform = "windows";
            }

            String resource = "org/waveywaves/jenkins/plugins/tekton/client/jxp/" + platform + "/jx-pipeline-effective";
            InputStream in = ToolUtils.class.getClassLoader().getResourceAsStream(resource);
            if (in == null) {
                throw new IOException("could not find resource on classpath: " + resource);
            }

            String path = f.getPath();
            try {
                Files.copy(in, f.toPath());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "failed to copy jx-pipeline-effective to " + path + " due to " + e);
                throw new IOException("failed to copy jx-pipeline-effective to " + path + " cause: " + e, e);
            }

            try {
                f.setExecutable(true);
            } catch (Exception e) {
                throw new IOException("failed make the file executable: " + path + " cause: " + e, e);
            }
            jxPipelineFile = path;

            logger.info("saved jx-pipeline-effective binary to " + jxPipelineFile);
        }
        return jxPipelineFile;
    }
}
