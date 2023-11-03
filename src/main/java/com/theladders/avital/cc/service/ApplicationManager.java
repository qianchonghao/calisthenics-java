package com.theladders.avital.cc.service;

import com.theladders.avital.cc.InvalidResumeException;
import com.theladders.avital.cc.RequiresResumeForJReqJobException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理应聘记录
 */
import static com.theladders.avital.cc.constants.JobType.JREQ;
import static com.theladders.avital.cc.constants.ReportType.CSV;
import static com.theladders.avital.cc.constants.ReportType.HTML;

public class ApplicationManager {
    // jobSeeker -> <jobName,jobType,applicationTime,employerName>
    private final HashMap<String, List<List<String>>> applied = new HashMap<>();

    // <jobName,jobType,applicationTime,employerName>
    private final List<List<String>> failedApplications = new ArrayList<>();

    public List<List<String>> getApplicationInfo(String jobSeekerName){
        return applied.get(jobSeekerName);
    }
    public void apply(String employerName, String jobName, String jobType, String jobSeekerName, String resumeApplicantName, LocalDate applicationTime) throws RequiresResumeForJReqJobException, InvalidResumeException {
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

    public List<String> doFindApplications(String jobName, LocalDate from, LocalDate to) {
        List<String> result = new ArrayList<String>() {
        };
        Iterator<Map.Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<List<String>>> set = iterator.next();
            String applicant = set.getKey();
            List<List<String>> jobs = set.getValue();
            addApplicantIfMatch(jobName, from, to, jobs, result, applicant);
        }
        return result;
    }

    private static void addApplicantIfMatch(String jobName, LocalDate from, LocalDate to, List<List<String>> jobs, List<String> result, String applicant) {
        boolean isAppliedThisDate = jobs.stream().anyMatch(job ->
        {
            Boolean matchJobName = true;
            if (jobName != null) {
                matchJobName = job.get(0).equals(jobName);
            }

            Boolean matchFrom = true;
            if (from != null) {
                matchFrom = !from.isAfter(LocalDate.parse(job.get(2), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            Boolean matchTo = true;
            if (to != null) {
                matchTo = !to.isBefore(LocalDate.parse(job.get(2), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }


            return matchJobName && matchFrom && matchTo;
        });
        if (isAppliedThisDate) {
            result.add(applicant);
        }
    }

    public String export(String type, LocalDate date) {
        if (type.equals(CSV)) {
            return exportCsv(date);
        }
        return exportHtml(date);
    }

    private String exportCsv(LocalDate date) {
        String result = "Employer,Job,Job Type,Applicants,Date" + "\n";
        for (Map.Entry entry :applied.entrySet()) {
            result = exportIfOnDate(CSV,date, entry, result);
        }
        return result;
    }

    private String exportHtml(LocalDate date) {
        String content = "";
        for (Map.Entry entry : applied.entrySet()) {
            content = exportIfOnDate(HTML, date, entry, content);
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

    private static String exportIfOnDate(String reportType, LocalDate date, Map.Entry<String, List<List<String>>> jobSeekerInfo, String result) {

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

    public int getSuccessfulApplications(String employerName, String jobName) {
        int result = 0;
        Iterator<Map.Entry<String, List<List<String>>>> iterator = this.applied.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<List<String>>> set = iterator.next();
            List<List<String>> jobs = set.getValue();

            result += jobs.stream().anyMatch(job -> job.get(3).equals(employerName) && job.get(0).equals(jobName)) ? 1 : 0;
        }
        return result;
    }

    public int getUnsuccessfulApplications(String employerName, String jobName) {
        return (int) failedApplications.stream().filter(job -> job.get(0).equals(jobName) && job.get(3).equals(employerName)).count();
    }
}
