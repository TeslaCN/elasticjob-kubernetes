package icu.wwj.elasticjob.reconciler;

import icu.wwj.elasticjob.api.ElasticJob;
import icu.wwj.elasticjob.api.trigger.ElasticJobTrigger;
import icu.wwj.elasticjob.api.trigger.ElasticJobTriggerStatus;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
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
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerConfiguration
@Slf4j
@RequiredArgsConstructor
public class ElasticJobTriggerReconciler implements EventSourceInitializer<ElasticJobTrigger>, Reconciler<ElasticJobTrigger>, ErrorStatusHandler<ElasticJobTrigger> {
    
    private final KubernetesClient kubernetesClient;
    
    @Override
    public Map<String, EventSource> prepareEventSources(final EventSourceContext<ElasticJobTrigger> context) {
        SecondaryToPrimaryMapper<ElasticJob> triggerMatchingElasticJobName = (ElasticJob elasticJob) -> context.getPrimaryCache()
                .list(trigger -> trigger.getMetadata().getName().equals(elasticJob.getMetadata().getName()))
                .map(ResourceID::fromResource).collect(Collectors.toSet());
        InformerConfiguration<ElasticJob> configuration = InformerConfiguration.from(ElasticJob.class, context)
                .withSecondaryToPrimaryMapper(triggerMatchingElasticJobName).build();
        InformerConfiguration<Job> jobConfiguration = InformerConfiguration.from(Job.class, context)
                .withSecondaryToPrimaryMapper(job -> context.getPrimaryCache().list(job::hasOwnerReferenceFor).map(ResourceID::fromResource).collect(Collectors.toSet())).build();
        return EventSourceInitializer.nameEventSources(new InformerEventSource<>(configuration, context), new InformerEventSource<>(jobConfiguration, context));
    }
    
    @Override
    public UpdateControl<ElasticJobTrigger> reconcile(final ElasticJobTrigger resource, final Context<ElasticJobTrigger> context) throws Exception {
        ElasticJob elasticJob = context.getSecondaryResource(ElasticJob.class)
                .orElseThrow(() -> new IllegalStateException("There is no ElasticJob [" + resource.getMetadata().getName() + "]"));
        Optional<Job> job = context.getSecondaryResource(Job.class);
        if (job.isEmpty()) {
            return createJob(resource, elasticJob);
        }
        // TODO Check job and trigger update
        return UpdateControl.noUpdate();
    }
    
    private UpdateControl<ElasticJobTrigger> createJob(final ElasticJobTrigger trigger, final ElasticJob elasticJob) {
        Job job = new JobBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(trigger.getMetadata().getName())
                        .withNamespace(trigger.getMetadata().getNamespace())
                        .withLabels(trigger.getMetadata().getLabels())
                        .build())
                .withSpec(new JobSpecBuilder()
                        .withTemplate(ElasticJobDecorator.getDecoratedPodTemplate(elasticJob))
                        .withParallelism(elasticJob.getSpec().getShardingTotalCount())
                        .withCompletions(elasticJob.getSpec().getShardingTotalCount())
                        .withCompletionMode("Indexed")
                        .build()
                ).build();
        job.addOwnerReference(trigger);
        kubernetesClient.batch().v1().jobs().resource(job).createOrReplace();
        ElasticJobTriggerStatus status = new ElasticJobTriggerStatus();
        trigger.setStatus(status);
        return UpdateControl.patchStatus(trigger);
    }
    
    @Override
    public ErrorStatusUpdateControl<ElasticJobTrigger> updateErrorStatus(final ElasticJobTrigger resource, final Context<ElasticJobTrigger> context, final Exception e) {
        ElasticJobTriggerStatus status = new ElasticJobTriggerStatus();
        status.setStatus(e.getMessage());
        resource.setStatus(status);
        return ErrorStatusUpdateControl.patchStatus(resource);
    }
}
