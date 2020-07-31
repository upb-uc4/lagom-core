package de.upb.cs.uc4.chaincode;

import com.google.gson.*;
import de.upb.cs.uc4.chaincode.model.Course;
import de.upb.cs.uc4.chaincode.model.Error;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.util.ArrayList;

@Contract(
        name="UC4.course"
)
public class CourseChaincode implements ContractInterface {

    private static Log _logger = LogFactory.getLog(CourseChaincode.class);
    // setup gson (de-)serializer capable of (de-)serializing dates
    private static final GsonWrapper gson = new GsonWrapper();

    @Transaction()
    public void initLedger(final Context ctx) {
        
    }

    /**
     * Adds a course to the ledger.
     * @param ctx
     * @param jsonCourse json-representation of a course to be added
     * @return Empty string on success, serialized error on failure
     */
    @Transaction()
    public String addCourse (final Context ctx, final String jsonCourse) {
        _logger.info("addCourse");

        ChaincodeStub stub = ctx.getStub();

        Course course = gson.fromJson(jsonCourse, Course.class);

        if(!stub.getStringState(course.getCourseId()).equals(""))
            return gson.toJson(new Error()
                    .name("02")
                    .detail("ID already exists"));

        String error = getErrorForCourse(course);
        if(error != null)
            return error;

        stub.putStringState(course.getCourseId(),gson.toJson(course));
        return "";
    }

    @Transaction()
    public String getAllCourses (final Context ctx) {
        _logger.info("queryAllLectures");
        ChaincodeStub stub = ctx.getStub();

        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"courseId\":{\"$regex\":\".*\"}}}");

        ArrayList<Course> courses = new ArrayList<Course>();
        for (KeyValue result: results) {
            courses.add(gson.fromJson(result.getStringValue(), Course.class));
        }
        return gson.toJson(courses.toArray(new Course[courses.size()]));
    }

    @Transaction()
    public String getCourseById (final Context ctx, final String courseId) {
        return gson.toJson(getCourse(ctx, courseId));
    }

    /**
     * Removes the course with the given courseId from the ledger.
     * @param ctx
     * @param courseId
     */
    @Transaction()
    public String deleteCourseById (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();

        if(stub.getStringState(courseId) == null || stub.getStringState(courseId).equals(""))
            return gson.toJson(new Error()
                    .name("03")
                    .detail("Course not found"));

        stub.delState(courseId);
        return "";
    }

    @Transaction()
    public String updateCourseById (
            final Context ctx,
            final String courseId,
            final String jsonCourse) {

        ChaincodeStub stub = ctx.getStub();

        Course updatedCourse = gson.fromJson(jsonCourse, Course.class);

        if (!courseId.equals(updatedCourse.getCourseId()))
            return gson.toJson(new Error()
                    .name("00")
                    .detail("Course ID and ID in path do not match"));

        String error = getErrorForCourse(updatedCourse);
        if (error != null)
            return error;
        if(stub.getStringState(courseId) == null || stub.getStringState(courseId).equals(""))
            return gson.toJson(new Error()
                    .name("03")
                    .detail("Course not found"));

        stub.delState(courseId);
        stub.putStringState(updatedCourse.getCourseId(), gson.toJson(updatedCourse));
        return "";
    }

    private Course getCourse (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();
        return gson.fromJson(stub.getStringState(courseId), Course.class);
    }

    private String getErrorForCourse(Course course) {
        if (course.getCourseName().equals(""))
            return gson.toJson(new Error()
                    .name("10")
                    .detail("Course name must not be empty"));

        if (false)
            return gson.toJson(new Error()
                    .name("11")
                    .detail("Course name has invalid characters"));

        if (course.getCourseType() == null)
            return gson.toJson(new Error()
                    .name("20")
                    .detail("Course type must be one of [\"Lecture\", \"Seminar\", \"Project Group\"]"));

        if (course.getStartDate() == null)
            return gson.toJson(new Error()
                    .name("30")
                    .detail("startDate must be the following format \"yyyy-mm-dd\""));

        if (course.getEndDate() == null)
            return gson.toJson(new Error()
                    .name("40")
                    .detail("endDate must be the following format \"yyyy-mm-dd\""));

        if (course.getEcts() == null || course.getEcts() < 1)
            return gson.toJson(new Error()
                    .name("50")
                    .detail("ects must be a positive integer number"));

        if (course.getLecturerId().equals(""))
            return gson.toJson(new Error()
                    .name("60")
                    .detail("lecturerID unknown"));

        if (course.getMaxParticipants() == null || course.getMaxParticipants() < 0)
            return gson.toJson(new Error()
                    .name("70")
                    .detail("maxParticipants must be a positive integer number"));

        if (course.getCourseLanguage() == null)
            return gson.toJson(new Error()
                    .name("80")
                    .detail("language must be one of [\"German\", \"English\"]"));

        return null;
    }
}
