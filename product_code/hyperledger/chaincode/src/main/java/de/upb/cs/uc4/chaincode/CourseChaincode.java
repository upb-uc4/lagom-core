package de.upb.cs.uc4.chaincode;

import com.google.gson.*;
import de.upb.cs.uc4.chaincode.model.Course;
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
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.lang.reflect.Type;
import java.util.ArrayList;

@Contract(
        name="UC4"
)
@Default
public class CourseChaincode implements ContractInterface {

    private static Log _logger = LogFactory.getLog(CourseChaincode.class);
    // setup gson (de-)serializer capable of (de-)serializing dates
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDate.class,
                    new JsonDeserializer<LocalDate>() {
                        @Override
                        public LocalDate deserialize(
                                JsonElement json,
                                Type type,
                                JsonDeserializationContext jsonDeserializationContext
                        ) throws JsonParseException {
                            return LocalDate.parse(json.getAsJsonPrimitive().getAsString());
                        }
                    })
            .registerTypeAdapter(
                    LocalDate.class,
                    new JsonSerializer<LocalDate>() {
                        @Override
                        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
                            return new JsonPrimitive("2020-06-26"); // "yyyy-mm-dd"
                        }
                    }
                    )
            .create();

    @Transaction()
    public void initLedger(final Context ctx) {
        
    }

    @Transaction()
    public void addCourse (final Context ctx, final String jsonCourse) {
        _logger.info("addCourse");

        ChaincodeStub stub = ctx.getStub();

        Course course = gson.fromJson(jsonCourse, Course.class);
        stub.putStringState(course.getCourseId(),gson.toJson(course));
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

    @Transaction()
    public void deleteCourseById (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();

        stub.delState(courseId);
    }

    @Transaction()
    public void updateCourseById (
            final Context ctx,
            final String courseId,
            final String jsonCourse) {

        ChaincodeStub stub = ctx.getStub();

        stub.delState(courseId);
        Course updatedCourse = gson.fromJson(jsonCourse, Course.class);
        stub.putStringState(updatedCourse.getCourseId(), gson.toJson(updatedCourse));
    }

    private Course getCourse (final Context ctx, final String courseId) {
        ChaincodeStub stub = ctx.getStub();
        return gson.fromJson(stub.getStringState(courseId), Course.class);
    }
}
