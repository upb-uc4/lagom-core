package de.upb.cs.uc4.chaincode;

import com.google.gson.Gson;
import de.upb.cs.uc4.chaincode.model.Course;
import de.upb.cs.uc4.chaincode.model.CourseLanguage;
import de.upb.cs.uc4.chaincode.model.CourseType;
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

    @Transaction()
    public void initLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        Course course = new Course(
                "courseId",
                "courseName",
                CourseType.Lecture,
                "startDate",
                "endDate",
                0,
                "lecturerId",
                0,
                0,
                CourseLanguage.English,
                "courseDescription"
        );
        stub.putStringState(course.getCourseId(),course.toJson());
    }

    @Transaction()
    public void addCourse (final Context ctx, final String jsonCourse) {
        _logger.info("addCourse");

        ChaincodeStub stub = ctx.getStub();

        Gson gson = new Gson();
        Course course = gson.fromJson(jsonCourse, Course.class);
        stub.putStringState(course.getCourseId(),course.toJson());
    }

    @Transaction()
    public Course[] getAllCourses (final Context ctx) {
        _logger.info("queryAllLectures");
        ChaincodeStub stub = ctx.getStub();

        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"courseId\":{\"$regex\":\".*\"}}}");

        ArrayList<Course> courses = new ArrayList<Course>();

        Gson gson = new Gson();
        for (KeyValue result: results) {
            courses.add(gson.fromJson(result.getStringValue(), Course.class));
        }
        return courses.toArray(new Course[courses.size()]);
    }

    @Transaction()
    public Course getCourseById (final Context ctx, final String courseId) {
        return getCourse(ctx, courseId);
    }

    @Transaction()
    public void deleteCourseById (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();

        stub.delState(courseId);
    }

    @Transaction()
    public void updateCourseById (
            final Context ctx,
            final String courseId,
            final Course updatedCourse) {

        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(updatedCourse.getCourseId(), updatedCourse.toJson());
    }

    private Course getCourse (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();
        Gson gson = new Gson();
        return gson.fromJson(stub.getStringState(courseId), Course.class);
    }
}
