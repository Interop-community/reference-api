package org.hspconsortium.platform.api.fhir.service;

import org.hspconsortium.platform.api.fhir.model.DataSet;
import org.hspconsortium.platform.api.fhir.model.Sandbox;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Set;

public interface SandboxService {
    void reset();

    Collection<String> all();

    Sandbox save(@NotNull Sandbox sandbox, @NotNull DataSet dataSet);

    Sandbox get(String teamId);

    boolean remove(String teamId);

    Sandbox reset(String teamId, DataSet dataSet);

    Set<String> getSandboxSnapshots(String teamId);

    String takeSnapshot(String teamId, String snapshotId);

    String restoreSnapshot(String teamId, String snapshotId);

    String deleteSnapshot(String teamId, String snapshotId);
}