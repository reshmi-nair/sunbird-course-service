package org.sunbird.models.batch.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchUser implements Serializable {

    private static final long serialVersionUID = 1L;
    private String batchid;

    private String userid;

    private String username;

    private String nodalname;

    private String coursename;

    private int status;

    private Date enrollDate;

    private Map<String,String> comment;

    private String score;

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(Map<String, String> comment) {
        this.comment = comment;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNodalname() {
        return nodalname;
    }

    public void setNodalname(String nodalname) {
        this.nodalname = nodalname;
    }

    public String getCoursename() {
        return coursename;
    }

    public void setCoursename(String coursename) {
        this.coursename = coursename;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getEnrollDate() {
        return enrollDate;
    }

    public void setEnrollDate(Date enrollDate) {
        this.enrollDate = enrollDate;
    }

    public String getBatchId() {
        return batchid;
    }

    public void setBatchId(String batchid) {
        this.batchid = batchid;
    }

    public String getUserId() {
        return userid;
    }

    public void setUserId(String userId) {
        this.userid = userid;
    }
}
