import com.github.opendevl.JFlat;
import com.google.gson.JsonObject;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.hc.core5.http.ParseException;

import java.io.*;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.readAllBytes;

public class Main3 {

    private static CookieManager cookieManager;

    public static void main(String[] args) throws IOException, CsvException {

        int start = 1;
        int end = 2;

        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.csv");
        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.csv");
        requestUpdateTrafficData(start, end);

    }

    public static String requestUpdateTrafficData(int start, int end) throws IOException, CsvException {

        //scan csv into array
        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated_modified.csv"));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (int i=start; i<end; i++) {

            System.out.println("system= "+ records.get(i).get(1) + "; count= " + i);
            String systemName = records.get(i).get(1);

            String responseStatus = null;
            HttpURLConnection connection = null;
            
            try {


                URL url= new URL("https://www.edsm.net/api-system-v1/traffic");



                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                //connection.setRequestProperty("systemName", systemName);
                connection.setUseCaches(false);
                connection.setDefaultUseCaches(false);
                connection.setDoOutput(true);

                //Send request
                try(DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())){
                    JsonObject jsonParam = new JsonObject();
                    jsonParam.addProperty("systemName", systemName);
                    outputStream.writeBytes(jsonParam.toString());
                    outputStream.flush();
                }

                //Get response

                InputStream inputStream;
                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                if(null == inputStream){
                    return String.valueOf(connection.getResponseCode());
                }


                StringBuilder response = new StringBuilder();
                try (BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while (null != (line = inputBuffer.readLine())) {
                        response.append(line);
                        response.append("\r");
                    }
                }


                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(response.toString());



                //JsonObject jsonObject = new JsonObject(response.toString());


                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    responseStatus = "success";
                } else {
                    responseStatus = String.valueOf(connection.getResponseCode()) + jsonObject.get("errorMessage") ;
                }



                String systemWeekTraffic = jsonObject.getAsString("week");
                String systemDayTraffic = jsonObject.getAsString("day");

                System.out.println(systemWeekTraffic);
                System.out.println(systemDayTraffic);


            } catch (net.minidev.json.parser.ParseException e) {
                e.printStackTrace();
            }
            finally {
                if (null != connection){
                    connection.disconnect();
                }
            }
            System.out.println(responseStatus);
            return responseStatus;
        }


        return null;
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
    public static void parsePopulatedPlanets(String pathInput, String pathOutput) throws IOException {

        String str = new String(readAllBytes(Paths.get(pathInput)));

        JFlat flatMe = new JFlat(str);

        //directly write the JSON document to CSV
        flatMe.json2Sheet().write2csv(pathOutput);

    }

}
