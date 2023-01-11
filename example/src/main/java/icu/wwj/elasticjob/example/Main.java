package icu.wwj.elasticjob.example;

import icu.wwj.elasticjob.example.job.MyLogSimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.cloud.api.JobBootstrap;
import org.apache.shardingsphere.elasticjob.cloud.executor.local.LocalTaskExecutor;

import java.util.Arrays;

@Slf4j
public class Main {
    
    public static void main(String[] args) {
        log.info("Launching {}", Arrays.toString(args));
        if (isScheduledByOperator()) {
            JobBootstrap.execute(new MyLogSimpleJob());
        } else {
            new LocalTaskExecutor(new MyLogSimpleJob(), JobConfiguration.newBuilder("example-job", 3).cron("0 * * * * ?").build(), 0).execute();
        }
    }
    
    private static boolean isScheduledByOperator() {
        return false;
    }
}
