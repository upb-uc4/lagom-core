import com.google.gson.Gson;
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
public class LectureChaincode implements ContractInterface {

    private static Log _logger = LogFactory.getLog(LectureChaincode.class);

    @Transaction()
    public void initLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        _logger.info("initLedger");
        Lecture[] lectures = {
                new Lecture("SSSP", new Lecturer("Karl", "Computer Networks")),
                new Lecture("FoC", new Lecturer("Blömer","Cryptography and Codes"))};

        for (int i=0; i<lectures.length; i++) {
            String key = String.format("%d",i);
            stub.putStringState(key, lectures[i].toJson());
        }
    }

    @Transaction()
    public Lecture[] queryAll (final Context ctx) {
        _logger.info("queryAllLectures");
        ChaincodeStub stub = ctx.getStub();

        // With CouchDB: Query the ledger using an ad-hoc selector
        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"id\":{\"$regex\":\".*\"}}}");

        // Without CouchDB:
        //QueryResultsIterator<KeyValue> results = stub.getStateByRange("0","2");

        ArrayList<Lecture> lectures = new ArrayList<Lecture>();

        // Deserialize queried objects and return them to the application
        Gson gson = new Gson();
        for (KeyValue result: results) {
            lectures.add(gson.fromJson(result.getStringValue(), Lecture.class));
        }
        return lectures.toArray(new Lecture[lectures.size()]);
    }

    @Transaction()
    public void changeLectureId (final Context ctx, final String oldId, final String newId) {
        ChaincodeStub stub = ctx.getStub();

        QueryResultsIterator<KeyValue> results = stub.getQueryResult("{\"selector\":{\"id\":\"" + oldId + "\"}}");

        Gson gson = new Gson();
        for (KeyValue result: results) {
            Lecture lecture = gson.fromJson(result.getStringValue(), Lecture.class);
            lecture.setId(newId);
            stub.putStringState(result.getKey(), lecture.toJson());
        }
    }
}
