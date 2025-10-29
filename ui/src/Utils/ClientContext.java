package Utils;

import emulator.api.dto.UserDTO;

public class ClientContext {
    private static HttpSessionClient httpClient;
    private static String baseUrl;
    private static UserDTO currentUser;

    public static void init(HttpSessionClient client, String baseUrl, UserDTO user) {
        ClientContext.httpClient = client;
        ClientContext.baseUrl = baseUrl;
        ClientContext.currentUser = user;
    }

    public static HttpSessionClient getHttpClient() {
        return httpClient;
    }
    public static String getBaseUrl() {
        return baseUrl;
    }
    public static UserDTO getCurrentUser() {
        return currentUser;
    }
    public static void setCurrentUser(UserDTO user) {
        ClientContext.currentUser = user;
    }
    public static boolean isInitialized() {
        return httpClient != null && baseUrl != null;
    }
    public static void setHttpClient(HttpSessionClient client) { ClientContext.httpClient = client; }
    public static void setBaseUrl(String baseUrl) { ClientContext.baseUrl = baseUrl; }
}
