package icu.wwj.elasticjob.sdk.executor;

import com.google.common.base.Preconditions;
import icu.wwj.elasticjob.cloud.common.pojo.CloudJobConfigurationPOJO;
import icu.wwj.elasticjob.cloud.common.pojo.ShardingContextPOJO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KubernetesCloudJobExecutor {
    
    private static final String CONFIG_FILE = "/etc/elasticjob/config";
    
    private static final String SHARDING_ITEM_FILE = "/etc/elasticjob/sharding-item";
    
    private static final String POD_NAME_FILE = "/etc/elasticjob/pod-name";
    
    private static final Pattern STATEFUL_SET_POD_NAME_PATTERN = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?-(?<podIndex>\\d+)");
    
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
        int shardingItem = getShardingItem();
        ShardingContext shardingContext = YamlEngine.unmarshal(readFile(SHARDING_CONTEXT_DIR + "/" + shardingItem), ShardingContextPOJO.class).toShardingContext();
        return new ShardingContexts("", jobConfiguration.getJobName(), jobConfiguration.getShardingTotalCount(), jobConfiguration.getJobParameter(), Collections.singletonMap(shardingItem, shardingContext.getShardingParameter()));
    }
    
    private int getShardingItem() {
        String shardingItemFileContent = readFile(SHARDING_ITEM_FILE);
        if (!shardingItemFileContent.isBlank()) {
            return Integer.parseInt(shardingItemFileContent);
        }
        String podNameFileContent = readFile(POD_NAME_FILE);
        Matcher matcher = STATEFUL_SET_POD_NAME_PATTERN.matcher(podNameFileContent);
        Preconditions.checkState(matcher.matches(), "Could not determine sharding item from sharding-item or pod-name file");
        return Integer.parseInt(matcher.group("podIndex"));
    }
    
    private String readFile(String filePath) {
        File configFile = Paths.get(filePath).toFile();
        Preconditions.checkState(configFile.exists(), "File [%s] not found. Please make sure that the job is scheduled by ElasticJob Cloud Operator.", filePath);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            return new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Failed to load job configuration from file [%s]. Please check its content.", filePath), ex);
        }
    }
    
    public void execute() {
        elasticJobExecutor.execute();
    }
}
