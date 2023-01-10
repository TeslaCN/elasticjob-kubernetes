package icu.wwj.elasticjob.api;

import io.fabric8.generator.annotation.Min;
import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
public class ElasticJobSpec {
    
    @Required
    private PodTemplateSpec template;
    
    @Required
    private JobExecutionType jobExecutionType;
    
    @Required
    @Min(1)
    private int shardingTotalCount;
    
    private Map<String, String> shardingItemParameters = new LinkedHashMap<>();
    
    private String jobParameter;
    
    private String cron;
    
    private boolean misfire;
    
    private Map<String, String> props = new LinkedHashMap<>();
    
    private String description;
    
    private boolean disabled;
}
