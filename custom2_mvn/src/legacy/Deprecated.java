import com.github.opendevl.JFlat;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import java.net.CookieManager;
import java.io.*;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.readAllBytes;

public class Deprecated {

    private static CookieManager cookieManager;

    public static void main(String[] args) throws IOException, CsvException {

        int depth = 2;

        //parsePopulatedPlanets();
        requestUpdateTrafficData(depth);

    }

    public static void parsePopulatedPlanets() throws IOException {

        String str = new String(readAllBytes(Paths.get("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\systemsPopulated2.json")));

        JFlat flatMe = new JFlat(str);

        //directly write the JSON document to CSV
        flatMe.json2Sheet().write2csv("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\systemsPopulated2.csv");

    }

    public static void requestUpdateTrafficData(int limit) throws IOException, CsvException {

        //scan csv into array
        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv"));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        URL url = new URL("https://www.edsm.net/api-system-v1/traffic");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        for (int i=1; i<limit; i++) {


            System.out.println(records.get(i).get(1));

            String systemName = records.get(i).get(1);


            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(0);
            con.setReadTimeout(0);
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(true);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("systemName", systemName);
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            System.out.println(ParameterStringBuilder.getParamsString(parameters));
            out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
            //System.out.println(String.valueOf(parameters));
            //out.writeBytes(String.valueOf(parameters));
            out.flush();
            out.close();

            /*
            String cookiesHeader = con.getHeaderField("Set-Cookie");
            List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
            cookies.forEach(cookie -> cookieManager.getCookieStore().add(null, cookie));
            Optional<HttpCookie> usernameCookie = cookies.stream()
                    .findAny().filter(cookie -> cookie.getName().equals("username"));
            if (usernameCookie == null) {
                cookieManager.getCookieStore().add(null, new HttpCookie("username", "john"));
            }
            con.disconnect();
            con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Cookie",
                    StringUtils.join(cookieManager.getCookieStore().getCookies(), ";"));
            */


            int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM) {
                String location = con.getHeaderField("Location");
                URL newUrl = new URL(location);
                con = (HttpURLConnection) newUrl.openConnection();
            }

            System.out.println("status = " + status);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            System.out.println(FullResponseBuilder.getFullResponse(con));
            System.out.println(content.toString());
            System.out.println(content.length());

            in.close();

            if (content.length() == 2) con.disconnect();









            String[] ss=content.toString().split(",");

            /*
            for(int j=0;j<ss.length;j++)
            {
                //System.out.println(ss[j]);
            }
            */

            String [] weekTraffic = ss[7].split(":");
            String systemWeekTraffic = weekTraffic[1];


            updateCSV("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv", systemWeekTraffic, i, 12);

            System.out.println("week traffic updated =" + systemWeekTraffic);

            String [] dayTraffic = ss[8].split(":");
            dayTraffic = dayTraffic[1].split("}");
            String systemDayTraffic = dayTraffic[0];


            updateCSV("C:\\Users\\ASH\\IdeaProjects\\Elite Farmer\\src\\main\\resources\\Copy of systemsPopulated_modified.csv", systemDayTraffic, i, 13);

            System.out.println("day traffic updated =" + systemDayTraffic);







        }


        con.disconnect();

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


}
