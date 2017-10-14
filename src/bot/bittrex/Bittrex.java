package bot.bittrex;

import bot.TBot;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bittrex {
    private final String encryptionAlgorithm = "HmacSHA512";
    private String apikey;
    private String secret;
    private final String API_VERSION = "1.1", INITIAL_URL = "https://bittrex.com/api/";
    private final String PUBLIC = "public", MARKET = "market", ACCOUNT = "account";
    private TBot bot;
    private boolean isActive;
    private static final Exception InvalidStringListException = new Exception("Must be in key-value pairs");

    private int retryAttempts;
    private int retryAttemptsLeft;
    private int retryDelaySeconds;

    //for tests
    public Bittrex() {
        isActive = true;
        apikey = "ab56aecc0c904becb7b187bd32583b32";
        secret = "45d2a7a86edf429f93fad8f8fc873c27";
    }

    public Bittrex(TBot bot) {

        //Consider to load these values from .properties file
        isActive = true;
        apikey = "ab56aecc0c904becb7b187bd32583b32";
        secret = "45d2a7a86edf429f93fad8f8fc873c27";
        this.bot = bot;
        retryAttempts = 3;
        retryDelaySeconds = 15;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    private String getResponseBody(final String baseUrl) {
        String result = null;
        final String url = baseUrl + "apikey=" + apikey + "&nonce=" + EncryptionUtility.generateNonce();
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);

            //From Bittrex Developer's Guide - Api
            //we use a standard HMAC-SHA512 signing. Append apikey and nonce to your request and calculate the HMAC hash
            //and include it under an apisign header. Note: the nonce is not respected right now but will be enforced later.

            request.addHeader("apisign", EncryptionUtility.calculateHash(secret, url, encryptionAlgorithm));
            HttpResponse httpResponse = client.execute(request);

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer resultBuffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                resultBuffer.append(line);
            }
            result = resultBuffer.toString();

        } catch (UnknownHostException e) {

            if(retryAttemptsLeft-- > 0) {
                System.err.printf("Could not connect to host - retrying in %d seconds... [%d/%d]%n", retryDelaySeconds, retryAttempts - retryAttemptsLeft, retryAttempts);

                try {
                    Thread.sleep(retryDelaySeconds * 1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                result = getResponseBody(baseUrl);
            } else {
                throw new ReconnectionAttemptsExceededException("Maximum amount of attempts to connect to host exceeded.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            retryAttemptsLeft = retryAttempts;
        }
        return result;
    }

    private String getJson(String apiVersion, String type, String method) {

        return getResponseBody(generateUrl(apiVersion, type, method));
    }

    private String getJson(String apiVersion, String type, String method, HashMap<String, String> parameters) {

        return getResponseBody(generateUrl(apiVersion, type, method, parameters));
    }

    private String generateUrl(String apiVersion, String type, String method, HashMap<String, String> parameters) {
        String url = INITIAL_URL;
        url += "v" + apiVersion + "/";
        url += type + "/";
        url += method;
        url += generateUrlParameters(parameters);

        return url;
    }

    private String generateUrl(String apiVersion, String type, String method) {

        return generateUrl(apiVersion, type, method, new HashMap<String, String>());
    }

    private String generateUrlParameters(HashMap<String, String> parameters) { // Returns a String with the key-value pairs formatted for URL

        String urlAttachment = "?";

        Object[] keys = parameters.keySet().toArray();

        for(Object key : keys)

            urlAttachment += key.toString() + "=" + parameters.get(key) + "&";

        return urlAttachment;
    }

    public static List<HashMap<String, String>> getMapsFromResponse(String response) {

        final List<HashMap<String, String>> maps = new ArrayList<>();

        if(!response.contains("[")) {

            maps.add(jsonMapToHashMap(response.substring(response.lastIndexOf("\"result\":") + "\"result\":".length(), response.indexOf("}") + 1))); // Sorry.

        } else {

            final String resultArray = response.substring(response.indexOf("\"result\":") + "\"result\":".length() + 1, response.lastIndexOf("]"));

            final String[] jsonMaps = resultArray.split(",(?=\\{)");

            for(String map : jsonMaps)

                maps.add(jsonMapToHashMap(map));
        }

        return maps;
    }

    // Handles the exception of the generateHashMapFromStringList() method gracefully as to not have an excess of try-catch statements
    private HashMap<String, String> returnCorrectMap(String...parameters) {
        HashMap<String, String> map = null;
        try {
            map = generateHashMapFromStringList(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // Method to easily create a HashMap from a list of Strings
    private HashMap<String, String> generateHashMapFromStringList(String...strings) throws Exception {

        if(strings.length % 2 != 0) throw InvalidStringListException;

        HashMap<String, String> map = new HashMap<String, String>();

        for(int i = 0; i < strings.length; i += 2) // Each key will be i, with the following becoming its value

            map.put(strings[i], strings[i + 1]);

        return map;
    }

    private static HashMap<String, String> jsonMapToHashMap(String jsonMap) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.fromJson(jsonMap, new TypeToken<HashMap<String, String>>(){}.getType());
    }

    public String getBalances() { // Returns all balances in your account
        return getJson(API_VERSION, ACCOUNT, "getbalances");
    }

    public String getMarkets() { // Returns all markets with their metadata
        return getJson(API_VERSION, PUBLIC, "getmarkets");
    }

    public String getMarketSummaries() { // Returns a 24-hour summary of all markets
        return getJson(API_VERSION, PUBLIC, "getmarketsummaries");
    }

    public String getMarketSummary(String market) { // Returns a 24-hour summar for a specific market
        String result = getJson(API_VERSION, PUBLIC, "getmarketsummary", returnCorrectMap("market", market));
        result = result.substring(40, result.length()-3);
        result = result.replaceAll("\"", "");
        String[] str = result.split(",");
        result = "";
        for (String s : str) {
            result += s + "\n";
        }

        if (str.length < 3) result = "Wrong Market Name";
        return result;
    }

}
