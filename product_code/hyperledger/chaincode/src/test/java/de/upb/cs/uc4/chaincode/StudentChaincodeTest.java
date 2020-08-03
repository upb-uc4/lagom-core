package de.upb.cs.uc4.chaincode;

import de.upb.cs.uc4.chaincode.model.*;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.threeten.bp.LocalDate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class StudentChaincodeTest {

    private final class MockKeyValue implements KeyValue {

        private final String key;
        private final String value;

        MockKeyValue(final String key, final String value) {
            super();
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public String getStringValue() {
            return this.value;
        }

        @Override
        public byte[] getValue() {
            return this.value.getBytes();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MockKeyValue other = (MockKeyValue) o;
            GsonWrapper gson = new GsonWrapper();
            return Objects.equals(this.key, other.key) &&
                    Objects.equals(
                            gson.fromJson(this.value, Student.class),
                            gson.fromJson(other.value, Student.class));
        }
    }

    private final class MockStudentResultIterator implements QueryResultsIterator<KeyValue> {

        private final List<KeyValue>studentList;

        MockStudentResultIterator() {
            super();
            studentList = new ArrayList<KeyValue>();
            studentList.add(new MockKeyValue("0000001",
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2020-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
            studentList.add(new MockKeyValue("0000002",
                    "{\n" +
                            "  \"matriculationId\": \"0000002\",\n" +
                            "  \"firstName\": \"firstName2\",\n" +
                            "  \"lastName\": \"lastName2\",\n" +
                            "  \"birthDate\": \"2020-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Philosophy\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"SS214\",\n" +
                            "          \"lastSemester\": \"WS2015\"\n" +
                            "        },\n" +
                            "       {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
            studentList.add(new MockKeyValue("0000003",
                    "{\n" +
                            "  \"matriculationId\": \"0000003\",\n" +
                            "  \"firstName\": \"firstName3\",\n" +
                            "  \"lastName\": \"lastName3\",\n" +
                            "  \"birthDate\": \"2020-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Economics\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Physics\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
        }

        @Override
        public void close() throws Exception {

        }

        @Override
        public Iterator<KeyValue> iterator() {
            return studentList.iterator();
        }
    }

    private final class MockChaincodeStub implements ChaincodeStub {

        public List<MockKeyValue> putStates;
        private int index = 0;

        MockChaincodeStub() {
            putStates = new ArrayList<MockKeyValue>();
        }

        @Override
        public void putStringState(String key, String value) {
            putStates.add(index++,new MockKeyValue(key, value));
        }

        @Override
        public String getStringState(String key) {
            for (MockKeyValue keyValue: putStates) {
                    if (keyValue.key.equals(key))
                        return keyValue.value;
            }
            return "";
        }

        @Override
        public List<byte[]> getArgs() {
            return null;
        }

        @Override
        public List<String> getStringArgs() {
            return null;
        }

        @Override
        public String getFunction() {
            return null;
        }

        @Override
        public List<String> getParameters() {
            return null;
        }

        @Override
        public String getTxId() {
            return null;
        }

        @Override
        public String getChannelId() {
            return null;
        }

        @Override
        public Chaincode.Response invokeChaincode(String chaincodeName, List<byte[]> args, String channel) {
            return null;
        }

        @Override
        public byte[] getState(String key) {
            return new byte[0];
        }

        @Override
        public byte[] getStateValidationParameter(String key) {
            return new byte[0];
        }

        @Override
        public void putState(String key, byte[] value) {

        }

        @Override
        public void setStateValidationParameter(String key, byte[] value) {

        }

        @Override
        public void delState(String key) {

        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey) {
            return null;
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(String startKey, String endKey, int pageSize, String bookmark) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String objectType, String... attributes) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(CompositeKey compositeKey) {
            return null;
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(CompositeKey compositeKey, int pageSize, String bookmark) {
            return null;
        }

        @Override
        public CompositeKey createCompositeKey(String objectType, String... attributes) {
            return null;
        }

        @Override
        public CompositeKey splitCompositeKey(String compositeKey) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getQueryResult(String query) {
            return null;
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(String query, int pageSize, String bookmark) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyModification> getHistoryForKey(String key) {
            return null;
        }

        @Override
        public byte[] getPrivateData(String collection, String key) {
            return new byte[0];
        }

        @Override
        public byte[] getPrivateDataHash(String collection, String key) {
            return new byte[0];
        }

        @Override
        public byte[] getPrivateDataValidationParameter(String collection, String key) {
            return new byte[0];
        }

        @Override
        public void putPrivateData(String collection, String key, byte[] value) {

        }

        @Override
        public void setPrivateDataValidationParameter(String collection, String key, byte[] value) {

        }

        @Override
        public void delPrivateData(String collection, String key) {

        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByRange(String collection, String startKey, String endKey) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String compositeKey) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, CompositeKey compositeKey) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String objectType, String... attributes) {
            return null;
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(String collection, String query) {
            return null;
        }

        @Override
        public void setEvent(String name, byte[] payload) {

        }

        @Override
        public ChaincodeEventPackage.ChaincodeEvent getEvent() {
            return null;
        }

        @Override
        public ProposalPackage.SignedProposal getSignedProposal() {
            return null;
        }

        @Override
        public Instant getTxTimestamp() {
            return null;
        }

        @Override
        public byte[] getCreator() {
            return new byte[0];
        }

        @Override
        public Map<String, byte[]> getTransient() {
            return null;
        }

        @Override
        public byte[] getBinding() {
            return new byte[0];
        }

        @Override
        public String getMspId() {
            return null;
        }
    }

    @Test
    public void queryExistingStudent() {
        StudentChaincode contract = new StudentChaincode();
        GsonWrapper gson = new GsonWrapper();
        Context ctx = mock(Context.class);
        ChaincodeStub stub = mock(ChaincodeStub.class);
        when(ctx.getStub()).thenReturn(stub);
        when(stub.getStringState("0000001")).thenReturn("{\n" +
                "  \"matriculationId\": \"0000001\",\n" +
                "  \"firstName\": \"firstName1\",\n" +
                "  \"lastName\": \"lastName1\",\n" +
                "  \"birthDate\": \"2000-07-21\",\n" +
                "  \"matriculationStatus\": [\n" +
                "    {\n" +
                "      \"fieldOfStudy\": \"Computer Science\",\n" +
                "      \"intervals\": [\n" +
                "        {\n" +
                "          \"firstSemester\": \"WS2018\",\n" +
                "          \"lastSemester\": \"SS2020\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        String tmp = contract.getMatriculationData(ctx, "0000001");
        Student student = gson.fromJson(contract.getMatriculationData(ctx, "0000001"), Student.class);
        assertThat(student).isEqualTo(new Student()
                .matriculationId("0000001")
                .firstName("firstName1")
                .lastName("lastName1")
                .birthDate(LocalDate.parse("2000-07-21"))
                .matriculationStatus(new ArrayList<SubjectMatriculation>()
                {{
                    add(new SubjectMatriculation()
                            .fieldOfStudy(SubjectMatriculation.FieldOfStudyEnum.COMPUTER_SCIENCE)
                            .intervals(new ArrayList<MatriculationInterval>()
                            {{
                                add(new MatriculationInterval()
                                        .firstSemester("WS2018")
                                        .lastSemester("SS2020"));
                            }}));
                }}));
    }

    @Test
    public void deleteNonExistingStudent() {
        StudentChaincode contract = new StudentChaincode();
        GsonWrapper gson = new GsonWrapper();
        Context ctx = mock(Context.class);
        ChaincodeStub stub = mock(ChaincodeStub.class);
        when(ctx.getStub()).thenReturn(stub);
        when(stub.getStringState("0000001")).thenReturn("{\n" +
                "  \"matriculationId\": \"0000001\",\n" +
                "  \"firstName\": \"firstName1\",\n" +
                "  \"lastName\": \"lastName1\",\n" +
                "  \"birthDate\": \"2000-07-21\",\n" +
                "  \"matriculationStatus\": [\n" +
                "    {\n" +
                "      \"fieldOfStudy\": \"Computer Science\",\n" +
                "      \"intervals\": [\n" +
                "        {\n" +
                "          \"firstSemester\": \"WS2018\",\n" +
                "          \"lastSemester\": \"SS2020\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        assertThat(contract.deleteStudent(ctx, "notExisting")).isEqualTo(
                gson.toJson(new DetailedError()
                        .type("Not found")
                        .title("There is no student for the given matriculationId.")));
    }

    @Nested
    class AddStudentTransaction {

        @Test
        public void immatriculateNonExistingStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            contract.addMatriculationData(ctx, "{\n" +
                    "  \"matriculationId\": \"0000001\",\n" +
                    "  \"firstName\": \"firstName1\",\n" +
                    "  \"lastName\": \"lastName1\",\n" +
                    "  \"birthDate\": \"2000-07-21\",\n" +
                    "  \"matriculationStatus\": [\n" +
                    "    {\n" +
                    "      \"fieldOfStudy\": \"Computer Science\",\n" +
                    "      \"intervals\": [\n" +
                    "        {\n" +
                    "          \"firstSemester\": \"WS2018\",\n" +
                    "          \"lastSemester\": \"WS2018\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
            assertThat(stub.putStates.get(0)).isEqualTo(new MockKeyValue("0000001",
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"WS2018\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
        }

        @Test
        public void immatriculateExistingStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("0000001")).thenReturn("{\n" +
                    "  \"matriculationId\": \"0000001\",\n" +
                    "  \"firstName\": \"firstName1\",\n" +
                    "  \"lastName\": \"lastName1\",\n" +
                    "  \"birthDate\": \"2000-07-21\",\n" +
                    "  \"matriculationStatus\": [\n" +
                    "    {\n" +
                    "      \"fieldOfStudy\": \"Computer Science\",\n" +
                    "      \"intervals\": [\n" +
                    "        {\n" +
                    "          \"firstSemester\": \"WS2018\",\n" +
                    "          \"lastSemester\": \"SS2020\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}");
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Conflict")
                            .title("There is already a student for the given matriculationId."));
        }

        @Test
        public void immatriculateEmptyFirstNameStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("firstName")
                                        .reason("First name must not be empty"));
                            }}));
        }

        @Test
        public void immatriculateEmptyLastNameStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("lastName")
                                        .reason("Last name must not be empty"));
                            }}));
        }

        @Test
        public void immatriculateInvalidBirthDateStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"something invalid\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("birthDate")
                                        .reason("Birth date must be the following format \"yyyy-mm-dd\""));
                            }}));
        }

        @Test
        public void immatriculateEmptyMatriculationStatusStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("matriculationStatus")
                                        .reason("Matriculation status must not be empty"));
                            }}));
        }

        @Test
        public void immatriculateEmptyIntervalStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.intervals")
                                        .reason("Intervals must not be empty"));
                            }}));
        }

        @Test
        public void immatriculateEmptyFieldOfStudyStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.fieldOfStudy")
                                        .reason("Field of study must not be empty"));
                            }}));
        }

        @Test
        public void immatriculateInvalidFirstSemesterStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WWS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));
                            }}));
        }

        @Test
        public void immatriculateInvalidLastSemesterStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS20200\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.lastSemester")
                                        .reason("Last semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));
                            }}));
        }

        @Test
        public void addNonExistingXssStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            contract.addMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1<p>alert(\\\"XSS\\\");</p>\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
            assertThat(stub.putStates.get(0)).isEqualTo(new MockKeyValue("0000001",
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1alert(\\\"XSS\\\");\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
        }
    }

    @Nested
    class UpdateStudentByIdTransaction {

        @Test
        public void updateStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            stub.putStringState("0000001",
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
            contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"WS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}");
            assertThat(stub.putStates.get(1)).isEqualTo(new MockKeyValue("0000001",
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"WS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"));
        }

        @Test
        public void updateNonExistingStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("0000001")).thenReturn("");
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"WS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"), DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Not Found")
                            .title("There is no student for the given matriculationId."));
        }

        @Test
        public void updateEmptyFirstNameStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("firstName")
                                        .reason("First name must not be empty"));
                            }}));
        }

        @Test
        public void updateEmptyLastNameStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("lastName")
                                        .reason("Last name must not be empty"));
                            }}));
        }

        @Test
        public void updateInvalidBirthDateStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"something invalid\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("birthDate")
                                        .reason("Birth date must be the following format \"yyyy-mm-dd\""));
                            }}));
        }

        @Test
        public void updateEmptyMatriculationStatusStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("matriculationStatus")
                                        .reason("Matriculation status must not be empty"));
                            }}));
        }

        @Test
        public void updateEmptyIntervalStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.intervals")
                                        .reason("Intervals must not be empty"));
                            }}));
        }

        @Test
        public void updateEmptyFieldOfStudyStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.fieldOfStudy")
                                        .reason("Field of study must not be empty"));
                            }}));
        }

        @Test
        public void updateInvalidFirstSemesterStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WWS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));
                            }}));
        }

        @Test
        public void updateInvalidLastSemesterStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS20200\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.lastSemester")
                                        .reason("Last semester must be the following format \"(WS|SS)\\d{4}\", e.g. \"WS2020\""));
                            }}));
        }

        @Test
        public void updateChronologicallyInvalidYearsStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2020\",\n" +
                            "          \"lastSemester\": \"SS2018\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First and last semester must be in chronological order. " +
                                                "Last semester lays chronologically before first semester."));
                            }}));
        }


        @Test
        public void updateChronologicallyInvalidSemestersStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2020\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First and last semester must be in chronological order. " +
                                                "Last semester lays chronologically before first semester."));
                            }}));
        }

        @Test
        public void updateFirstSemesterBeforeBirthDateStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2020-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.MatriculationInterval.firstSemester")
                                        .reason("First semester must not be earlier than birth date."));
                            }}));
        }

        @Test
        public void updateDuplicateFieldOfStudyStudent() {
            StudentChaincode contract = new StudentChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            DetailedError error = gson.fromJson(contract.updateMatriculationData(ctx,
                    "{\n" +
                            "  \"matriculationId\": \"0000001\",\n" +
                            "  \"firstName\": \"firstName1\",\n" +
                            "  \"lastName\": \"lastName1\",\n" +
                            "  \"birthDate\": \"2000-07-21\",\n" +
                            "  \"matriculationStatus\": [\n" +
                            "    {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2018\",\n" +
                            "          \"lastSemester\": \"SS2020\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    },\n" +
                            "   {\n" +
                            "      \"fieldOfStudy\": \"Computer Science\",\n" +
                            "      \"intervals\": [\n" +
                            "        {\n" +
                            "          \"firstSemester\": \"WS2014\",\n" +
                            "          \"lastSemester\": \"SS2016\"\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"),
                    DetailedError.class);
            assertThat(error).isEqualTo(
                    new DetailedError()
                            .type("Unprocessable Entity")
                            .title("The given string does not conform to the specified json format.")
                            .invalidParams(new ArrayList<InvalidParameter>()
                            {{
                                add(new InvalidParameter()
                                        .name("SubjectMatriculationInterval.fieldOfStudy")
                                        .reason("Each field of study should only appear in one SubjectMatriculationInterval."));
                            }}));
        }
    }
}
