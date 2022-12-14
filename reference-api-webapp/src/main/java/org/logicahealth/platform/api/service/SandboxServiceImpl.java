/**
 * * #%L
 * *
 * * %%
 * * Copyright (C) 2014-2020 Healthcare Services Platform Consortium
 * * %%
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 * *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 * * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * #L%
 */

package org.logicahealth.platform.api.service;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.logicahealth.platform.api.DatabaseProperties;
import org.logicahealth.platform.api.model.DataSet;
import org.logicahealth.platform.api.model.Sandbox;
import org.logicahealth.platform.api.multitenant.db.SandboxPersister;
import org.logicahealth.platform.api.multitenant.db.SchemaNotInitializedException;
import org.logicahealth.platform.api.multitenant.TenantInfoRequestMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class SandboxServiceImpl implements SandboxService {
    private static final Logger logger = LoggerFactory.getLogger(SandboxServiceImpl.class);
    public static final long WAIT_BEFORE_DELETION = 1000_000L;
    private static final String FHIR_SERVER_VERSION = "platformVersion";
    private static final String HAPI_VERSION = "hapiVersion";
    private static final String FHIR_VERSION = "fhirVersion";

    @Value("${hspc.platform.api.sandboxManagerApi.url}")
    private String sandboxManagerApiUrl;

    @Value("${hspc.platform.api.sandboxManagerApi.userAuthPath}")
    private String userAuthPath;

    @Value("${hspc.platform.api.sandboxManagerApi.exportImportAuthPath}")
    private String exportImportAuthPath;

    private SandboxPersister sandboxPersister;

    private TenantInfoRequestMatcher tenantInfoRequestMatcher;

    private RestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FhirContext fhirContext;

    @Autowired
    public SandboxServiceImpl(SandboxPersister sandboxPersister, TenantInfoRequestMatcher tenantInfoRequestMatcher,
                              RestTemplate restTemplate) {
        this.sandboxPersister = sandboxPersister;
        this.tenantInfoRequestMatcher = tenantInfoRequestMatcher;
        this.restTemplate = restTemplate;
    }

    @Override
    public void reset() {
        tenantInfoRequestMatcher.reset();
        logger.info("Sandbox Service reset");
    }

    @Override
    public Collection<String> allTenantNames() {
        return sandboxPersister.getSandboxNames();
    }

    @Override
    public Collection<Sandbox> allSandboxes() {
        return sandboxPersister.getSandboxes();
    }

    @Override
    public Sandbox save(@NotNull Sandbox sandbox, @NotNull DataSet dataSet) {
        logger.info("Saving sandbox: " + sandbox);
        Validate.notNull(sandbox, "Sandbox must be provided");
        Validate.notNull(sandbox.getTeamId(), "Sandbox.teamId must be provided");

        sandbox.setSchemaVersion(DatabaseProperties.DEFAULT_HSPC_SCHEMA_VERSION);


        Sandbox existing = checkIfTenantNameIsUnique(sandbox);

        // save the sandbox info
        Sandbox saved = sandboxPersister.saveSandbox(sandbox);
        logger.info("Saved sandbox: " + saved);

        logger.info("useStarterData: " + dataSet);
        if (existing == null) {
            sandboxPersister.loadInitialDataset(sandbox, dataSet);
        }

        // Make sure the initial data set didn't replace the sandbox info
        saved = sandboxPersister.saveSandbox(sandbox);
        logger.info("Saved sandbox: " + saved);

        // update security
        if (sandbox.isAllowOpenAccess()) {
            tenantInfoRequestMatcher.addOpenTeamId(saved.getTeamId());
        } else {
            tenantInfoRequestMatcher.removeOpenTeamId(saved.getTeamId());
        }

        return saved;
    }

    @Override
    public void clone(@NotNull Sandbox newSandbox, @NotNull Sandbox clonedSandbox) {
        logger.info("Cloning sandbox " + clonedSandbox.getTeamId() + " to sandbox: " + newSandbox.getTeamId());
        Validate.notNull(newSandbox, "New sandbox must be provided");
        Validate.notNull(newSandbox.getTeamId(), "New sandbox.teamId must be provided");
        Validate.notNull(clonedSandbox, "Cloned sandbox must be provided");
        Validate.notNull(clonedSandbox.getTeamId(), "Cloned sandbox.teamId must be provided");

        newSandbox.setSchemaVersion(DatabaseProperties.DEFAULT_HSPC_SCHEMA_VERSION);
        clonedSandbox.setSchemaVersion(DatabaseProperties.DEFAULT_HSPC_SCHEMA_VERSION);

        Sandbox existing = checkIfTenantNameIsUnique(newSandbox);

        if (existing == null) {
            sandboxPersister.cloneSandbox(newSandbox, clonedSandbox);
            if (newSandbox.isAllowOpenAccess()) {
                tenantInfoRequestMatcher.addOpenTeamId(newSandbox.getTeamId());
            } else {
                tenantInfoRequestMatcher.removeOpenTeamId(newSandbox.getTeamId());
            }
            return;
        }

        if (newSandbox.isAllowOpenAccess()) {
            tenantInfoRequestMatcher.addOpenTeamId(newSandbox.getTeamId());
        } else {
            tenantInfoRequestMatcher.removeOpenTeamId(newSandbox.getTeamId());
        }

        throw new IllegalArgumentException("The new sandbox already exists");
    }

    @Override
    public String sandboxSchemaDump(@NotNull Sandbox sandbox) {
        var sandboxDumpFilename = "dump" + sandbox.getTeamId() + UUID.randomUUID() + ".sql";
        logger.info("Dumping schema for sandbox " + sandbox.getTeamId() + " into " + sandboxDumpFilename);
        sandboxPersister.dumpSandboxSchema(sandbox, sandboxDumpFilename);
        return sandboxDumpFilename;
    }

    @Override
    public Sandbox get(String teamId) {
        Sandbox sandbox;
        try {
            sandbox = sandboxPersister.findSandbox(teamId);
        } catch (SchemaNotInitializedException e) {
            sandbox = save(SandboxPersister.sandboxTemplate().setTeamId(teamId), DataSet.DEFAULT);
        }

        return sandbox;
    }

    @Override
    public boolean remove(String teamId) {
        Sandbox existing = get(teamId);
        return (existing == null) || delete(existing);
    }

    private boolean delete(Sandbox existing) {
        if (existing != null) {
            boolean success = sandboxPersister.removeSandbox(existing.getSchemaVersion(), existing.getTeamId());
            // update security
            tenantInfoRequestMatcher.removeOpenTeamId(existing.getTeamId());
            return success;
        } else {
            return true;
        }
    }

    @Override
    public Sandbox reset(String teamId, DataSet dataSet) {
        Sandbox existing = get(teamId);

        if (existing == null) {
            throw new RuntimeException("Unable to reset sandbox because sandbox does not exist: [" + teamId + "]");
        } else {
            boolean deleted = delete(existing);
            if (!deleted) {
                throw new RuntimeException("Unable to reset sandbox because existing could not be deleted: [" + teamId + "]");
            }
        }

        return save(SandboxPersister.sandboxTemplate().setTeamId(teamId), dataSet);
    }

    private Sandbox checkIfTenantNameIsUnique(Sandbox sandbox) {
        try {
            Sandbox existing = sandboxPersister.findSandbox(sandbox.getTeamId());
            logger.info("Existing sandbox: " + existing);
            if (existing == null) {
                // check that the sandbox is unique across versions
                if (!sandboxPersister.isTeamIdUnique(sandbox.getTeamId())) {
                    throw new RuntimeException("TeamId [" + sandbox.getTeamId() + "] is not unique");
                }
            }
            return existing;
        } catch (SchemaNotInitializedException e) {
            logger.info("SchemaNotInitializedException ignored for now");
            // ignore, will be fixed when saving
        }
        return null;
    }

    @Override
    public boolean verifyUser(HttpServletRequest request, String sandboxId) {
        return verifyUserPermissions( request, sandboxId, this.userAuthPath);
    }

    @Override
    public boolean verifyUserCanExportImport(HttpServletRequest request, String sandboxId) {
        return verifyUserPermissions( request,  sandboxId,  this.exportImportAuthPath);
    }

    private boolean verifyUserPermissions(HttpServletRequest request, String sandboxId, String activity) {
        String authToken = getBearerToken(request);
        if (authToken == null) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "BEARER " + authToken);

        String jsonBody = "{\"sandbox\": \"" + sandboxId + "\"}";

        HttpEntity entity = new HttpEntity(jsonBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(this.sandboxManagerApiUrl + activity, HttpMethod.POST, entity, String.class);
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    @Override
    @Async("taskExecutor")
    @Retryable
    public void deleteSchemaDump(String dumpFileName) {
        try {
            Thread.sleep(WAIT_BEFORE_DELETION);
            String delete = "rm ./" + dumpFileName;
            String[] cmdarray = {"/bin/sh", "-c", delete};
            Process pr = Runtime.getRuntime().exec(cmdarray);
            Integer outcome4 = pr.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            var error = IOUtils.toString(in);
            if (outcome4 == 0) {
                logger.info(dumpFileName + " file deleted.");
            } else {
                logger.info("Error deleting " + dumpFileName);
                throw new RuntimeException();
            }
        } catch (InterruptedException | IOException e) {
            logger.info("Error deleting " + dumpFileName, e);
            throw new RuntimeException();
        }

    }

    @Override
    public void writeZipFileToResponse(ZipOutputStream zipOutputStream, String dumpFileName) {
        try {
            var byteArrayInputStream = new ByteArrayInputStream(hapiAndSandboxVersions().getBytes());
            addZipFileEntry(byteArrayInputStream, new ZipEntry("versions.json"), zipOutputStream);
            byteArrayInputStream.close();
            byteArrayInputStream = new ByteArrayInputStream(getSHA256Hash(dumpFileName).getBytes());
            addZipFileEntry(byteArrayInputStream, new ZipEntry("hash"), zipOutputStream);
            byteArrayInputStream.close();
            var fileInputStream = new FileInputStream(new File("./" + dumpFileName));
            addZipFileEntry(fileInputStream, new ZipEntry("sandbox.sql"), zipOutputStream);
            fileInputStream.close();
            zipOutputStream.close();
        } catch (IOException e) {
            logger.error("Exception while zipping schema dump and versions", e);
            throw new RuntimeException();
        }
    }

    @Override
    public void importSandboxSchema(File schemaFile, Sandbox sandbox, String hapiVersion) {
        checkImportHapiVersionMatchesCurrent(hapiVersion);
        sandboxPersister.importSandboxSchema(schemaFile, sandbox);
    }

    private void addZipFileEntry(InputStream inputStream, ZipEntry zipEntry, ZipOutputStream zipOutputStream) {
        try {
            zipOutputStream.putNextEntry(zipEntry);
            IOUtils.copyLarge(inputStream, zipOutputStream);
        } catch (IOException e) {
            logger.error("Exception while adding zip entry", e);
            throw new RuntimeException();
        }
    }

    private void checkImportHapiVersionMatchesCurrent(String importHapiVersion) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new FileReader("pom.xml"));
            var currentHapiVersion = model.getProperties().get("hapi.version").toString();
            if (!importHapiVersion.equals(currentHapiVersion)) {
                var error = "Imported file hapi version " + importHapiVersion + " does not match current hapi version " + currentHapiVersion;
                logger.error(error);
                throw new RuntimeException(error);
            }
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error while parsing pom file", e);
            throw new RuntimeException();
        }
    }

    private String hapiAndSandboxVersions() {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new FileReader("pom.xml"));
            var manifest = new HashMap<String, String>();
            manifest.put(FHIR_SERVER_VERSION, model.getVersion());
            manifest.put(HAPI_VERSION, model.getProperties().get("hapi.version").toString());
            manifest.put(FHIR_VERSION, fhirContext.getVersion().getVersion().name());
            return new Gson().toJson(manifest);
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error while parsing pom file", e);
            throw new RuntimeException();
        }
    }

    private String getSHA256Hash(String dumpFileName) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Exception while hashing schema dump", e);
            throw new RuntimeException();
        }
        try (
                var bufferedInputStream = new BufferedInputStream(new FileInputStream(new File("./" + dumpFileName)));
                var digestInputStream = new DigestInputStream(bufferedInputStream, messageDigest)
        ) {
            while (digestInputStream.read() != -1) ;
            return Hex.encodeHexString(messageDigest.digest());
        } catch (IOException e) {
            logger.error("Exception while hashing schema dump", e);
            throw new RuntimeException();
        }
    }

    private String getBearerToken(HttpServletRequest request) {

        String authToken = request.getHeader("Authorization");
        if (authToken == null) {
            return null;
        }
        return authToken.substring(7);
    }

}
