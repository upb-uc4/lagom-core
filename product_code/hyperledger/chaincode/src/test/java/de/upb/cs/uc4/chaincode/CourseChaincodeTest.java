package de.upb.cs.uc4.chaincode;

import de.upb.cs.uc4.chaincode.model.Course;
import de.upb.cs.uc4.chaincode.model.Error;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.*;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.threeten.bp.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class CourseChaincodeTest {

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
                            gson.fromJson(this.value, Course.class),
                            gson.fromJson(other.value, Course.class));
        }
    }

    private final class MockCourseResultIterator implements QueryResultsIterator<KeyValue> {

        private final List<KeyValue> courseList;

        MockCourseResultIterator() {
            super();
            courseList = new ArrayList<KeyValue>();
            courseList.add(new MockKeyValue("course1",
                    "{ \"courseId\": \"course1\",\n" +
                            "  \"courseName\": \"courseName1\",\n" +
                            "  \"courseType\": \"Lecture\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 3,\n" +
                            "  \"lecturerId\": \"lecturer1\",\n" +
                            "  \"maxParticipants\": 100,\n" +
                            "  \"currentParticipants\": 0,\n" +
                            "  \"courseLanguage\": \"English\",\n" +
                            "  \"courseDescription\": \"some lecture\" }"));
            courseList.add(new MockKeyValue("course2",
                    "{ \"courseId\": \"course2\",\n" +
                            "  \"courseName\": \"courseName2\",\n" +
                            "  \"courseType\": \"Seminar\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 5,\n" +
                            "  \"lecturerId\": \"lecturer2\",\n" +
                            "  \"maxParticipants\": 12,\n" +
                            "  \"currentParticipants\": 3,\n" +
                            "  \"courseLanguage\": \"English\",\n" +
                            "  \"courseDescription\": \"some seminar\" }"));
            courseList.add(new MockKeyValue("course3",
                    "{ \"courseId\": \"course3\",\n" +
                            "  \"courseName\": \"courseName3\",\n" +
                            "  \"courseType\": \"Project Group\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 20,\n" +
                            "  \"lecturerId\": \"lecturer3\",\n" +
                            "  \"maxParticipants\": 16,\n" +
                            "  \"currentParticipants\": 16,\n" +
                            "  \"courseLanguage\": \"German\",\n" +
                            "  \"courseDescription\": \"some project group\" }"));
        }

        @Override
        public void close() throws Exception {

        }

        @Override
        public Iterator<KeyValue> iterator() {
            return courseList.iterator();
        }
    }

    private final class MockChaincodeStub implements ChaincodeStub {

        public List<MockKeyValue> putStates;

        MockChaincodeStub() {
            putStates = new ArrayList<MockKeyValue>();
        }

        @Override
        public void putStringState(String key, String value) {
            putStates.add(new MockKeyValue(key, value));
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
    public void queryExistingCourse() {
        CourseChaincode contract = new CourseChaincode();
        GsonWrapper gson = new GsonWrapper();
        Context ctx = mock(Context.class);
        ChaincodeStub stub = mock(ChaincodeStub.class);
        when(ctx.getStub()).thenReturn(stub);
        when(stub.getStringState("course1")).thenReturn("{ \"courseId\": \"course1\",\n" +
                "  \"courseName\": \"courseName1\",\n" +
                "  \"courseType\": \"Lecture\",\n" +
                "  \"startDate\": \"2020-06-29\",\n" +
                "  \"endDate\": \"2020-06-29\",\n" +
                "  \"ects\": 3,\n" +
                "  \"lecturerId\": \"lecturer1\",\n" +
                "  \"maxParticipants\": 100,\n" +
                "  \"currentParticipants\": 0,\n" +
                "  \"courseLanguage\": \"English\",\n" +
                "  \"courseDescription\": \"some lecture\" }");
        Course course = gson.fromJson(contract.getCourseById(ctx, "course1"), Course.class);
        assertThat(course).isEqualTo(new Course()
                .courseId("course1")
                .courseName("courseName1")
                .courseType(Course.CourseTypeEnum.LECTURE)
                .startDate(LocalDate.parse("2020-06-29"))
                .endDate(LocalDate.parse("2020-06-29"))
                .ects(3)
                .lecturerId("lecturer1")
                .maxParticipants(100)
                .currentParticipants(0)
                .courseLanguage(Course.CourseLanguageEnum.ENGLISH)
                .courseDescription("some lecture"));
    }

    @Nested
    class AddCourseTransaction {

        @Test
        public void addNonExistingCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }");
            assertThat(stub.putStates.get(0)).isEqualTo(new MockKeyValue("course1",
                    "{ \"courseId\": \"course1\",\n" +
                            "  \"courseName\": \"courseName1\",\n" +
                            "  \"courseType\": \"Lecture\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 3,\n" +
                            "  \"lecturerId\": \"lecturer1\",\n" +
                            "  \"maxParticipants\": 100,\n" +
                            "  \"currentParticipants\": 0,\n" +
                            "  \"courseLanguage\": \"English\",\n" +
                            "  \"courseDescription\": \"some lecture\" }"));
        }

        @Test
        public void addEmptyNameCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("10")
                            .detail("Course name must not be empty"));
        }

        @Test
        public void addInvalidCourseTypeCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"something invalid\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("20")
                            .detail("Course type must be one of [\"Lecture\", \"Seminar\", \"Project Group\"]")
            );
        }

        @Test
        public void addInvalidStartDateCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"something invalid\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("30")
                            .detail("startDate must be the following format \"yyyy-mm-dd\"")
            );
        }

        @Test
        public void addInvalidEndDateCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"something invalid\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("40")
                            .detail("endDate must be the following format \"yyyy-mm-dd\"")
            );
        }

        @Test
        public void addNegativeEctsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": -1,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("50")
                            .detail("ects must be a positive integer number")
            );
        }

        @Test
        public void addStringEctsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": \"something invalid\",\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("50")
                            .detail("ects must be a positive integer number")
            );
        }

        @Test
        public void addEmptyLecturerIdCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("60")
                            .detail("lecturerID unknown")
            );
        }

        @Test
        public void addNegativeMaxParticipantsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": -1,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("70")
                            .detail("maxParticipants must be a positive integer number")
            );
        }

        @Test
        public void addInvalidLanguageCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.addCourse(ctx, "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"something invalid\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("80")
                            .detail("language must be one of [\"German\", \"English\"]")
            );
        }
    }


    @Nested
    class UpdateCourseByIdTransaction {

        @Test
        public void updateCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }");
            assertThat(stub.putStates.get(0)).isEqualTo(new MockKeyValue("course1",
                    "{ \"courseId\": \"course1\",\n" +
                            "  \"courseName\": \"courseName1\",\n" +
                            "  \"courseType\": \"Lecture\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 3,\n" +
                            "  \"lecturerId\": \"lecturer1\",\n" +
                            "  \"maxParticipants\": 100,\n" +
                            "  \"currentParticipants\": 0,\n" +
                            "  \"courseLanguage\": \"English\",\n" +
                            "  \"courseDescription\": \"some lecture\" }"));
        }

        @Test
        public void updateFaultyCourseId() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course2\",\n" +
                            "  \"courseName\": \"\",\n" +
                            "  \"courseType\": \"Lecture\",\n" +
                            "  \"startDate\": \"2020-06-29\",\n" +
                            "  \"endDate\": \"2020-06-29\",\n" +
                            "  \"ects\": 3,\n" +
                            "  \"lecturerId\": \"lecturer1\",\n" +
                            "  \"maxParticipants\": 100,\n" +
                            "  \"currentParticipants\": 0,\n" +
                            "  \"courseLanguage\": \"English\",\n" +
                            "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("00")
                            .detail("Course ID and ID in path do not match"));
        }

        @Test
        public void updateEmptyNameCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("10")
                            .detail("Course name must not be empty"));
        }

        @Test
        public void updateInvalidCourseTypeCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"something invalid\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("20")
                            .detail("Course type must be one of [\"Lecture\", \"Seminar\", \"Project Group\"]")
            );
        }

        @Test
        public void updateInvalidStartDateCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"something invalid\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("30")
                            .detail("startDate must be the following format \"yyyy-mm-dd\"")
            );
        }

        @Test
        public void updateInvalidEndDateCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"something invalid\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("40")
                            .detail("endDate must be the following format \"yyyy-mm-dd\"")
            );
        }

        @Test
        public void updateNegativeEctsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": -1,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("50")
                            .detail("ects must be a positive integer number")
            );
        }

        @Test
        public void updateStringEctsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": \"something invalid\",\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("50")
                            .detail("ects must be a positive integer number")
            );
        }

        @Test
        public void updateEmptyLecturerIdCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("60")
                            .detail("lecturerID unknown")
            );
        }

        @Test
        public void updateNegativeMaxParticipantsCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": -1,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"English\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("70")
                            .detail("maxParticipants must be a positive integer number")
            );
        }

        @Test
        public void updateInvalidLanguageCourse() {
            CourseChaincode contract = new CourseChaincode();
            GsonWrapper gson = new GsonWrapper();
            Context ctx = mock(Context.class);
            MockChaincodeStub stub = new MockChaincodeStub();
            when(ctx.getStub()).thenReturn(stub);
            Error error = gson.fromJson(contract.updateCourseById(ctx, "course1",
                    "{ \"courseId\": \"course1\",\n" +
                    "  \"courseName\": \"courseName1\",\n" +
                    "  \"courseType\": \"Lecture\",\n" +
                    "  \"startDate\": \"2020-06-29\",\n" +
                    "  \"endDate\": \"2020-06-29\",\n" +
                    "  \"ects\": 3,\n" +
                    "  \"lecturerId\": \"lecturer1\",\n" +
                    "  \"maxParticipants\": 100,\n" +
                    "  \"currentParticipants\": 0,\n" +
                    "  \"courseLanguage\": \"something invalid\",\n" +
                    "  \"courseDescription\": \"some lecture\" }"), Error.class);
            assertThat(error).isEqualTo(
                    new Error()
                            .name("80")
                            .detail("language must be one of [\"German\", \"English\"]")
            );
        }
    }
}
