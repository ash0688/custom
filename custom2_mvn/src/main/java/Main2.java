import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;
import com.jayway.jsonpath.JsonPath;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.*;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;


import static java.nio.file.Files.readAllBytes;

public class Main2 {

    private static CookieManager cookieManager;

    public static void main(String[] args) throws IOException, CsvException {

        int start = 101;
        int end = 102;

        //parsePopulatedPlanets();
        requestUpdateTrafficData(start, end);

    }

    public static void requestUpdateTrafficData(int start, int end) throws IOException, CsvException {

        //scan csv into array
        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv"));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        CloseableHttpClient client = HttpClients.createDefault();

        for (int i=start; i<end; i++) {

            System.out.println("system= "+ records.get(i).get(1) + "; count= " + i);
            String systemName = records.get(i).get(1);

            try {


                Map<String, String> parameters = new HashMap<>();
                parameters.put("systemName", systemName);
                HttpGet request = new HttpGet("https://www.edsm.net/api-system-v1/traffic?" + ParameterStringBuilder.getParamsString(parameters));

                /*
                APOD response = client.execute(request, httpResponse ->
                        mapper.readValue(httpResponse.getEntity().getContent(), APOD.class)); */

                CloseableHttpResponse response = client.execute(request);
                String json = EntityUtils.toString(response.getEntity());

                System.out.println(json);

                String[] ss=json.split(",");

                String [] weekTraffic = ss[7].split(":");
                String systemWeekTraffic = weekTraffic[1];

                updateCSV("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv", systemWeekTraffic, i, 12);

                System.out.println("week traffic updated =" + systemWeekTraffic);

                String [] dayTraffic = ss[8].split(":");
                dayTraffic = dayTraffic[1].split("}");
                String systemDayTraffic = dayTraffic[0];

                updateCSV("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv", systemDayTraffic, i, 13);

                System.out.println("day traffic updated =" + systemDayTraffic);

                response.close();

            } catch (ParseException e) {
                e.printStackTrace();
            }
            finally {

            }

            client.close();

        }


    }

    public static void updateCSV(String fileToUpdate, String replace,
                                 int row, int col) throws IOException, CsvException {

        File inputFile = new File(fileToUpdate);

        // Read existing file
        CSVReader reader = new CSVReader(new FileReader(inputFile));
        List<String[]> csvBody = reader.readAll();
        // get CSV row column  and replace with by using row and column
        csvBody.get(row)[col] = replace;
        reader.close();

        // Write to CSV file which is open
        CSVWriter writer = new CSVWriter(new FileWriter(inputFile));
        writer.writeAll(csvBody);
        writer.flush();
        writer.close();
    }

    public static List<String> getRecordFromLine(String line) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    public static class ParameterStringBuilder {
        public static String getParamsString(Map<String, String> params)
                throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append("&");
            }

            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - 1)
                    : resultString;
        }
    }

    public static class FullResponseBuilder {
        public static String getFullResponse(HttpURLConnection con) throws IOException {
            StringBuilder fullResponseBuilder = new StringBuilder();

            fullResponseBuilder.append(con.getResponseCode())
                    .append(" ")
                    .append(con.getResponseMessage())
                    .append("\n");

            con.getHeaderFields()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null)
                    .forEach(entry -> {

                        fullResponseBuilder.append(entry.getKey())
                                .append(": ");

                        List<String> headerValues = entry.getValue();
                        Iterator<String> it = headerValues.iterator();
                        if (it.hasNext()) {
                            fullResponseBuilder.append(it.next());

                            while (it.hasNext()) {
                                fullResponseBuilder.append(", ")
                                        .append(it.next());
                            }
                        }

                        fullResponseBuilder.append("\n");
                    });

            Reader streamReader = null;

            if (con.getResponseCode() > 299) {
                streamReader = new InputStreamReader(con.getErrorStream());
            } else {
                streamReader = new InputStreamReader(con.getInputStream());
            }

            BufferedReader in = new BufferedReader(streamReader);
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();

            fullResponseBuilder.append("Response: ")
                    .append(content);

            return fullResponseBuilder.toString();
        }
    }
    public static void parsePopulatedPlanets() throws IOException {

        String str = new String(readAllBytes(Paths.get("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\systemsPopulated2.json")));

        JFlat flatMe = new JFlat(str);

        //directly write the JSON document to CSV
        flatMe.json2Sheet().write2csv("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\systemsPopulated2.csv");

    }

}