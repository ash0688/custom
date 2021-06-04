import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)

public class APOD {

    public final String url;
    public final String id;
    public final String id64;
    public final String name;
    public Map<String, String> traffic;
    public String week;
    public String day;

    private static String unpackWeekFromNestedObject(Map<String, String> traffic) {
        String week = traffic.get("name");
        return week;
    }

    public APOD(@JsonProperty("url") String url,
                @JsonProperty("id") String id,
                @JsonProperty("id64") String id64,
                @JsonProperty("name") String name,
                @JsonProperty("traffic") Map<String, String> traffic,
                @JsonProperty("week") String week,
                @JsonProperty("day") String day)

                 {



        this.url = url;
        this.id = id;
        this.id64 = id64;
        this.name = name;
        //this.traffic = traffic;
        this.week = APOD.unpackWeekFromNestedObject(traffic);
        this.day = day;



    }
}