package de.upb.cs.uc4.chaincode;

import de.upb.cs.uc4.chaincode.model.*;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Contract(
        name="UC4"
)
@Default
public class StudentChaincode implements ContractInterface {

    private static Log _logger = LogFactory.getLog(StudentChaincode.class);
    // setup gson (de-)serializer capable of (de-)serializing dates
    private static final GsonWrapper gson = new GsonWrapper();

    @Transaction()
    public void initLedger(final Context ctx) {

    }

    /**
     * Adds a student to the ledger.
     * @param ctx
     * @param jsonStudent json-representation of a student to be added
     * @return Empty string on success, serialized error on failure
     */
    @Transaction()
    public String immatriculateStudent (final Context ctx, final String jsonStudent) {
        _logger.info("immatriculateStudent");

        ChaincodeStub stub = ctx.getStub();
        Student student = null;

        try
        {
            student = gson.fromJson(jsonStudent, Student.class);
        }
        catch(Exception e)
        {
            return gson.toJson(new DetailedError()
                    .type("Unprocessable Entity")
                    .title("The given string does not conform to the specified json format."));
        }

        String result = stub.getStringState(student.getMatriculationId());
        if (result != null && result != "") {
            return gson.toJson(new DetailedError()
                    .type("Conflict")
                    .title("There is already a student for the given matriculationId."));
        }

        ArrayList<InvalidParameter> invalidParams = getErrorForStudent(student);

        if(!invalidParams.isEmpty()){
            return gson.toJson(new DetailedError()
                    .type("Unprocessable Entity")
                    .title("The given string does not conform to the specified json format.")
                    .invalidParams(invalidParams));
        }

        stub.putStringState(student.getMatriculationId(),gson.toJson(student));
        return "";
    }

    @Transaction()
    public String getAllStudents (final Context ctx) {
        _logger.info("queryAllStudents");
        ChaincodeStub stub = ctx.getStub();

        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"matriculationId\":{\"$regex\":\".*\"}}}");

