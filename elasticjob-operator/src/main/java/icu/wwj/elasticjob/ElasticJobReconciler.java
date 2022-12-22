package icu.wwj.elasticjob;

import icu.wwj.elasticjob.api.ElasticJob;
import icu.wwj.elasticjob.api.ElasticJobStatus;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
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
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@ControllerConfiguration
@Slf4j
@RequiredArgsConstructor
public class ElasticJobReconciler implements EventSourceInitializer<ElasticJob>, Reconciler<ElasticJob>, ErrorStatusHandler<ElasticJob> {
    
    private final KubernetesClient kubernetesClient;
    
    @Override
    public UpdateControl<ElasticJob> reconcile(final ElasticJob elasticJob, final Context<ElasticJob> context) {
        Optional<CronJob> cronJob = context.getSecondaryResource(CronJob.class);
        if (cronJob.isPresent()) {
            return UpdateControl.noUpdate();
        }
        CronJob newCronJob = new CronJobBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(elasticJob.getMetadata().getName())
                        .withNamespace(elasticJob.getMetadata().getNamespace())
                        .withLabels(elasticJob.getMetadata().getLabels())
                        .build())
                .withSpec(new CronJobSpecBuilder()
                        .withSchedule(elasticJob.getSpec().getCron())
                        .withSuspend(elasticJob.getSpec().isDisabled())
                        .withJobTemplate(new JobTemplateSpecBuilder()
                                .withSpec(new JobSpecBuilder()
                                        .withTemplate(elasticJob.getSpec().getTemplate())
                                        .build())
                                .build())
                        .build())
                .build();
        newCronJob.addOwnerReference(elasticJob);
        kubernetesClient.batch().v1().cronjobs().resource(newCronJob).createOrReplace();
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("RECONCILED");
        elasticJob.setStatus(status);
        return UpdateControl.updateStatus(elasticJob);
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
