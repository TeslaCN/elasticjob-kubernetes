package icu.wwj.elasticjob.sdk.executor;

import com.google.common.base.Preconditions;
import icu.wwj.elasticjob.cloud.common.pojo.CloudJobConfigurationPOJO;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.apache.shardingsphere.elasticjob.infra.yaml.YamlEngine;
import org.apache.shardingsphere.elasticjob.tracing.JobTracingEventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;

public final class KubernetesCloudJobExecutor {
    
    private static final String CONFIG_FILE = "/etc/elasticjob/config";
    
    private static final String SHARDING_ITEM_FILE = "/etc/elasticjob/sharding-item";
    
    private static final String SHARDING_CONTEXT_DIR = "/etc/elasticjob/sharding-context";
    
    private final ElasticJobExecutor elasticJobExecutor;
    
    public KubernetesCloudJobExecutor(ElasticJob elasticJob) {
        JobConfiguration jobConfiguration = loadJobConfiguration();
        ShardingContexts shardingContexts = loadShardingContexts(jobConfiguration);
        elasticJobExecutor = new ElasticJobExecutor(elasticJob, jobConfiguration, new KubernetesCloudJobFacade(shardingContexts, jobConfiguration, new JobTracingEventBus()));
    }
    
    public KubernetesCloudJobExecutor(String jobType) {
        JobConfiguration jobConfiguration = loadJobConfiguration();
        ShardingContexts shardingContexts = loadShardingContexts(jobConfiguration);
        elasticJobExecutor = new ElasticJobExecutor(jobType, jobConfiguration, new KubernetesCloudJobFacade(shardingContexts, jobConfiguration, new JobTracingEventBus()));
    }
    
    private JobConfiguration loadJobConfiguration() {
        return YamlEngine.unmarshal(readFile(CONFIG_FILE), CloudJobConfigurationPOJO.class).toJobConfiguration();
    }
    
    private ShardingContexts loadShardingContexts(JobConfiguration jobConfiguration) {
        int shardingItem = Integer.parseInt(readFile(SHARDING_ITEM_FILE));
        // TODO Fix the failure of unmarshal
        ShardingContext shardingContext = YamlEngine.unmarshal(readFile(SHARDING_CONTEXT_DIR + "/" + shardingItem), ShardingContext.class);
        return new ShardingContexts("", jobConfiguration.getJobName(), jobConfiguration.getShardingTotalCount(), jobConfiguration.getJobParameter(), Collections.singletonMap(shardingItem, shardingContext.getShardingParameter()));
    }
    
    private String readFile(String filePath) {
        File configFile = Paths.get(filePath).toFile();
        Preconditions.checkState(configFile.exists(), "File [%s] not found. Please make sure that the job is scheduled by ElasticJob Cloud Operator.", filePath);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            return new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed to load job configuration from file [%s]. Please check its content.", filePath), ex);
        }
    }
    
    public void execute() {
        elasticJobExecutor.execute();
    }
}
