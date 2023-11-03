package com.theladders.avital.cc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.theladders.avital.cc.constants.CommandType.*;
import static com.theladders.avital.cc.constants.Constants.*;
import static com.theladders.avital.cc.constants.JobStatus.*;
import static com.theladders.avital.cc.constants.JobType.*;
import static com.theladders.avital.cc.constants.ReportType.*;
import static java.util.Map.*;

public class Application {


    // EmployeeName -> <jobName,jobType>
    // jobs的属性，不仅是 雇主使用，用户也会使用。用户使用时候，请求雇主服务提供该属性？
    private final HashMap<String, List<List<String>>> jobs = new HashMap<>();

    // jobSeeker -> <jobName,jobType,applicationTime,employerName>
    private final HashMap<String, List<List<String>>> applied = new HashMap<>();

    // <jobName,jobType,applicationTime,employerName>
    private final List<List<String>> failedApplications = new ArrayList<>();

    public void execute(String command, String employerName, String jobName, String jobType, String jobSeekerName, String resumeApplicantName, LocalDate applicationTime) throws NotSupportedJobTypeException, RequiresResumeForJReqJobException, InvalidResumeException {
        if (command == PUBLISH) {
            publishJob(employerName, jobName, jobType);
            return;
        }

        if (command == SAVE) {
            saveJob(employerName, jobName, jobType);
            return;
        }

        if (command == APPLY) {
            apply(employerName, jobName, jobType, jobSeekerName, resumeApplicantName, applicationTime);
        }
    }

    /**
     * 雇主行为
     */
    private void saveJob(String employerName, String jobName, String jobType) {
        List<List<String>> saved = jobs.getOrDefault(employerName, new ArrayList<>());

        saved.add(new ArrayList<>() {{
            add(jobName);
            add(jobType);
        }});
        jobs.put(employerName, saved);
    }

    private void publishJob(String employerName, String jobName, String jobType) throws NotSupportedJobTypeException {
        if (!jobType.equals(JREQ) && !jobType.equals(ATS)) {
            throw new NotSupportedJobTypeException();
        }

        saveJob(employerName, jobName, jobType);
    }

