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
        try{
                        
            ourLog.info("Purging expired files before building export files");
            final var purger = new Thread(()-> myBulkDataExportSvc.purgeExpiredFiles());
            purger.start();
            purger.join();
            ourLog.info("Purging expired files finished");
			
            ourLog.info("Building export files..");
			final var jobRunner = new Thread(() -> myBulkDataExportSvc.buildExportFiles());
			jobRunner.start();
			jobRunner.join();
            ourLog.info("Building export files finished");
		} catch (InterruptedException e) {
			ourLog.error("Exception while BulkExportJobRunnerService.runJob", e);

		}        ourLog.info("Finished BulkExportJobRunnerService.runJob");

    }

}
