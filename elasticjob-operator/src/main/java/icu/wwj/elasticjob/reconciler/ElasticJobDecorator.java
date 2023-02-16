package icu.wwj.elasticjob.reconciler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import icu.wwj.elasticjob.api.ElasticJob;
import icu.wwj.elasticjob.api.ElasticJobSpec;
import icu.wwj.elasticjob.cloud.common.pojo.CloudJobConfigurationPOJO;
import icu.wwj.elasticjob.cloud.common.pojo.ShardingContextPOJO;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFile;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFileBuilder;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ElasticJobDecorator {
    
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @SneakyThrows(JsonProcessingException.class)
    public static PodTemplateSpec getDecoratedPodTemplate(ElasticJob elasticJob) {
        PodTemplateSpec copiedTemplate = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(elasticJob.getSpec().getTemplate()), PodTemplateSpec.class);
        setElasticJobLabel(copiedTemplate, elasticJob);
        setElasticJobAnnotations(copiedTemplate, elasticJob);
        mountElasticJobConfiguration(copiedTemplate, elasticJob.getSpec().getShardingTotalCount());
        return copiedTemplate;
    }
    
    private static void setElasticJobLabel(final PodTemplateSpec target, final ElasticJob elasticJob) {
        target.getMetadata().getLabels().put(Constants.ELASTICJOB_LABEL_APP, elasticJob.getMetadata().getName());
    }
    
    private static void setElasticJobAnnotations(final PodTemplateSpec target, final ElasticJob elasticJob) throws JsonProcessingException {
        target.getMetadata().getAnnotations().put(Constants.ELASTICJOB_ANNOTATION_CONFIG, OBJECT_MAPPER.writeValueAsString(toCloudJobConfigurationPOJO(elasticJob)));
        for (int shardingItem = 0; shardingItem < elasticJob.getSpec().getShardingTotalCount(); shardingItem++) {
            ShardingContextPOJO shardingContext = new ShardingContextPOJO(new ShardingContext(elasticJob.getMetadata().getName(), "", elasticJob.getSpec().getShardingTotalCount(), elasticJob.getSpec().getJobParameter(), shardingItem, elasticJob.getSpec().getShardingItemParameters().getOrDefault("" + shardingItem, "")));
            target.getMetadata().getAnnotations().put(Constants.ELASTICJOB_DOMAIN + Constants.ELASTICJOB_SHARDING_CONTEXT_PREFIX + shardingItem, OBJECT_MAPPER.writeValueAsString(shardingContext));
        }
    }
    
    private static CloudJobConfigurationPOJO toCloudJobConfigurationPOJO(final ElasticJob elasticJob) {
        ElasticJobSpec spec = elasticJob.getSpec();
        CloudJobConfigurationPOJO result = new CloudJobConfigurationPOJO();
        result.setJobName(elasticJob.getMetadata().getName());
        result.setShardingTotalCount(spec.getShardingTotalCount());
        result.setCron(spec.getCron());
        result.setShardingItemParameters(spec.getShardingItemParameters());
        result.setJobParameter(spec.getJobParameter());
        result.setFailover(spec.isFailover());
        result.setMisfire(spec.isMisfire());
        result.setJobErrorHandlerType(spec.getJobErrorHandlerType());
        result.setDescription(spec.getDescription());
        result.setProps(new LinkedHashMap<>(spec.getProps()));
        result.setDisabled(spec.isDisabled());
        return result;
    }
    
    private static void mountElasticJobConfiguration(final PodTemplateSpec target, final int shardingTotalCount) {
        target.getSpec().getVolumes().add(new VolumeBuilder().withName("elasticjob")
                .withDownwardAPI(new DownwardAPIVolumeSourceBuilder()
                        .withItems(IntStream.range(0, shardingTotalCount).mapToObj(ElasticJobDecorator::mountShardingContext).collect(Collectors.toList()))
                        .addToItems(
                                new DownwardAPIVolumeFileBuilder().withPath("config").withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.annotations['" + Constants.ELASTICJOB_ANNOTATION_CONFIG + "']").build()).build(),
                                new DownwardAPIVolumeFileBuilder().withPath("sharding-item").withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.annotations['batch.kubernetes.io/job-completion-index']").build()).build(),
                                new DownwardAPIVolumeFileBuilder().withPath("statefulset-pod-name").withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.labels['statefulset.kubernetes.io/pod-name']").build()).build()
                        )
                        .build())
                .build());
        for (Container each : target.getSpec().getContainers()) {
            each.getVolumeMounts().add(new VolumeMountBuilder().withName("elasticjob").withMountPath("/etc/elasticjob").build());
        }
    }
    
    private static DownwardAPIVolumeFile mountShardingContext(int shardingItem) {
        return new DownwardAPIVolumeFileBuilder().withPath("sharding-context/" + shardingItem).withFieldRef(new ObjectFieldSelectorBuilder()
                .withFieldPath("metadata.annotations['" + Constants.ELASTICJOB_DOMAIN + Constants.ELASTICJOB_SHARDING_CONTEXT_PREFIX + shardingItem + "']").build()).build();
    }
}
