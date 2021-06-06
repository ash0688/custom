import com.github.opendevl.JFlat;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;


import static java.nio.file.Files.readAllBytes;

public class Main2 {

    public static void main(String[] args) throws IOException, CsvException, ParseException, InterruptedException, URISyntaxException {

        //system number to start
        int start = 600;
        //system number to finish
        int end = 700;
        //path to populated systems file
        String filepath = System.getProperty("user.home") + "\\Downloads\\systemsPopulated\\systemsPopulated_modified.csv" ;

        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.csv");
        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.csv");
        requestUpdateTrafficData(start, end, filepath);

    }

    public static void requestUpdateTrafficData(int start, int end, String filepath) throws IOException, CsvException, ParseException, InterruptedException, URISyntaxException {

        //scan csv into array
        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filepath));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //initiate http client
        CloseableHttpClient client;
        CloseableHttpResponse response = null;

        client = HttpClients.createDefault();

        for (int i=start; i<end; i++) {

            System.out.println("system= "+ records.get(i).get(1) + "; count= " + i);
            String systemName = records.get(i).get(1);
            systemName = systemName.replace("\"", "");

            try {

                HttpGet request = new HttpGet("https://www.edsm.net/api-system-v1/traffic");

                URI uri = new URIBuilder(request.getUri())
                        .addParameter("systemName", systemName)
                        .build();

                request.setUri(uri);

                response = client.execute(request);

                String json = EntityUtils.toString(response.getEntity());

                //parse response and update the csv
                String[] ss=json.split(",");

                String [] weekTraffic = ss[7].split(":");
                String systemWeekTraffic = weekTraffic[1];

                updateCSV(filepath, systemWeekTraffic, i, 12);

                System.out.println("week traffic updated =" + systemWeekTraffic);

                String [] dayTraffic = ss[8].split(":");
                dayTraffic = dayTraffic[1].split("}");
                String systemDayTraffic = dayTraffic[0];

                updateCSV(filepath, systemDayTraffic, i, 13);

                System.out.println("day traffic updated =" + systemDayTraffic);

                Thread.sleep(1000);

            } finally {
                response.close();
            }
        }

        client.close();

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

    public static void parsePopulatedPlanets(String pathInput, String pathOutput) throws IOException {

        String str = new String(readAllBytes(Paths.get(pathInput)));

        JFlat flatMe = new JFlat(str);

        //directly parse the JSON document into CSV
        flatMe.json2Sheet().write2csv(pathOutput);

    }

}
