package icu.wwj.elasticjob.sdk.environment;

import com.google.common.base.Preconditions;
import icu.wwj.elasticjob.cloud.common.pojo.CloudJobConfigurationPOJO;
import icu.wwj.elasticjob.cloud.common.pojo.ShardingContextPOJO;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.apache.shardingsphere.elasticjob.infra.yaml.YamlEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public final class KubernetesEnvironmentLoader {
    
    private static final String CONFIG_FILE = "/config";
    
    private static final String SHARDING_CONTEXT_DIR = "/sharding-context";
    
    private static final String SHARDING_ITEM_FILE = "/sharding-item";
    
    private static final String STATEFULSET_POD_NAME_FILE = "/statefulset-pod-name";
    
    private static final Pattern STATEFULSET_POD_NAME_PATTERN = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?-(?<podIndex>\\d+)");
    
    private final String baseDir;
    
    public JobConfiguration loadJobConfiguration() {
        return YamlEngine.unmarshal(readFile(CONFIG_FILE), CloudJobConfigurationPOJO.class).toJobConfiguration();
    }
    
    public ShardingContexts loadShardingContexts(JobConfiguration jobConfiguration) {
        int shardingItem = loadShardingItem();
        ShardingContext shardingContext = YamlEngine.unmarshal(readFile(SHARDING_CONTEXT_DIR + "/" + shardingItem), ShardingContextPOJO.class).toShardingContext();
        return new ShardingContexts("", jobConfiguration.getJobName(), jobConfiguration.getShardingTotalCount(), jobConfiguration.getJobParameter(), Collections.singletonMap(shardingItem, shardingContext.getShardingParameter()));
    }
    
    public int loadShardingItem() {
        String shardingItemFileContent = readFile(SHARDING_ITEM_FILE);
        if (!shardingItemFileContent.isBlank()) {
            return Integer.parseInt(shardingItemFileContent);
        }
        String podNameFileContent = readFile(STATEFULSET_POD_NAME_FILE);
        Matcher matcher = STATEFULSET_POD_NAME_PATTERN.matcher(podNameFileContent);
        Preconditions.checkState(matcher.matches(), "Could not determine sharding item from sharding-item or statefulset-pod-name file");
        return Integer.parseInt(matcher.group("podIndex"));
    }
    
    public boolean isTransient() {
        return readFile(STATEFULSET_POD_NAME_FILE).isBlank();
    }
    
    private String readFile(String filePath) {
        File configFile = Paths.get(baseDir, filePath).toFile();
        Preconditions.checkState(configFile.exists(), "File [%s] not found. Please make sure that the job is scheduled by ElasticJob Cloud Operator.", filePath);
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            return new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Failed to load job configuration from file [%s]. Please check its content.", filePath), ex);
        }
    }
}
