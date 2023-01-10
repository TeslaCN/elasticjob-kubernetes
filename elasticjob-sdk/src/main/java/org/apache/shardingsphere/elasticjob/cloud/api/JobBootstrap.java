package org.apache.shardingsphere.elasticjob.cloud.api;

import icu.wwj.elasticjob.sdk.executor.KubernetesCloudJobExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobBootstrap {
   
    public static void execute(final ElasticJob elasticJob) {
        new KubernetesCloudJobExecutor(elasticJob).execute();
    }
    
    public static void execute(final String elasticJobType) {
        new KubernetesCloudJobExecutor(elasticJobType).execute();
    }
}
