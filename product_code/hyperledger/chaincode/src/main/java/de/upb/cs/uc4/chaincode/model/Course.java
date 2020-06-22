package de.upb.cs.uc4.chaincode.model;


import com.google.gson.Gson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;



@DataType
public class Course {


    @Property()
    private String courseId;

    @Property()
    private String courseName;

    @Property()
    private CourseType courseType;

    @Property()
    private String startDate;

    @Property()
    private String endDate;

    @Property()
    private int ects;

    @Property()
    private String lecturerId;

    @Property()
    private int maxParticipants;

    @Property()
    private int currentParticipants;

    @Property()
    private CourseLanguage courseLanguage;

    @Property()
    private String courseDescription;

    public Course(
           String courseId,
           String courseName,
           CourseType courseType,
           String startDate,
           String endDate,
           int ects,
           String lecturerId,
           int maxParticipants,
           int currentParticipants,
           CourseLanguage courseLanguage,
           String courseDescription
    ) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseType = courseType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ects = ects;
        this.lecturerId = lecturerId;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = currentParticipants;
        this.courseLanguage = courseLanguage;
        this.courseDescription = courseDescription;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getEcts() {
        return ects;
    }

    public void setEcts(int ects) {
        this.ects = ects;
    }

    public String getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public CourseLanguage getCourseLanguage() {
        return courseLanguage;
    }

    public void setCourseLanguage(CourseLanguage courseLanguage) {
        this.courseLanguage = courseLanguage;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }
}
