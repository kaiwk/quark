package factorial_md;

public class List_reflect_Class_ extends reflect.Class implements io.datawire.quark.runtime.QObject {
    public static reflect.Class singleton = new List_reflect_Class_();
    public List_reflect_Class_() {
        super("List<reflect.Class>");
        (this).name = "List";
        (this).parameters = new java.util.ArrayList(java.util.Arrays.asList(new Object[]{"reflect.Class"}));
        (this).fields = new java.util.ArrayList(java.util.Arrays.asList(new Object[]{}));
        (this).methods = new java.util.ArrayList(java.util.Arrays.asList(new Object[]{}));
    }
    public Object construct(java.util.ArrayList<Object> args) {
        return new java.util.ArrayList<reflect.Class>();
    }
    public String _getClass() {
        return (String) (null);
    }
    public Object _getField(String name) {
        return null;
    }
    public void _setField(String name, Object value) {}
}