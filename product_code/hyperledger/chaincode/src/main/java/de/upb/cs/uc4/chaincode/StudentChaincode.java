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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Contract(
        name="UC4.student"
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
    public String addMatriculationData(final Context ctx, final String jsonStudent) {
        _logger.info("immatriculateStudent");

        ChaincodeStub stub = ctx.getStub();
        Student student = null;

        try
        {
            student = gson.fromJson(jsonStudent, Student.class);
        }
        catch(Exception e)
        {
            return gson.toJson(new GenericError()
                    .type("hl: unprocessable entity")
                    .title("The given string does not conform to the specified format."));
        }

        String result = stub.getStringState(student.getMatriculationId());
        if (result != null && result != "") {
            return gson.toJson(new GenericError()
                    .type("hl: conflict")
                    .title("There is already a student for the given matriculationId."));
        }

        ArrayList<InvalidParameter> invalidParams = getErrorForStudent(student);

        if(!invalidParams.isEmpty()){
            return gson.toJson(new DetailedError()
                    .type("hl: unprocessable entity")
                    .title("The given string does not conform to the specified format.")
                    .invalidParams(invalidParams));
        }

        stub.putStringState(student.getMatriculationId(),gson.toJson(student));
        return "";
    }

    @Transaction()
    public String updateMatriculationData(final Context ctx, final String jsonStudent) {

        ChaincodeStub stub = ctx.getStub();

        Student updatedStudent = gson.fromJson(jsonStudent, Student.class);

        ArrayList<InvalidParameter> invalidParams = getErrorForStudent(updatedStudent);

        if (!invalidParams.isEmpty())
            return gson.toJson(new DetailedError()
                    .type("hl: unprocessable entity")
                    .title("The given string does not conform to the specified format.")
                    .invalidParams(invalidParams));

        String studentOnLedger = stub.getStringState(updatedStudent.getMatriculationId());

        if(studentOnLedger == null || studentOnLedger.equals(""))
            return gson.toJson(new GenericError()
                    .type("hl: not found")
                    .title("There is no student for the given matriculationId."));

        stub.delState(updatedStudent.getMatriculationId());
        stub.putStringState(updatedStudent.getMatriculationId(), gson.toJson(updatedStudent));
        return "";
    }

    @Transaction()
    public String getMatriculationData(final Context ctx, final String matriculationId) {

        ChaincodeStub stub = ctx.getStub();
        Student student = gson.fromJson(stub.getStringState(matriculationId), Student.class);

        if(student == null || student.equals(""))
            return gson.toJson(new DetailedError()
                    .type("hl: not found")
                    .title("There is no student for the given matriculationId."));
        return gson.toJson(student);
    }

    @Transaction()
    public String addEntryToMatriculationData (
            final Context ctx,
            final String matriculationId,
            final String fieldOfStudy,
            final String semester) {

        ArrayList<InvalidParameter> invalidParams = new ArrayList<InvalidParameter>();
        SubjectMatriculation.FieldOfStudyEnum fieldOfStudyValue = SubjectMatriculation.FieldOfStudyEnum.fromValue(fieldOfStudy);

        if (fieldOfStudyValue == null)
            invalidParams.add(new InvalidParameter()
                    .name("fieldOfStudy")
                    .reason("The given value is not accepted."));

        if (!semesterFormatValid(semester))
            invalidParams.add(new InvalidParameter()
                    .name("semester")
                    .reason("First semester must be the following format \"(WS\\d{4}/\\d{2}|SS\\d{4})\", e.g. \"WS2020/21\""));

        if (!invalidParams.isEmpty())
            return gson.toJson(new DetailedError()
                    .type("hl: unprocessable entity")
                    .title("The given string does not conform to the specified format.")
                    .invalidParams(invalidParams));

        ChaincodeStub stub = ctx.getStub();

        String jsonStudent = stub.getStringState(matriculationId);

        if(jsonStudent == null || jsonStudent.equals(""))
            return gson.toJson(new GenericError()
                    .type("hl: not found")
                    .title("There is no student for the given matriculationId."));

        Student student = null;

        try
        {
            student = gson.fromJson(jsonStudent, Student.class);
        }
        catch(Exception e)
        {
            return gson.toJson(new GenericError()
                    .type("hl: unprocessable ledger state")
                    .title("The state on the ledger does not conform to the specified format."));
        }

        for (SubjectMatriculation item: student.getMatriculationStatus()) {
            if (item.getFieldOfStudy() == fieldOfStudyValue) {
                for (String existingSemester: item.getSemesters()) {
                    if (existingSemester.equals(semester))
                        return "";
                }
                item.addsemestersItem(semester);
                return "";
            }
        }

        student.addMatriculationStatusItem(new SubjectMatriculation()
                .fieldOfStudy(fieldOfStudyValue)
                .semesters(new ArrayList<String>()
                {{add(semester);}})
        );

        return "";
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

        List<SubjectMatriculation> immatriculationStatus = student.getMatriculationStatus();

        if (immatriculationStatus == null || immatriculationStatus.size() == 0)
            list.add(new InvalidParameter()
                    .name("matriculationStatus")
                    .reason("Matriculation status must not be empty"));
        else {

            ArrayList<SubjectMatriculation.FieldOfStudyEnum> existingFields = new ArrayList<SubjectMatriculation.FieldOfStudyEnum>();

            for (SubjectMatriculation subInterval: immatriculationStatus) {

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

                if (subInterval.getSemesters() == null || subInterval.getSemesters().size() == 0)
                    list.add(new InvalidParameter()
                            .name("SubjectMatriculationInterval.intervals")
                            .reason("Intervals must not be empty"));

                for (String semester: subInterval.getSemesters()) {
                    if (semester == null || semester.equals(""))
                        list.add(new InvalidParameter()
                                .name("matriculationStatus.semesters")
                                .reason("A semester must not be empty."));

                    if (semesterFormatValid(semester) && student.getBirthDate() != null) {

                        int semesterYear = Integer.parseInt(semester.substring(2, 6));
                        if (semesterYear < student.getBirthDate().getYear()) {
                            list.add(new InvalidParameter()
                                    .name("matriculationStatus.semesters")
                                    .reason("First semester must not be earlier than birth date."));
                        }
                    }

                    if (!semesterFormatValid(semester))
                        list.add(new InvalidParameter()
                                .name("matriculationStatus.semesters")
                                .reason("Semester must be the following format \"(WS\\d{4}/\\d{2}|SS\\d{4})\", e.g. \"WS2020/21\""));
                }
            }
        }

        return list;
    }

    public boolean semesterFormatValid(String semester) {
        Pattern pattern = Pattern.compile("^(WS\\d{4}/\\d{2}|SS\\d{4})");
        Matcher matcher = pattern.matcher(semester);
        return matcher.matches();
    }
}
