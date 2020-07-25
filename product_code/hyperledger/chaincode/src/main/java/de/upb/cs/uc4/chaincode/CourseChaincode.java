package de.upb.cs.uc4.chaincode;

import com.google.gson.*;
import de.upb.cs.uc4.chaincode.model.Course;
import de.upb.cs.uc4.chaincode.model.Error;
import de.upb.cs.uc4.chaincode.model.invalidParams;
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
        name="UC4"
)
@Default
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

        try:
            Course course = gson.fromJson(jsonCourse, Course.class);
        catch:
            return unprocessableEntityError("parser_error");

        List<String> invalidParams = getErrorForCourse(course);

        if(invalidParams != null){
            return gson.toJson(new Error(invalidParams)
            .type("invalidPamameter")
            .title("The following parameters are invalid."));
        }

        
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

    private ArrayList<String> getErrorForCourse(Course course) {
        List<String> list = new ArrayList<String>();
        

        if(course.getCourseId()).equals(""))
            list.add(gson.toJson(new invalidParams()
                    .name("CourseID")
                    .reason("ID is empty")));

        if (course.getCourseName().equals(""))
            list.add(gson.toJson(new invalidParams()
                    .name("CourseName")
                    .reason("Course name must not be empty")));

        if (course.getCourseType() == null)
            list.add(gson.toJson(new invalidParams()
                    .name("CourseType")
                    .reason("Course type must be one of [\"Lecture\", \"Seminar\", \"Project Group\"]")));

        if (course.getStartDate() == null)
            list.add(gson.toJson(new invalidParams()
                    .name("StartDate")
                    .reason("startDate must be the following format \"yyyy-mm-dd\"")));

        if (course.getEndDate() == null)
            list.add(gson.toJson(new invalidParams()
                    .name("EndDate")
                    .reason("endDate must be the following format \"yyyy-mm-dd\"")));

        if (course.getEcts() == null || course.getEcts() < 1)
            list.add(gson.toJson(new invalidParams()
                    .name("Ects")
                    .reason("ects must be a positive integer number")));

        if (course.getLecturerId().equals(""))
            list.add(gson.toJson(new invalidParams()
                    .name("LecturerID")
                    .reason("lecturerID unknown")));

        if (course.getMaxParticipants() == null || course.getMaxParticipants() < 0)
            list.add(gson.toJson(new invalidParams()
                    .name("MaxParticipants")
                    .detail("maxParticipants must be a positive integer number")));

        if (course.getCourseLanguage() == null)
            list.add(gson.toJson(new invalidParams()
                    .name("CourseLanguage")
                    .detail("language must be one of [\"German\", \"English\"]")));

        return list;
    }

    private String notfoundError(Course course){


    }

    private json unprocessableEntityError(String error){
        if(error.equals("parser_error")):
            return gson.toJson(new Error([])
                .type("unprocessableEntity")
                .title("The given string does not conform to the specified json format."));

    }

}
