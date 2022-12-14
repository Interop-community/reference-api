/**
 *  * #%L
 *  *
 *  * %%
 *  * Copyright (C) 2014-2020 Healthcare Services Platform Consortium
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 */

package org.logicahealth.platform.api.service;

import org.logicahealth.platform.api.model.DataSet;
import org.logicahealth.platform.api.model.Sandbox;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Collection;
import java.util.zip.ZipOutputStream;

public interface SandboxService {
    void reset();

    Collection<String> allTenantNames();

    Collection<Sandbox> allSandboxes();

    Sandbox save(@NotNull Sandbox sandbox, @NotNull DataSet dataSet);

    void clone(@NotNull Sandbox newSandbox, @NotNull Sandbox clonedSandbox);

    String sandboxSchemaDump(@NotNull Sandbox sandbox);

    Sandbox get(String teamId);

    boolean remove(String teamId);

    Sandbox reset(String teamId, DataSet dataSet);

    boolean verifyUser(HttpServletRequest request, String sandboxId);

    boolean verifyUserCanExportImport(HttpServletRequest request, String sandboxId);

    void deleteSchemaDump(String dumpFileName);

    void writeZipFileToResponse(ZipOutputStream zipOutputStream, String dumpFileName);

    void importSandboxSchema(File schemaFile, Sandbox sandbox, String hapiVersion);

}
