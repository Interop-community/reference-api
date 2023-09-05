package org.logicahealth.platform.api.bulk;


import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.logicahealth.platform.api.bulk.BulkDataExportSvcImpl;

@Service
public class BulkExportJobRunnerService {
 
    private static final Logger ourLog = LoggerFactory.getLogger(BulkExportJobRunnerService.class);


    @Async("taskExecutor")
    public void runJob(BulkDataExportSvcImpl myBulkDataExportSvc) {
        ourLog.info("Starting BulkExportJobRunnerService.runJob");
                        
        ourLog.info("Purging expired files before building export files");
        myBulkDataExportSvc.purgeExpiredFiles();
        ourLog.info("Purging expired files finished");
        
        ourLog.info("Building export files..");
        myBulkDataExportSvc.buildExportFiles();
        ourLog.info("Building export files finished");

        ourLog.info("Finished BulkExportJobRunnerService.runJob");

    }

}
