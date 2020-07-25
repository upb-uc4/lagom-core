package de.upb.cs.uc4.chaincode;

import de.upb.cs.uc4.chaincode.model.Course;
import de.upb.cs.uc4.chaincode.model.Error;
import de.upb.cs.uc4.chaincode.model.InvalidParameter;
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
        Course course = null;

        try
        {
            course = gson.fromJson(jsonCourse, Course.class);
        }
        catch(Exception e)
        {
            return gson.toJson(new Error()
                    .type("Unprocessable Entity")
                    .title("The given string does not conform to the specified json format."));
        }

        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"courseId\":{\"$regex\":\".*\"}}}");


        ArrayList<InvalidParameter> invalidParams = getErrorForCourse(course);

        if(!invalidParams.isEmpty()){
            return gson.toJson(new Error(invalidParams)
            .type("Unprocessable Entity")
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

        ArrayList<Course> courses = new ArrayList<>();
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
                    .type("Not found")
                    .title("The given ID does not fit any existing course."));

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
                    .type("Not Found")
                    .title("Course ID and ID in path do not match"));

        ArrayList<InvalidParameter> invalidParams = getErrorForCourse(updatedCourse);

        if (!invalidParams.isEmpty())
            return gson.toJson(new Error()
                    .type("Unprocessable Entity")
                    .title("The given string does not conform to the specified json format."));

        if(stub.getStringState(courseId) == null || stub.getStringState(courseId).equals(""))
            return gson.toJson(new Error()
                    .type("Not Found")
                    .title("Course not found"));

        stub.delState(courseId);
        stub.putStringState(updatedCourse.getCourseId(), gson.toJson(updatedCourse));
        return "";
    }

    private Course getCourse (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();
        return gson.fromJson(stub.getStringState(courseId), Course.class);
    }

    private ArrayList<InvalidParameter> getErrorForCourse(Course course) {

        ArrayList<InvalidParameter> list = new ArrayList<>();

        if(course.getCourseId().equals(""))
            list.add(new InvalidParameter()
                    .name("CourseID")
                    .reason("ID is empty"));

        if (course.getCourseName().equals(""))
            list.add(new InvalidParameter()
                    .name("CourseName")
                    .reason("Course name must not be empty"));

        if (course.getCourseType() == null)
            list.add(new InvalidParameter()
                    .name("CourseType")
                    .reason("Course type must be one of [\"Lecture\", \"Seminar\", \"Project Group\"]"));

        if (course.getStartDate() == null)
            list.add(new InvalidParameter()
                    .name("StartDate")
                    .reason("startDate must be the following format \"yyyy-mm-dd\""));

        if (course.getEndDate() == null)
            list.add(new InvalidParameter()
                    .name("EndDate")
                    .reason("endDate must be the following format \"yyyy-mm-dd\""));

        if (course.getEcts() == null || course.getEcts() < 1)
            list.add(new InvalidParameter()
                    .name("Ects")
                    .reason("ects must be a positive integer number"));

        if (course.getLecturerId().equals(""))
            list.add(new InvalidParameter()
                    .name("LecturerID")
                    .reason("lecturerID unknown"));

        if (course.getMaxParticipants() == null || course.getMaxParticipants() < 0)
            list.add(new InvalidParameter()
                    .name("MaxParticipants")
                    .reason("maxParticipants must be a positive integer number"));

        if (course.getCourseLanguage() == null)
            list.add(new InvalidParameter()
                    .name("CourseLanguage")
                    .reason("language must be one of [\"German\", \"English\"]"));

        return list;
    }
}
