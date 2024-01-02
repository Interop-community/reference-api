package org.logicahealth.platform.api.bulk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.jpa.bulk.api.IBulkDataExportSvc;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.logicahealth.platform.api.multitenant.tenantid.UrlPathTenantIdentifierResolver; //for multitenant awareness in scheduler's job

@Service
public class BulkExportJobRunnerService {

    private static final Logger ourLog = LoggerFactory.getLogger(BulkExportJobRunnerService.class);

    @Autowired
    private UrlPathTenantIdentifierResolver urlPathTenantIdentifierResolver;

    @Autowired
    private IBulkDataExportSvc myBulkDataExportSvc;

    private BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    @Async("taskExecutor")
    public void runJob(String tenant) {
        ourLog.info(" Async call to BulkExportJobRunnerService.runJob in {}.", Thread.currentThread().getName());
        queue.add(tenant);
    }

    @PostConstruct
    public void start() {
        new Thread(() -> { 
            Thread.currentThread().setName("BulkExportJobRunnerService");
            init();
            }).start();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void init() {
        try {
            while (true) {
                String t = queue.take();
                ourLog.info("Starting BulkExportJobRunnerService.runJob");
                urlPathTenantIdentifierResolver.setTenantForScheduledTasks(t);

                myBulkDataExportSvc.purgeExpiredFiles();
                myBulkDataExportSvc.buildExportFiles();

                ourLog.info("Finished BulkExportJobRunnerService.runJob");
            }
        } catch (InterruptedException e) {
            ourLog.error("Exception while BulkExportJobRunnerService.runJob from Queue.", e);
        }
    }

}
