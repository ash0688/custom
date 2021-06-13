import com.github.opendevl.JFlat;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.winium.DesktopOptions;
import org.openqa.selenium.winium.WiniumDriver;
import org.openqa.selenium.winium.WiniumDriverService;


import static java.nio.file.Files.readAllBytes;

public class Main2 {

    public static void main(String[] args) throws IOException, CsvException, ParseException, InterruptedException, URISyntaxException {

        //System count start & end from CSV. It is not recommended to set more than 500 at a time due to API requests per time limitations
        int start = 1300;
        int end = 1500;
        String cmdrName = "ASH D";
        String cmdrEDSMAPIKEY = "enter your key";
        String systemsPopulatedPath = "C:\\Users\\ASH\\Downloads\\systemsPopulated";
        String EDPFpath = "C:\\Program Files (x86)\\EDPathFinder\\EDPathFinder.exe";
        String winiumPath = "C:\\Users\\ASH\\IdeaProjects\\custom2\\custom2_mvn\\src\\resources\\Winium.Desktop.Driver.exe";


        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated.csv");
        //parsePopulatedPlanets("C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.json", "C:\\Users\\ASH\\Downloads\\systemsPopulated\\systemsPopulated2.csv");
        requestUpdateTrafficData(start, end, cmdrName, cmdrEDSMAPIKEY, systemsPopulatedPath, EDPFpath, winiumPath);

    }