        ArrayList<Student> students = new ArrayList<>();
        for (KeyValue result: results) {
            students.add(gson.fromJson(result.getStringValue(), Student.class));
        }
        return gson.toJson(students.toArray(new Student[students.size()]));
    }

    /**
     * Removes the student with the given matriculationId from the ledger.
     * @param ctx
     * @param matriculationId
     */
    @Transaction()
    public String deleteStudent (final Context ctx, final String matriculationId) {
        ChaincodeStub stub = ctx.getStub();

        String studentOnLedger = stub.getStringState(matriculationId);

        if(studentOnLedger == null || studentOnLedger.equals(""))
            return gson.toJson(new DetailedError()
                    .type("Not found")
                    .title("There is no student for the given matriculationId."));

        stub.delState(matriculationId);
        return "";
    }

    @Transaction()
    public String updateStudent (final Context ctx, final String jsonStudent) {

        ChaincodeStub stub = ctx.getStub();

        Student updatedStudent = gson.fromJson(jsonStudent, Student.class);

        ArrayList<InvalidParameter> invalidParams = getErrorForStudent(updatedStudent);

        if (!invalidParams.isEmpty())
            return gson.toJson(new DetailedError()
                    .type("Unprocessable Entity")
                    .title("The given string does not conform to the specified json format.")
                    .invalidParams(invalidParams));

        String studentOnLedger = stub.getStringState(updatedStudent.getMatriculationId());

        if(studentOnLedger == null || studentOnLedger.equals(""))
            return gson.toJson(new DetailedError()
                    .type("Not Found")
                    .title("There is no student for the given matriculationId."));

        stub.delState(updatedStudent.getMatriculationId());
        stub.putStringState(updatedStudent.getMatriculationId(), gson.toJson(updatedStudent));
        return "";
    }

    @Transaction()
    public String getStudent (final Context ctx, final String matriculationId) {
        return getStudentByMatriculationId(ctx, matriculationId);
    }

    private String getStudentByMatriculationId (final Context ctx, final String matriculationId) {
        ChaincodeStub stub = ctx.getStub();
        Student student = gson.fromJson(stub.getStringState(matriculationId), Student.class);

        if(student == null || student.equals(""))
            return gson.toJson(new DetailedError()
                    .type("Not Found")
                    .title("There is no student for the given matriculationId."));
        return gson.toJson(student);
    }

    private ArrayList<InvalidParameter> getErrorForStudent(Student student) {

        ArrayList<InvalidParameter> list = new ArrayList<>();

        if(student.getMatriculationId() == null || student.getMatriculationId().equals(""))
            list.add(new InvalidParameter()
                    .name("matriculationID")
                    .reason("ID is empty"));

        if (student.getFirstName() == null || student.getFirstName().equals(""))
            list.add(new InvalidParameter()
                    .name("firstName")
                    .reason("First name must not be empty"));

        if (student.getLastName() == null || student.getLastName().equals(""))
            list.add(new InvalidParameter()
                    .name("lastName")
                    .reason("Last name must not be empty"));

        if (student.getBirthDate() == null)
            list.add(new InvalidParameter()
                    .name("birthDate")
                    .reason("Birth date must be the following format \"yyyy-mm-dd\""));

        List<SubjectMatriculationInterval> immatriculationStatus = student.getMatriculationStatus();

        if (immatriculationStatus == null || immatriculationStatus.size() == 0)
            list.add(new InvalidParameter()
                    .name("matriculationStatus")
                    .reason("Matriculation status must not be empty"));
        else {

            ArrayList<SubjectMatriculationInterval.FieldOfStudyEnum> existingFields = new ArrayList<SubjectMatriculationInterval.FieldOfStudyEnum>();

            for (SubjectMatriculationInterval subInterval: immatriculationStatus) {

                if (subInterval.getFieldOfStudy() == null || subInterval.getFieldOfStudy().equals(""))
                    list.add(new InvalidParameter()
                            .name("SubjectMatriculationInterval.fieldOfStudy")
                            .reason("Field of study must not be empty"));
                else
                    if (existingFields.contains(subInterval.getFieldOfStudy()))
                        list.add(new InvalidParameter()
                                .name("SubjectMatriculationInterval.fieldOfStudy")
                                .reason("Each field of study should only appear in one SubjectMatriculationInterval."));
                    else
                        existingFields.add(subInterval.getFieldOfStudy());

                if (subInterval.getIntervals() == null || subInterval.getIntervals().size() == 0)
                    list.add(new InvalidParameter()
                            .name("SubjectMatriculationInterval.intervals")
                            .reason("Intervals must not be empty"));

                for (MatriculationInterval interval: subInterval.getIntervals()) {


                    if (interval.getFirstSemester() == null || interval.getFirstSemester().equals(""))
                        list.add(new InvalidParameter()
                                .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                .reason("First semester must not be empty"));

                    if (interval.getLastSemester() == null || interval.getLastSemester().equals(""))
                        list.add(new InvalidParameter()
                                .name("SubjectMatriculationInterval.MatriculationInterval.lastSemester")
                                .reason("Last semester must not be empty"));

                    if (semesterFormatValid(interval.getFirstSemester()) && semesterFormatValid(interval.getFirstSemester())) {

                        int firstSemesterYear = Integer.parseInt(interval.getFirstSemester().substring(2));
                        int lastSemesterYear = Integer.parseInt(interval.getLastSemester().substring(2));

                        if (firstSemesterYear > lastSemesterYear){
                            list.add(new InvalidParameter()
                                    .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                    .reason("First and last semester must be in chronological order. " +
                                            "Last semester lays chronologically before first semester."));
                        }

                        if (firstSemesterYear == lastSemesterYear){

                            if(interval.getFirstSemester().startsWith("WS") && interval.getLastSemester().startsWith("SS") )
                                list.add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First and last semester must be in chronological order. " +
                                                "Last semester lays chronologically before first semester."));
                        }
                    }

                    if (semesterFormatValid(interval.getFirstSemester()) && student.getBirthDate() != null) {

                        int firstSemesterYear = Integer.parseInt(interval.getFirstSemester().substring(2));
                        if (firstSemesterYear < student.getBirthDate().getYear()) {
                            list.add(new InvalidParameter()
                                    .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                    .reason("First semester must not be earlier than birth date."));
                        }
                    }

                    if (!semesterFormatValid(interval.getFirstSemester()))
                            list.add(new InvalidParameter()
                                .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                .reason("First semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));

                    if (!semesterFormatValid(interval.getLastSemester()))
                        list.add(new InvalidParameter()
                                .name("SubjectMatriculationInterval.MatriculationInterval.lastSemester")
                                .reason("Last semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));
                }
            }
        }

        return list;
    }

    public boolean semesterFormatValid(String semester) {
        Pattern pattern = Pattern.compile("^(WS|SS)\\d{4}");
        Matcher matcher = pattern.matcher(semester);
        return matcher.matches();
    }
}
