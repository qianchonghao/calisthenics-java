package com.theladders.avital.cc.service;

import com.theladders.avital.cc.InvalidResumeException;
import com.theladders.avital.cc.NotSupportedJobTypeException;
import com.theladders.avital.cc.RequiresResumeForJReqJobException;

import java.time.LocalDate;
import java.util.List;

import static com.theladders.avital.cc.constants.CommandType.*;
import static com.theladders.avital.cc.constants.JobStatus.APPLIED;
/**
 * Application的职责： 做一些聚合 ApplicationManager 和 JobManager 二者能力的工作， 例如 execute方法
 * 但是感觉没有必要存在Application，可以从调用方直接分解调用 ApplicationManager 和 JobManager
 */

public class Application {
    private ApplicationManager applicationManager;
    private JobManager jobManager;

    public Application() {
        this.applicationManager = new ApplicationManager();
        this.jobManager = new JobManager();
    }

    // write
    public void execute(String command, String employerName, String jobName, String jobType, String jobSeekerName, String resumeApplicantName, LocalDate applicationTime) throws NotSupportedJobTypeException, RequiresResumeForJReqJobException, InvalidResumeException {
        if (command == PUBLISH) {
            jobManager.publishJob(employerName, jobName, jobType);
            return;
        }

        if (command == SAVE) {
            jobManager.saveJob(employerName, jobName, jobType);
            return;
        }

        if (command == APPLY) {
            applicationManager.apply(employerName, jobName, jobType, jobSeekerName, resumeApplicantName, applicationTime);
        }
    }

    // 以下都是read
    // @Chamber todo: 方法可拆分，拆分成 getApplicationInfo & getJob
    public List<List<String>> getJobs(String key, String type) {
        if (type.equals(APPLIED)) {
            return applicationManager.getApplicationInfo(key);
        }

        return jobManager.getJob(key);
    }



    // 根据jobName查询
    public List<String> findApplicants(String jobName, String employerName) {
        return applicationManager.doFindApplications(jobName, null, null);
    }

    //  根据JobName和 from 联合查询
    public List<String> findApplicants(String jobName, String employerName, LocalDate from) {
        return applicationManager.doFindApplications(jobName, from, null);
    }

    // @Chamber todo: 去除employee参数
    // 根据 JobName, from, to三个参数联合查询
    public List<String> findApplicants(String jobName, String employerName, LocalDate from, LocalDate to) {
        return applicationManager.doFindApplications(jobName, from, to);
    }

    public String export(String type, LocalDate date) {
        return applicationManager.export(type,date);
    }

    public int getSuccessfulApplications(String employerName, String jobName) {
        return applicationManager.getSuccessfulApplications(employerName,jobName);
    }

    public int getUnsuccessfulApplications(String employerName, String jobName) {
        return applicationManager.getUnsuccessfulApplications(employerName,jobName);
    }
}
