package icu.wwj.elasticjob.example.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.elasticjob.simple.job.SimpleJob;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MyLogSimpleJob implements SimpleJob {
    
    @Override
    public void execute(final ShardingContext shardingContext) {
        log.info("Job launched {}", shardingContext);
        try {
            int timeout = new Random().nextInt(10, 30);
            log.info("Job finish after {} seconds {}", timeout, shardingContext);
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Job finished {}", shardingContext);
    }
}
