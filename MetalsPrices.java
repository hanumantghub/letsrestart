import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;

public class MetalsPrices {
    static HttpClient httpClient;
    
    static {
        try {
            httpClient = createHttpClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            // Get current date and time
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = now.format(formatter);
            
            // Fetch gold and silver rates in USD per oz
            double goldRateUSDPerOz = getMetalRate("gold");
            double silverRateUSDPerOz = getMetalRate("silver");
            
            // Convert from oz to gram (1 oz = 28.3495 grams)
            double goldRateUSD = goldRateUSDPerOz / 28.3495;
            double silverRateUSD = silverRateUSDPerOz / 28.3495;
            
            // Get USD to INR exchange rate
            double usdToInr = getExchangeRate();
            
            // Convert to INR
            double goldRateINR = goldRateUSD * usdToInr;
            double silverRateINR = silverRateUSD * usdToInr;
            
            // Display results
            System.out.println("========================================");
            System.out.println("METALS PRICES - " + timestamp);
            System.out.println("========================================");
            System.out.println("Gold Rate (USD/gram):   $" + String.format("%.2f", goldRateUSD));
            System.out.println("Gold Rate (INR/gram):   Rs " + String.format("%.2f", goldRateINR));
            System.out.println("");
            System.out.println("Silver Rate (USD/gram): $" + String.format("%.2f", silverRateUSD));
            System.out.println("Silver Rate (INR/gram): Rs " + String.format("%.2f", silverRateINR));
            System.out.println("");
            System.out.println("Exchange Rate (1 USD = Rs " + String.format("%.2f", usdToInr) + ")");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static HttpClient createHttpClient() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        // Disable certificate verification
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
    }
    
    public static double getExchangeRate() throws Exception {
        try {
            // Try to fetch live exchange rate from exchangerate-api.com
            String urlString = "https://api.exchangerate-api.com/v4/latest/USD";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("User-Agent", "curl/7.68.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject jsonObject = new JSONObject(response.body());
                JSONObject rates = jsonObject.getJSONObject("rates");
                double inrRate = rates.getDouble("INR");
                System.out.println("Note: Using live exchange rate");
                return inrRate;
            }
        } catch (Exception e) {
            System.out.println("Note: Unable to fetch live exchange rate, using default rate");
        }
        
        // Fallback: Use approximate exchange rate (1 USD = 83 INR)
        return 83.0;
    }
    
    public static double getMetalRate(String metal) throws Exception {
        // Using Commodities API (free tier alternative)
        String urlString = "https://api.metals.live/v1/spot/" + metal.toLowerCase();
        
        // Disable SNI to work around server compatibility issues
        System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("User-Agent", "curl/7.68.0")
                .GET()
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new Exception("API request failed with status code: " + response.statusCode());
            }
            
            // Parse JSON response
            JSONObject jsonObject = new JSONObject(response.body());
            double rate = jsonObject.getDouble("price");
            
            return rate;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            // Fallback: Try with a simpler representation
            System.err.println("Warning: Could not reach metals.live. Using example rates.");
            return metal.equalsIgnoreCase("gold") ? 2095.50 : 29.45;
        }
    }
}
