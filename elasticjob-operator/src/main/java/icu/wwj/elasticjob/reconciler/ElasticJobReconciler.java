package icu.wwj.elasticjob.reconciler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import icu.wwj.elasticjob.api.ElasticJob;
import icu.wwj.elasticjob.api.ElasticJobSpec;
import icu.wwj.elasticjob.api.ElasticJobStatus;
import icu.wwj.elasticjob.api.JobExecutionType;
import icu.wwj.elasticjob.cloud.common.pojo.CloudJobConfigurationPOJO;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFile;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeFileBuilder;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ControllerConfiguration
@Slf4j
@RequiredArgsConstructor
public class ElasticJobReconciler implements EventSourceInitializer<ElasticJob>, Reconciler<ElasticJob>, ErrorStatusHandler<ElasticJob> {
    
    private static final String ELASTICJOB_PREFIX = "elasticjob-";
    
    private static final String ELASTICJOB_ANNOTATION_PREFIX = "icu.wwj.elasticjob/";
    
    private static final String ELASTICJOB_ANNOTATION_CONFIG = ELASTICJOB_ANNOTATION_PREFIX + "config";
    
    private static final String ELASTICJOB_SHARDING_CONTEXT_PREFIX = "sharding-context-";
    
    private final KubernetesClient kubernetesClient;
    
    @Override
    public UpdateControl<ElasticJob> reconcile(final ElasticJob elasticJob, final Context<ElasticJob> context) {
        log.debug("Reconciling {} {}", elasticJob, context);
        if (JobExecutionType.TRANSIENT == elasticJob.getSpec().getJobExecutionType()) {
            if (null != elasticJob.getSpec().getCron() && !elasticJob.getSpec().getCron().isEmpty()) {
                Optional<CronJob> cronJob = context.getSecondaryResource(CronJob.class);
                if (cronJob.isPresent()) {
                    // TODO Check for update
                    return UpdateControl.noUpdate();
                }
                return createCronJob(elasticJob);
            }
        }
        throw new UnsupportedOperationException("Unsupported for now");
    }
    
    private UpdateControl<ElasticJob> createCronJob(final ElasticJob elasticJob) {
        kubernetesClient.batch().v1().cronjobs().resource(toCronJob(elasticJob)).createOrReplace();
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("Reconciled");
        elasticJob.setStatus(status);
        return UpdateControl.updateStatus(elasticJob);
    }
    
    private CronJob toCronJob(final ElasticJob elasticJob) {
        CronJob result = new CronJobBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(ELASTICJOB_PREFIX + elasticJob.getMetadata().getName())
                        .withNamespace(elasticJob.getMetadata().getNamespace())
                        .withLabels(elasticJob.getMetadata().getLabels())
                        .build())
                .withSpec(new CronJobSpecBuilder()
                        .withSchedule(elasticJob.getSpec().getCron())
                        .withSuspend(elasticJob.getSpec().isDisabled())
                        .withJobTemplate(new JobTemplateSpecBuilder()
                                .withSpec(new JobSpecBuilder()
                                        .withTemplate(getDecoratedPodTemplate(elasticJob))
                                        .withParallelism(elasticJob.getSpec().getShardingTotalCount())
                                        .withCompletions(elasticJob.getSpec().getShardingTotalCount())
                                        .withCompletionMode("Indexed")
                                        .build())
                                .build())
                        .build())
                .build();
        result.addOwnerReference(elasticJob);
        return result;
    }
    
    @SneakyThrows(JsonProcessingException.class)
    private PodTemplateSpec getDecoratedPodTemplate(ElasticJob elasticJob) {
        ObjectMapper objectMapper = new ObjectMapper();
        PodTemplateSpec copiedTemplate = objectMapper.readValue(objectMapper.writeValueAsString(elasticJob.getSpec().getTemplate()), PodTemplateSpec.class);
        copiedTemplate.getMetadata().getAnnotations().put(ELASTICJOB_ANNOTATION_PREFIX + "config", objectMapper.writeValueAsString(toCloudJobConfigurationPOJO(elasticJob)));
        for (int shardingItem = 0; shardingItem < elasticJob.getSpec().getShardingTotalCount(); shardingItem++) {
            ShardingContext shardingContext = new ShardingContext(elasticJob.getMetadata().getName(), "", elasticJob.getSpec().getShardingTotalCount(), elasticJob.getSpec().getJobParameter(), shardingItem, elasticJob.getSpec().getShardingItemParameters().getOrDefault("" + shardingItem, ""));
            copiedTemplate.getMetadata().getAnnotations().put(ELASTICJOB_ANNOTATION_PREFIX + ELASTICJOB_SHARDING_CONTEXT_PREFIX + shardingItem, objectMapper.writeValueAsString(shardingContext));
        }
        copiedTemplate.getSpec().getVolumes().add(new VolumeBuilder().withName("elasticjob")
                .withDownwardAPI(new DownwardAPIVolumeSourceBuilder()
                        .withItems(
                                new DownwardAPIVolumeFileBuilder().withPath("config").withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.annotations['" + ELASTICJOB_ANNOTATION_CONFIG + "']").build()).build(),
                                new DownwardAPIVolumeFileBuilder().withPath("sharding-item").withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.annotations['batch.kubernetes.io/job-completion-index']").build()).build()
                        )
                        .build())
                .build());
        copiedTemplate.getSpec().getVolumes().add(new VolumeBuilder().withName("elasticjob-sharding-context")
                .withDownwardAPI(new DownwardAPIVolumeSourceBuilder()
                        .withItems(IntStream.range(0, elasticJob.getSpec().getShardingTotalCount()).mapToObj(this::mountShardingContext).collect(Collectors.toList())).build())
                .build());
        for (Container each : copiedTemplate.getSpec().getContainers()) {
            each.getVolumeMounts().add(new VolumeMountBuilder().withName("elasticjob").withMountPath("/etc/elasticjob").build());
            each.getVolumeMounts().add(new VolumeMountBuilder().withName("elasticjob-sharding-context").withMountPath("/etc/elasticjob/sharding-context").build());
        }
        return copiedTemplate;
    }
    
    private CloudJobConfigurationPOJO toCloudJobConfigurationPOJO(final ElasticJob elasticJob) {
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
    
    private DownwardAPIVolumeFile mountShardingContext(int shardingItem) {
        return new DownwardAPIVolumeFileBuilder().withPath(shardingItem + "").withFieldRef(new ObjectFieldSelectorBuilder()
                .withFieldPath("metadata.annotations['" + ELASTICJOB_ANNOTATION_PREFIX + ELASTICJOB_SHARDING_CONTEXT_PREFIX + shardingItem + "']").build()).build();
    }
    
    @Override
    public Map<String, EventSource> prepareEventSources(final EventSourceContext<ElasticJob> context) {
        return EventSourceInitializer.nameEventSources(new InformerEventSource<>(InformerConfiguration.from(CronJob.class, context).build(), context));
    }
    
    @Override
    public ErrorStatusUpdateControl<ElasticJob> updateErrorStatus(final ElasticJob resource, final Context<ElasticJob> context, final Exception e) {
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("ERROR: " + e);
        resource.setStatus(status);
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
