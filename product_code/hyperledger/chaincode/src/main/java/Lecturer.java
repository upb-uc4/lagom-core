import com.google.gson.Gson;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


@DataType
public class Lecturer {

    @Property
    private String name;

    @Property
    private String group;

    public Lecturer(final String name, final String group) {
        this.name = name;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
