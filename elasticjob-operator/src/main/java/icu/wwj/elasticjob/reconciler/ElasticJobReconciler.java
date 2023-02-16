package icu.wwj.elasticjob.reconciler;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import icu.wwj.elasticjob.api.ElasticJob;
import icu.wwj.elasticjob.api.ElasticJobStatus;
import icu.wwj.elasticjob.api.JobExecutionType;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@ControllerConfiguration
@Slf4j
@RequiredArgsConstructor
public class ElasticJobReconciler implements EventSourceInitializer<ElasticJob>, Reconciler<ElasticJob>, ErrorStatusHandler<ElasticJob> {
    
    private final KubernetesClient kubernetesClient;
    
    @Override
    public UpdateControl<ElasticJob> reconcile(final ElasticJob elasticJob, final Context<ElasticJob> context) {
        log.debug("Reconciling {} {}", elasticJob, context);
        // TODO Check job execution type changes
        if (JobExecutionType.DAEMON == elasticJob.getSpec().getJobExecutionType()) {
            return createStatefulSet(elasticJob);
        }
        if (null != elasticJob.getSpec().getCron() && !elasticJob.getSpec().getCron().isEmpty()) {
            Optional<CronJob> cronJob = context.getSecondaryResource(CronJob.class);
            // TODO Check cron changes
            return cronJob.map(job -> update(elasticJob, job)).orElseGet(() -> createCronJob(elasticJob));
        }
        return prepareTransientJob(elasticJob);
    }
    
    private UpdateControl<ElasticJob> update(final ElasticJob elasticJob, final CronJob cronJob) {
        kubernetesClient.batch().v1().cronjobs().resource(toCronJob(elasticJob)).createOrReplace();
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus(cronJob.getStatus().getActive().isEmpty() ? "Staging" : "Running");
        status.setLastScheduleTime(cronJob.getStatus().getLastScheduleTime());
        status.setShardingTotalCount(elasticJob.getSpec().getShardingTotalCount());
        elasticJob.setStatus(status);
        return UpdateControl.patchStatus(elasticJob);
    }
    
    private UpdateControl<ElasticJob> createCronJob(final ElasticJob elasticJob) {
        kubernetesClient.batch().v1().cronjobs().resource(toCronJob(elasticJob)).createOrReplace();
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("Reconciled");
        status.setShardingTotalCount(elasticJob.getSpec().getShardingTotalCount());
        elasticJob.setStatus(status);
        return UpdateControl.patchStatus(elasticJob);
    }
    
    private CronJob toCronJob(final ElasticJob elasticJob) {
        CronJob result = new CronJobBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(Constants.ELASTICJOB_PREFIX + elasticJob.getMetadata().getName())
                        .withNamespace(elasticJob.getMetadata().getNamespace())
                        .withLabels(elasticJob.getMetadata().getLabels())
                        .build())
                .withSpec(new CronJobSpecBuilder()
                        .withSchedule(convertToUnixCron(elasticJob.getSpec().getCron()))
                        .withSuspend(elasticJob.getSpec().isDisabled())
                        .withJobTemplate(new JobTemplateSpecBuilder()
                                .withSpec(new JobSpecBuilder()
                                        .withTemplate(ElasticJobDecorator.getDecoratedPodTemplate(elasticJob))
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
    
    private String convertToUnixCron(String cron) {
        CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        return CronMapper.fromQuartzToUnix().map(cronParser.parse(cron)).asString();
    }
    
    private UpdateControl<ElasticJob> createStatefulSet(ElasticJob elasticJob) {
        StatefulSet statefulSet = toStatefulSet(elasticJob);
        kubernetesClient.apps().statefulSets().resource(statefulSet).createOrReplace();
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("Reconciled");
        status.setShardingTotalCount(elasticJob.getSpec().getShardingTotalCount());
        elasticJob.setStatus(status);
        return UpdateControl.updateStatus(elasticJob);
    }
    
    private StatefulSet toStatefulSet(ElasticJob elasticJob) {
        StatefulSet result = new StatefulSetBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(Constants.ELASTICJOB_PREFIX + elasticJob.getMetadata().getName())
                        .withNamespace(elasticJob.getMetadata().getNamespace())
                        .withLabels(elasticJob.getMetadata().getLabels())
                        .build())
                .withSpec(new StatefulSetSpecBuilder()
                        .withSelector(new LabelSelectorBuilder().withMatchLabels(
                                Collections.singletonMap(Constants.ELASTICJOB_LABEL_APP, elasticJob.getMetadata().getName())).build())
                        .withTemplate(ElasticJobDecorator.getDecoratedPodTemplate(elasticJob))
                        .withReplicas(elasticJob.getSpec().getShardingTotalCount())
                        .build())
                .build();
        result.addOwnerReference(elasticJob);
        return result;
    }
    
    private UpdateControl<ElasticJob> prepareTransientJob(final ElasticJob elasticJob) {
        if (null != elasticJob.getStatus()) {
            return UpdateControl.noUpdate();
        }
        ElasticJobStatus status = new ElasticJobStatus();
        status.setStatus("Waiting for trigger");
        status.setShardingTotalCount(elasticJob.getSpec().getShardingTotalCount());
        elasticJob.setStatus(status);
        return UpdateControl.patchStatus(elasticJob);
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
