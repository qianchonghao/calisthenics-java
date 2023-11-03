package com.theladders.avital.cc.service;

import com.theladders.avital.cc.NotSupportedJobTypeException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.theladders.avital.cc.constants.JobType.ATS;
import static com.theladders.avital.cc.constants.JobType.JREQ;

/**
 * 管理工作岗位
 */
public class JobManager {
    // EmployeeName -> <jobName,jobType>
    // jobs的属性，不仅是 雇主使用，用户也会使用。用户使用时候，请求雇主服务提供该属性？
    private final HashMap<String, List<List<String>>> jobs = new HashMap<>();

    /**
     * 雇主行为
     */
    public void saveJob(String employerName, String jobName, String jobType) {
        List<List<String>> saved = jobs.getOrDefault(employerName, new ArrayList<>());

        saved.add(new ArrayList<>() {{
            add(jobName);
            add(jobType);
        }});
        jobs.put(employerName, saved);
    }

    public void publishJob(String employerName, String jobName, String jobType) throws NotSupportedJobTypeException {
        if (!jobType.equals(JREQ) && !jobType.equals(ATS)) {
            throw new NotSupportedJobTypeException();
        }

        saveJob(employerName, jobName, jobType);
    }

    public List<List<String>> getJob(String employeeName){
        return jobs.get(employeeName);
    }


}