    public static void requestUpdateTrafficData(int start, int end, String cmdrName, String cmdrEDSMAPIKEY, String systemsPopulatedPath, String EDPFpath, String winiumPath) throws IOException, CsvException, ParseException, InterruptedException, URISyntaxException {

        Set<String> systemListToRoute = new HashSet<String>();
        ;
        String cmdrLastLocSystem = null;

        //scan csv into array
        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(systemsPopulatedPath + "\\systemsPopulated_modified.csv"));) {
            while (scanner.hasNextLine()) {
                records.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //HTTP Client init
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofDays(1000))
                .setConnectTimeout(Timeout.ofDays(1000))
                .setResponseTimeout(Timeout.ofDays(1000))
                .build();

        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        CloseableHttpResponse response = null;


        BidiMap<Integer,String> map = new DualHashBidiMap<>();

        //Iterate through systems and update
        for (int i = start; i < end; i++) {

            System.out.println("system= " + records.get(i).get(1) + "; count= " + i);
            String systemName = records.get(i).get(1).replace("\"", "");


            try {

                HttpGet request = new HttpGet("https://www.edsm.net/api-system-v1/traffic");

                URI uri = new URIBuilder(request.getUri())
                        .addParameter("systemName", systemName)
                        .build();

                request.setUri(uri);

                response = client.execute(request);

                String json = EntityUtils.toString(response.getEntity());

                String[] ss = json.split(",");

                String[] weekTraffic = ss[7].split(":");
                String systemWeekTraffic = weekTraffic[1];
                String[] dayTraffic = ss[8].split(":");
                dayTraffic = dayTraffic[1].split("}");
                String systemDayTraffic = dayTraffic[0];

                if (systemWeekTraffic.matches("0") && systemDayTraffic.matches("0")) {
                    updateCSV(systemsPopulatedPath + "\\systemsPopulated_modified.csv", systemWeekTraffic, i, 12);
                    System.out.println("week traffic updated =" + systemWeekTraffic);
                    updateCSV(systemsPopulatedPath + "\\systemsPopulated_modified.csv", systemDayTraffic, i, 13);
                    System.out.println("day traffic updated =" + systemDayTraffic);
                    map.put(i, systemName);
                    systemListToRoute.add(systemName);
                }


            } finally {
                response.close();
                //Thread.sleep(1500);
            }
        }

        System.out.println("Route to optimize : " + systemListToRoute);



        //get CMDR's current location
        try {
            HttpGet request = new HttpGet("https://www.edsm.net/api-logs-v1/get-position");

            URI uri = new URIBuilder(request.getUri())
                    .addParameter("commanderName", cmdrName.replace("\"", ""))
                    .addParameter("apiKey", cmdrEDSMAPIKEY.replace("\"", ""))
                    .build();

            request.setUri(uri);

            response = client.execute(request);

            String json = EntityUtils.toString(response.getEntity());

            String[] ss = json.split(",");
            String[] cmdrLastLoc = ss[2].split(":");
            cmdrLastLocSystem = cmdrLastLoc[1];


        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            response.close();
        }

        System.out.println("Current system = " + cmdrLastLocSystem);

        client.close();

        //Add route to EDPathFinder and save optimized route as csv

        DesktopOptions options = new DesktopOptions(); //Instantiate Winium Desktop Options
        options.setApplicationPath(EDPFpath);
        File driverPath = new File(winiumPath);
        WiniumDriverService service = new WiniumDriverService.Builder().usingDriverExecutable(driverPath).usingPort(9999).withVerbose(true).withSilent(false).buildDesktopService();

        String routeOptimizedFileName = System.currentTimeMillis() +"_route_optimized.csv";
        try {
            service.start();
            WiniumDriver driver = new WiniumDriver(service,options);
            Thread.sleep(17000);
            driver.getWindowHandle();

            do {
                Thread.sleep(1000);
            }
            while (driver.findElement(By.name("Tools")).isEnabled() == false);

            driver.findElement(By.name("Tools")).click();
            driver.findElement(By.name("Mission and Custom Router")).click();
            driver.getWindowHandle();

            Iterator setValues = systemListToRoute.iterator();
            while (setValues.hasNext()) {
                driver.findElement(By.name("Custom Routing")).findElement(By.name("Add Custom Stop")).findElement(By.className("QLineEdit")).sendKeys((CharSequence) setValues.next());
                driver.findElement(By.name("Add Stop")).click();
            }

            driver.findElement(By.name("Optimize Route")).click();

            do {
                Thread.sleep(1000);
            }
            while (driver.findElement(By.name("File")).isEnabled() == false);
            driver.findElement(By.name("File")).click();

            driver.findElement(By.name("Export as CSV...")).click();

            driver.getWindowHandle();
            driver.findElement(By.name("Save file")).findElement(By.name("Previous Locations")).click();
            driver.findElement(By.name("Save file")).findElement(By.name("Address")).sendKeys(systemsPopulatedPath);
            driver.findElement(By.name("Save file")).findElement(By.name("Address")).click();
            driver.findElement(By.name("Save file")).findElement(By.name("Go to \""+ systemsPopulatedPath +"\"")).click();


            driver.findElement(By.name("Save file")).findElement(By.className("Edit")).click();

            driver.findElement(By.name("Save file")).findElement(By.className("Edit")).sendKeys(routeOptimizedFileName);
            driver.findElement(By.name("Save file")).findElement(By.name("Save")).click();

            //Thread.sleep(60000);
            driver.quit();

        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            service.stop();
        }

        //update optimized route into core csv
        //scan csv into array
        List<List<String>> optimizedRoute = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(systemsPopulatedPath + "\\" + routeOptimizedFileName));) {
            while (scanner.hasNextLine()) {
                optimizedRoute.add(getRecordFromLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            for (int j = 2; j< optimizedRoute.size(); j++) {
                String systemToUpdate = optimizedRoute.get(j).get(0).split(";")[0].replace("\"", "");
                int row = map.getKey(systemToUpdate);
                updateCSV(systemsPopulatedPath + "\\systemsPopulated_modified.csv", Integer.toString(j), row, 14);
            }
        } catch (Exception e){
            e.printStackTrace();
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

    public static void parsePopulatedPlanets(String pathInput, String pathOutput) throws IOException {

        String str = new String(readAllBytes(Paths.get(pathInput)));

        JFlat flatMe = new JFlat(str);

        //directly write the JSON document to CSV
        flatMe.json2Sheet().write2csv(pathOutput);

    }

}