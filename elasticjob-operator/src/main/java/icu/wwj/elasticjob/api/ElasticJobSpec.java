package icu.wwj.elasticjob.api;

import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ElasticJobSpec {
    
    @Required
    private PodTemplateSpec template;
    
    private JobExecutionType jobExecutionType;
    
    @Required
    @Min(1)
    private int shardingTotalCount;
    
    private Map<String, String> shardingItemParameters = new LinkedHashMap<>();
    
    private String cron;
    
    private boolean misfire;
    
    private Map<String, String> props = new LinkedHashMap<>();
    
    private String description;
    
    private boolean disabled;
}
