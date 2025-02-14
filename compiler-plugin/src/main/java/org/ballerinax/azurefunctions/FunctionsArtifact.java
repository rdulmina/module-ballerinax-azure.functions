/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.azurefunctions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.ballerina.projects.Project;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the output artifact (.zip) generated for Azure Functions.
 */
public class FunctionsArtifact {

    protected Map<String, JsonObject> functions;
    protected Path jarPath;
    protected Project project;

    protected Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FunctionsArtifact(Map<String, JsonObject> functions, Path jarPath, Project project) {
        this.functions = functions;
        this.jarPath = jarPath;
        this.project = project;
    }

    public Map<String, JsonObject> getFunctions() {
        return functions;
    }

    protected JsonObject readExistingHostJson() throws IOException {
        File file = new File(Constants.HOST_JSON_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), Constants.CHARSET))) {
                JsonParser parser = new JsonParser();
                return parser.parse(reader).getAsJsonObject();
            }
        } else {
            return null;
        }
    }

    protected JsonObject generateHostJson() throws IOException {
        JsonObject hostJson = readExistingHostJson();
        if (hostJson == null) {
            hostJson = new JsonObject();
        }
        hostJson.add("version", new JsonPrimitive("2.0"));
        JsonObject extensions = new JsonObject();
        JsonObject http = new JsonObject();
        http.addProperty("routePrefix", "");
        extensions.add("http", http);
        hostJson.add("extensions", extensions);
        JsonObject httpWorker = new JsonObject();
        hostJson.add("customHandler", httpWorker);
        JsonObject httpWorkerDesc = new JsonObject();
        httpWorker.add("description", httpWorkerDesc);
        httpWorkerDesc.add("defaultExecutablePath", new JsonPrimitive("java"));
        Path fileName = this.jarPath.getFileName();
        if (fileName != null) {
            httpWorkerDesc.add("defaultWorkerPath", new JsonPrimitive(fileName.toString()));
        }
        JsonArray workerArgs = new JsonArray();
        workerArgs.add("-jar");
        httpWorkerDesc.add("arguments", workerArgs);
        httpWorker.add("enableForwardingHttpRequest", new JsonPrimitive(false));
        JsonObject extensionBundle = new JsonObject();
        hostJson.add("extensionBundle", extensionBundle);
        extensionBundle.add("id", new JsonPrimitive("Microsoft.Azure.Functions.ExtensionBundle"));
        extensionBundle.add("version", new JsonPrimitive("[3.*, 4.0.0)"));
        return hostJson;
    }

    protected InputStream jtos(Object element) {
        try {
            return new ByteArrayInputStream(this.gson.toJson(element).getBytes(Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void generate() throws IOException {
        Path projectDir = Util.getProjectDir(project, jarPath);
        if (projectDir == null) {
            return;
        }

        Path functionsDir = Util.getAzureFunctionsDir(project, jarPath);
        Optional<String> cachedLocalSettings = cacheLocalSettings(functionsDir);
        Util.deleteDirectory(functionsDir);
        Files.createDirectories(functionsDir);
        generateExecutable(functionsDir);
        Files.copy(this.jtos(this.generateHostJson()), functionsDir.resolve(Constants.HOST_JSON_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        if (cachedLocalSettings.isEmpty()) {
            generateLocalSettings(functionsDir);
        } else {
            useCachedLocalSettings(functionsDir, cachedLocalSettings.get());
        }
        createFunctionArtifact(functionsDir);
    }

    protected void generateExecutable(Path functionsDir) throws IOException {
        Path jarFileName = this.jarPath.getFileName();
        Path azureFunctionsJar = functionsDir.resolve(jarFileName);
        Files.copy(this.jarPath, azureFunctionsJar, StandardCopyOption.REPLACE_EXISTING);
    }

    private void useCachedLocalSettings(Path functionsDir, String localSettings) throws IOException {
        ByteArrayInputStream inStream =
                new ByteArrayInputStream(localSettings.getBytes(StandardCharsets.UTF_8));
        Files.copy(inStream, functionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void createFunctionArtifact(Path functionsDir) throws IOException {
        for (Map.Entry<String, JsonObject> entry : this.functions.entrySet()) {
            Path functionDir = functionsDir.resolve(entry.getKey());
            Files.createDirectories(functionDir);
            Files.copy(this.jtos(entry.getValue()), functionDir.resolve(Constants.FUNCTION_JSON_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    protected void generateLocalSettings(Path azureFunctionsDir) throws IOException {
        Files.copy(jtos(new LocalSettings()), azureFunctionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
    }

    protected Optional<String> cacheLocalSettings(Path azureFunctionsDir) throws IOException {
        Path localSettingsPath = azureFunctionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME);
        if (localSettingsPath.toFile().exists()) {
            return Optional.of(Files.readString(localSettingsPath));
        }
        return Optional.empty();
    }
}

