package icu.wwj.elasticjob.cloud.common.pojo;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class CloudJobConfigurationPOJO {
    
    private String jobName;
    
    private String cron;
    
    private String timeZone;
    
    private int shardingTotalCount;
    
    private Map<String, String> shardingItemParameters = new LinkedHashMap<>();
    
    private String jobParameter;
    
    private boolean failover;
    
    private boolean misfire;
    
    private String jobExecutorServiceHandlerType;
    
    private String jobErrorHandlerType;
    
    private Collection<String> jobListenerTypes = new ArrayList<>();
    
    private String description;
    
    private Map<String, String> props = new LinkedHashMap<>();
    
    private boolean disabled;
    
    public JobConfiguration toJobConfiguration() {
        JobConfiguration result = JobConfiguration.newBuilder(jobName, shardingTotalCount)
                .cron(cron)
                .timeZone(timeZone)
                .shardingItemParameters(shardingItemParameters.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")))
                .jobParameter(jobParameter)
                .failover(failover)
                .misfire(misfire)
                .jobExecutorServiceHandlerType(jobExecutorServiceHandlerType)
                .jobErrorHandlerType(jobErrorHandlerType)
                .jobListenerTypes(jobListenerTypes.toArray(new String[]{}))
                .description(description)
                .disabled(disabled).build();
        props.forEach(result.getProps()::setProperty);
        return result;
    }
    
    public static CloudJobConfigurationPOJO fromJobConfiguration(JobConfiguration jobConfiguration) {
        CloudJobConfigurationPOJO result = new CloudJobConfigurationPOJO();
        result.setJobName(jobConfiguration.getJobName());
        result.setCron(jobConfiguration.getCron());
        result.setTimeZone(jobConfiguration.getTimeZone());
        result.setShardingTotalCount(jobConfiguration.getShardingTotalCount());
        result.setShardingItemParameters(parseShardingItemParameters(jobConfiguration.getShardingItemParameters()));
        result.setJobParameter(jobConfiguration.getJobParameter());
        result.setFailover(jobConfiguration.isFailover());
        result.setMisfire(jobConfiguration.isMisfire());
        result.setJobExecutorServiceHandlerType(jobConfiguration.getJobExecutorServiceHandlerType());
        result.setJobErrorHandlerType(jobConfiguration.getJobErrorHandlerType());
        result.setJobListenerTypes(jobConfiguration.getJobListenerTypes());
        result.setDescription(jobConfiguration.getDescription());
        result.setProps(jobConfiguration.getProps().stringPropertyNames().stream().collect(Collectors.toMap(Function.identity(), k -> jobConfiguration.getProps().getProperty(k))));
        result.setDisabled(jobConfiguration.isDisabled());
        return result;
    }
    
    private static Map<String, String> parseShardingItemParameters(String shardingItemParameters) {
        if (null == shardingItemParameters || shardingItemParameters.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        Arrays.stream(shardingItemParameters.split(",")).forEach(s -> {
            String[] split = s.split("=", 2);
            Preconditions.checkArgument(2 == split.length, "Invalid sharding item paremeter [%s]", s);
            result.put(split[0], split[1]);
        });
        return result;
    }
}
