import com.google.gson.Gson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * If you want an object to be returned from the chaincode to the calling application in a JSON format
 * you need to use the @DataType annotation.
 *
 * It makes sense to use some JSON library to make objects like this one serializable, as it enables
 * us to easily write such objects to and read them from the ledger.
 */
@DataType
public class Lecture {

    /**
     * Properties that should be present in the JSON representation returned to the application must be marked
     * using the @Property() annotation. Since these are private fields, you must make sure to provide a corresponding
     * getter-method. I am not entirely sure, but it seems like these getter-methods can return either a primitive
     * datatype, or another datatype marked with the @DataType annotation, which makes it serializable for fabric.
     */
    @Property()
    private String id;

    /**
     * This property can also be serialized, since Lecturer is marked by the DataType annotation as well.
     */
    @Property()
    private Lecturer lecturer;

    public Lecture(String id , Lecturer lecturer) {
        this.id = id;
        this.lecturer = lecturer;
    }

    /**
     * This getter-method is required for fabric to be able to serialize the id property.
     * @return Whatever is returned here will appear as the value for the key id in the JSON representation returned
     * to the application.
     */
    public String getId () {
        return this.id;
    }

    /**
     * Again the getter-method must be present to enable fabric to access the annotated property.
     * @return
     */
    public Lecturer getLecturer () {
        return this.lecturer;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
