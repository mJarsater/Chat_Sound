
import java.io.*;

public class Storage implements Serializable {

    private String id;
    private byte[] data;

    public Storage(ByteArrayOutputStream baos) {
        setData(baos);
    }

    public Storage(ByteArrayOutputStream baos, String id) {
        setData(baos);
        setId(id);
    }

    public Storage(byte[] data) {
        setData(data);
    }

    public Storage(byte[] data, String id) {
        setData(data);
        setId(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setData(ByteArrayOutputStream baos) {
        data = baos.toByteArray();
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
