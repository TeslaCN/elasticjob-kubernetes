package icu.wwj.elasticjob.sdk.daemon;

import icu.wwj.elasticjob.sdk.executor.KubernetesCloudJobFacade;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor;
import org.apache.shardingsphere.elasticjob.executor.JobFacade;
import org.apache.shardingsphere.elasticjob.infra.exception.JobSystemException;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.plugins.management.ShutdownHookPlugin;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class DaemonJobScheduler {
    
    private static final String ELASTIC_JOB_DATA_MAP_KEY = "elasticJob";
    
    private static final String ELASTIC_JOB_TYPE_DATA_MAP_KEY = "elasticJobType";
    
    private static final String JOB_FACADE_DATA_MAP_KEY = "jobFacade";
    
    private final ElasticJob elasticJob;
    
    private final String elasticJobType;
    
    private final JobConfiguration jobConfig;
    
    private final JobFacade jobFacade;
    
    /**
     * Init the job.
     */
    public void init() {
        JobDetail jobDetail = JobBuilder.newJob(DaemonJob.class).withIdentity(jobConfig.getJobName()).build();
        jobDetail.getJobDataMap().put(ELASTIC_JOB_DATA_MAP_KEY, elasticJob);
        jobDetail.getJobDataMap().put(ELASTIC_JOB_TYPE_DATA_MAP_KEY, elasticJobType);
        jobDetail.getJobDataMap().put(JOB_FACADE_DATA_MAP_KEY, jobFacade);
        try {
            // TODO Fill triggerIdentity
            scheduleJob(initializeScheduler(), jobDetail, "TODO", jobConfig.getCron());
        } catch (final SchedulerException ex) {
            throw new JobSystemException(ex);
        }
    }
    
    private Scheduler initializeScheduler() throws SchedulerException {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(getBaseQuartzProperties());
        return factory.getScheduler();
    }
    
    private Properties getBaseQuartzProperties() {
        Properties result = new Properties();
        result.put("org.quartz.threadPool.class", org.quartz.simpl.SimpleThreadPool.class.getName());
        result.put("org.quartz.threadPool.threadCount", "1");
        // TODO Fill
        result.put("org.quartz.scheduler.instanceName", "TODO");
        if (!jobConfig.isMisfire()) {
            result.put("org.quartz.jobStore.misfireThreshold", "1");
        }
        result.put("org.quartz.plugin.shutdownhook.class", ShutdownHookPlugin.class.getName());
        result.put("org.quartz.plugin.shutdownhook.cleanShutdown", Boolean.TRUE.toString());
        return result;
    }
    
    private void scheduleJob(final Scheduler scheduler, final JobDetail jobDetail, final String triggerIdentity, final String cron) {
        try {
            if (!scheduler.checkExists(jobDetail.getKey())) {
                scheduler.scheduleJob(jobDetail, createTrigger(triggerIdentity, cron));
            }
            scheduler.start();
        } catch (final SchedulerException ex) {
            throw new JobSystemException(ex);
        }
    }
    
    private CronTrigger createTrigger(final String triggerIdentity, final String cron) {
        return TriggerBuilder.newTrigger().withIdentity(triggerIdentity).withSchedule(CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionDoNothing()).build();
    }
    
    /**
     * Daemon job.
     */
    public static final class DaemonJob implements Job {
        
        @Setter
        private ElasticJob elasticJob;
        
        @Setter
        private String elasticJobType;
        
        @Setter
        private JobFacade jobFacade;
        
        private volatile ElasticJobExecutor jobExecutor;
        
        @Override
        public void execute(final JobExecutionContext context) {
            ShardingContexts shardingContexts = jobFacade.getShardingContexts();
            int jobEventSamplingCount = shardingContexts.getJobEventSamplingCount();
            int currentJobEventSamplingCount = shardingContexts.getCurrentJobEventSamplingCount();
            if (jobEventSamplingCount > 0 && ++currentJobEventSamplingCount < jobEventSamplingCount) {
                shardingContexts.setCurrentJobEventSamplingCount(currentJobEventSamplingCount);
                jobFacade.getShardingContexts().setAllowSendJobEvent(false);
                getJobExecutor().execute();
            } else {
                jobFacade.getShardingContexts().setAllowSendJobEvent(true);
                getJobExecutor().execute();
                shardingContexts.setCurrentJobEventSamplingCount(0);
            }
        }
        
        private ElasticJobExecutor getJobExecutor() {
            if (null == jobExecutor) {
                createJobExecutor();
            }
            return jobExecutor;
        }
        
        private synchronized void createJobExecutor() {
            if (null != jobExecutor) {
                return;
            }
            jobExecutor = null == elasticJob
                    ? new ElasticJobExecutor(elasticJobType, jobFacade.loadJobConfiguration(true), jobFacade)
                    : new ElasticJobExecutor(elasticJob, jobFacade.loadJobConfiguration(true), jobFacade);
        }
    }
}
