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

package org.logicahealth.platform.api.controller;

import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.util.UrlUtil;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.logicahealth.platform.api.DatabaseProperties;
import org.logicahealth.platform.api.model.DataSet;
import org.logicahealth.platform.api.model.ResetSandboxCommand;
import org.logicahealth.platform.api.model.Sandbox;
import org.logicahealth.platform.api.service.SandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("${hspc.platform.api.sandboxPath:/{teamId}/sandbox}")
public class MultitenantSandboxController {

    private static final Logger logger = LoggerFactory.getLogger(MultitenantSandboxController.class);

    private SandboxService sandboxService;

    @Autowired
    public MultitenantSandboxController(SandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Sandbox save(HttpServletRequest request, @PathVariable String teamId, @NotNull @RequestBody Sandbox sandbox,
                        @RequestParam(value = "dataSet", required = false) DataSet dataSet) {
        Validate.notNull(sandbox);
        Validate.notNull(sandbox.getTeamId());

//        don't validate the name to allow for initialization
//        validate(teamId);

        Validate.isTrue(teamId.equals(sandbox.getTeamId()));
        if (!sandboxService.verifyUser(request, sandbox.getTeamId())) {
            throw new UnauthorizedUserException("User not authorized to create/update sandbox " + sandbox.getTeamId());
        }
        if (dataSet == null) {
            dataSet = DataSet.NONE;
        }
        return sandboxService.save(sandbox, dataSet);
    }

    @RequestMapping(path = "/clone", method = RequestMethod.PUT)
    public Sandbox clone(HttpServletRequest request, @NotNull @RequestBody HashMap<String, Sandbox> sandboxes) {
        Sandbox newSandbox = sandboxes.get("newSandbox");
        Sandbox clonedSandbox = sandboxes.get("clonedSandbox");
        Validate.notNull(newSandbox);
        Validate.notNull(newSandbox.getTeamId());
        Validate.notNull(clonedSandbox);
        Validate.notNull(clonedSandbox.getTeamId());
        if (!sandboxService.verifyUser(request, clonedSandbox.getTeamId())) {
            throw new UnauthorizedUserException("User not authorized to clone sandbox " + clonedSandbox.getTeamId());
        }
        sandboxService.clone(newSandbox, clonedSandbox);
        return newSandbox;
    }

    @GetMapping(value = "/download/{teamId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadSandbox(HttpServletRequest request, @PathVariable("teamId") String teamId, final HttpServletResponse response) throws IOException {
        if (!sandboxService.verifyUserCanExportImport(request, teamId)) {
            throw new UnauthorizedUserException("User not authorized to export sandbox " + teamId);
        }
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=sandbox.zip");
        var dumpFileName = sandboxService.sandboxSchemaDump(sandboxService.get(teamId));
        var zipOutputStream = new ZipOutputStream(response.getOutputStream());
        sandboxService.writeZipFileToResponse(zipOutputStream, dumpFileName);
        sandboxService.deleteSchemaDump(dumpFileName);
        response.flushBuffer();
    }

    @PostMapping(value = "/import/{teamId}/{hapiVersion}")
    @ResponseStatus(HttpStatus.CREATED)
    @Consumes("multipart/form-data")
    public void importSandboxSchema(HttpServletRequest request, @RequestParam("schema") MultipartFile multipartFile, @PathVariable("teamId") String teamId, @PathVariable("hapiVersion") String hapiVersion) {
        if (!sandboxService.verifyUserCanExportImport(request, teamId)) {
            throw new UnauthorizedUserException("User not authorized to import sandbox " + teamId);
        }
        var schemaFile = new File(teamId + UUID.randomUUID() + ".sql");
        try {

            FileUtils.copyInputStreamToFile(multipartFile.getInputStream(), schemaFile);
            var sandbox = new Sandbox(teamId);
            sandbox.setSchemaVersion(DatabaseProperties.DEFAULT_HSPC_SCHEMA_VERSION);
            sandboxService.importSandboxSchema(schemaFile, sandbox, hapiVersion);
            sandboxService.deleteSchemaDump(schemaFile.getName());
        } catch (IOException e) {
            throw new RuntimeException("Exception while creating schema file for sandbox " + teamId);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public Sandbox get(HttpServletRequest request, @PathVariable String teamId) {
        Sandbox existing = sandboxService.get(teamId);
        if (existing == null) {
            throw new ResourceNotFoundException("Sandbox [" + teamId + "] is not found");
        }
        return existing;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public boolean delete(HttpServletRequest request, @PathVariable String teamId) {
        validate(teamId);
        if (!sandboxService.verifyUser(request, teamId)) {
            throw new UnauthorizedUserException("User not authorized to delete sandbox " + teamId);
        }
        return sandboxService.remove(teamId);
    }

    @RequestMapping(path = "/reset", method = RequestMethod.POST)
    public String reset(HttpServletRequest request, @PathVariable String teamId, @RequestBody ResetSandboxCommand resetSandboxCommand) {
        if (!sandboxService.verifyUser(request, teamId)) {
            throw new UnauthorizedUserException("User not authorized to reset sandbox " + teamId);
        }
        sandboxService.reset(teamId, resetSandboxCommand.getDataSet());
        return "Success";
    }

    @GetMapping("/echo/**")
    public @ResponseBody
    ResponseEntity<String> echoGet(HttpServletRequest request) {
        String message = "Received " + request.getMethod() + ", request path: " + request.getRequestURI();
        logger.info(message);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PostMapping("/echo/**")
    public @ResponseBody
    ResponseEntity<String> echoPost(HttpServletRequest request, @RequestBody Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("Received " + request.getMethod() + ", request path: " + request.getRequestURI());
        sb.append("Received POST request body: " + obj);
        logger.info(sb.toString());

        return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
    }

    @PutMapping("/echo/**")
    public @ResponseBody
    ResponseEntity<String> echoPut(HttpServletRequest request, @RequestBody Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("Received " + request.getMethod() + ", request path: " + request.getRequestURI());
        sb.append("Received PUT request body: " + obj);
        logger.info(sb.toString());

        return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
    }

    private void validate(String teamId) {
        try {
            SandboxReservedName.valueOf(teamId);
            // oh no, this is a reserved name
            throw new ForbiddenOperationException("Sandbox [" + teamId + "] is not allowed.");
        } catch (IllegalArgumentException e) {
            // good, not a reserved name
        }
    }

    // TODO: remove after migration to 3.6.0

    private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(0);
    private static final byte[] DELIMITER_BYTES = "|".getBytes(Charsets.UTF_8);

    @RequestMapping(path = "/hash/{values}", method = RequestMethod.GET)
    public long hash(@PathVariable("values") String values) {
        Hasher hasher = HASH_FUNCTION.newHasher();
        List<String> valueList = new ArrayList<String>(Arrays.asList(values.split(",")));
        for (String next : valueList) {
            if (next == null) {
                hasher.putByte((byte) 0);
            } else {
                next = UrlUtil.escapeUrlParam(next);
                byte[] bytes = next.getBytes(Charsets.UTF_8);
                hasher.putBytes(bytes);
            }
            hasher.putBytes(DELIMITER_BYTES);
        }

        HashCode hashCode = hasher.hash();
        return hashCode.asLong();
    }

}