    /**
     * 求职者行为
     */
    private void apply(String employerName, String jobName, String jobType, String jobSeekerName, String resumeApplicantName, LocalDate applicationTime) throws RequiresResumeForJReqJobException, InvalidResumeException {
        if (jobType.equals(JREQ) && resumeApplicantName == null) {
            List<String> failedApplication = new ArrayList<String>() {{
                add(jobName);
                add(jobType);
                add(applicationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                add(employerName);
            }};
            failedApplications.add(failedApplication);
            throw new RequiresResumeForJReqJobException();
        }

        if (jobType.equals(JREQ) && !resumeApplicantName.equals(jobSeekerName)) {
            throw new InvalidResumeException();
        }
        List<List<String>> saved = this.applied.getOrDefault(jobSeekerName, new ArrayList<>());

        saved.add(new ArrayList<String>() {{
            add(jobName);
            add(jobType);
            add(applicationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            add(employerName);
        }});
        applied.put(jobSeekerName, saved);
    }


    // @Chamber todo: 方法可拆分，归属到 求职者 or 雇主
    public List<List<String>> getJobs(String employerName, String type) {
        if (type.equals(APPLIED)) {
            return applied.get(employerName);
        }

        return jobs.get(employerName);
    }

    public List<String> findApplicants(String jobName, String employerName) {
        return findApplicants(jobName, employerName, null);
    }

    public List<String> findApplicants(String jobName, String employerName, LocalDate from) {
        return findApplicants(jobName, employerName, from, null);
    }

    // @Chamber todo: 查询应聘job的候选人
    // @Chamber todo: 去除employee参数
    public List<String> findApplicants(String jobName, String employerName, LocalDate from, LocalDate to) {
            return doFindApplications(jobName,from,to);
    }

    // @Chamber todo: 做一个兼容逻辑, <jobName,jobType,applicationTime,employerName>
    private static void addApplicantIfMatchV0(String jobName, LocalDate from, LocalDate to, List<List<String>> jobs, List<String> result, String applicant) {
        boolean isAppliedThisDate = jobs.stream().anyMatch(job ->
        {
            Boolean matchJobName = true;
            if (jobName != null) {
                matchJobName = job.get(0).equals(jobName);
            }

            Boolean matchFrom = true;
            if (from != null) {
                // @Chamber todo: 改成before
                matchFrom = !from.isAfter(LocalDate.parse(job.get(2), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            Boolean matchTo = true;
            if (to != null) {
                matchTo = !to.isBefore(LocalDate.parse(job.get(2), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }


            return  matchJobName && matchFrom && matchTo;
        });
        if (isAppliedThisDate) {
            result.add(applicant);
        }
    }

    private List<String> doFindApplications(String jobName, LocalDate from, LocalDate to) {
        List<String> result = new ArrayList<String>() {};
        Iterator<Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, List<List<String>>> set = iterator.next();
            String applicant = set.getKey();
            List<List<String>> jobs = set.getValue();
            addApplicantIfMatchV0(jobName, from, to, jobs, result, applicant);
        }
        return result;
    }

    public String export(String type, LocalDate date) {
        if (type.equals(CSV)) {
            return exportCsv(date);
        }
        return exportHtml(date);
    }

    private static String exportIfOnDate(String reportType, LocalDate date, Entry<String, List<List<String>>> jobSeekerInfo, String result) {

        String applicant = jobSeekerInfo.getKey();
        List<List<String>> jobs1 = jobSeekerInfo.getValue();
        List<List<String>> appliedOnDate = jobs1.stream().filter(job -> job.get(2).equals(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))).collect(Collectors.toList());

        for (List<String> job : appliedOnDate) {
            result = contactResultWithApplicant(reportType, result, job, applicant);

        }
        return result;
    }

    private static String contactResultWithApplicant(String reportType, String result, List<String> job, String applicant) {
        if (CSV.equals(reportType)) {
            return result.concat(job.get(3) + "," + job.get(0) + "," + job.get(1) + "," + applicant + "," + job.get(2) + "\n");
        }

        if (HTML.equals(reportType)) {
            return result.concat("<tr>" + "<td>" + job.get(3) + "</td>" + "<td>" + job.get(0) + "</td>" + "<td>" + job.get(1) + "</td>" + "<td>" + applicant + "</td>" + "<td>" + job.get(2) + "</td>" + "</tr>");
        }

        return result;
    }

    private String exportCsv(LocalDate date) {
        String result = "Employer,Job,Job Type,Applicants,Date" + "\n";
        // @Chamber todo: forEach语句简化
        Iterator<Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            result = exportIfOnDate(CSV,date, iterator.next(), result);
        }
        return result;
    }

    private String exportHtml(LocalDate date) {
        String content = "";
        Iterator<Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            content = exportIfOnDate(HTML, date, iterator.next(), content);
        }

        return buildHtmlContent(content);
    }

    private static String buildHtmlContent(String content) {
        return "<!DOCTYPE html>"
                + "<body>"
                + "<table>"
                + "<thead>"
                + "<tr>"
                + "<th>Employer</th>"
                + "<th>Job</th>"
                + "<th>Job Type</th>"
                + "<th>Applicants</th>"
                + "<th>Date</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>"
                + content
                + "</tbody>"
                + "</table>"
                + "</body>"
                + "</html>";
    }


    public int getSuccessfulApplications(String employerName, String jobName) {
        int result = 0;
        Iterator<Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, List<List<String>>> set = iterator.next();
            List<List<String>> jobs = set.getValue();

            result += jobs.stream().anyMatch(job -> job.get(3).equals(employerName) && job.get(0).equals(jobName)) ? 1 : 0;
        }
        return result;
    }

    public int getUnsuccessfulApplications(String employerName, String jobName) {
        return (int) failedApplications.stream().filter(job -> job.get(0).equals(jobName) && job.get(3).equals(employerName)).count();
    }
}
