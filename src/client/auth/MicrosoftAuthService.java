package client.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Performs official Microsoft account authentication for Minecraft Java.
 *
 * This flow avoids custom Azure app registration by using the public desktop
 * OAuth client that is widely used by Java launchers.
 */
public final class MicrosoftAuthService
{
    private static final JsonParser JSON = new JsonParser();
    private static final String USER_AGENT = "DWGX-Minecraft-Auth/1.0";

    // Legacy browser-code flow (kept as fallback)
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String OAUTH_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    // Device-code flow (no redirect URL copy needed)
    // Uses live.com device flow compatible with Xbox/Minecraft scope.
    private static final String DEVICE_CLIENT_ID = "00000000441cc96b";
    private static final String DEVICE_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private static final String AUTHORIZE_ENDPOINT = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_ENDPOINT = "https://login.live.com/oauth20_token.srf";
    private static final String DEVICE_CODE_ENDPOINT = "https://login.live.com/oauth20_connect.srf";
    private static final String DEVICE_TOKEN_ENDPOINT = "https://login.live.com/oauth20_token.srf";
    private static final String XBL_AUTH_ENDPOINT = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_ENDPOINT = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_ENDPOINT = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    public String buildAuthorizationUrl()
    {
        try
        {
            StringBuilder builder = new StringBuilder(AUTHORIZE_ENDPOINT);
            builder.append("?client_id=").append(urlEncode(CLIENT_ID));
            builder.append("&response_type=code");
            builder.append("&redirect_uri=").append(urlEncode(REDIRECT_URI));
            builder.append("&scope=").append(urlEncode(OAUTH_SCOPE));
            builder.append("&prompt=select_account");
            return builder.toString();
        }
        catch (Exception ignored)
        {
            return AUTHORIZE_ENDPOINT;
        }
    }

    public MicrosoftAuthResult loginWithAuthorizationInput(String rawInput) throws AuthException
    {
        String code = this.extractAuthorizationCode(rawInput);
        OAuthTokens tokens = this.exchangeAuthorizationCode(code);
        return this.finishMicrosoftLogin(tokens);
    }

    public MicrosoftAuthResult loginWithRefreshToken(String refreshToken) throws AuthException
    {
        OAuthTokens tokens = this.exchangeRefreshToken(refreshToken);
        return this.finishMicrosoftLogin(tokens);
    }

    public DeviceAuthStart startDeviceLogin() throws AuthException
    {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", DEVICE_CLIENT_ID);
        form.put("scope", DEVICE_SCOPE);
        form.put("response_type", "device_code");
        HttpResponse response = this.postForm(DEVICE_CODE_ENDPOINT, form);
        JsonObject json = this.parseJsonResponse(response, "Microsoft device login start");
        String deviceCode = getString(json, "device_code");
        String userCode = getString(json, "user_code");
        String verificationUri = firstNonEmpty(getString(json, "verification_uri_complete"), getString(json, "verification_uri"));
        int expiresIn = safeInt(json.get("expires_in"), 900);
        int interval = safeInt(json.get("interval"), 5);
        String message = getString(json, "message");

        if (isEmpty(deviceCode) || isEmpty(userCode) || isEmpty(verificationUri))
        {
            throw new AuthException("Microsoft device login start failed: missing device code fields.");
        }

        return new DeviceAuthStart(deviceCode, userCode, verificationUri, expiresIn, interval, message);
    }

    public MicrosoftAuthResult awaitDeviceLogin(DeviceAuthStart start, CancelCheck cancelCheck, DeviceProgressListener listener) throws AuthException
    {
        if (start == null)
        {
            throw new AuthException("Device login not initialized.");
        }

        long deadline = System.currentTimeMillis() + Math.max(30, start.expiresInSeconds) * 1000L;
        int pollInterval = Math.max(1, start.intervalSeconds);

        while (System.currentTimeMillis() < deadline)
        {
            if (cancelCheck != null && cancelCheck.isCancelled())
            {
                throw new AuthException("Device login cancelled.");
            }

            sleepSeconds(pollInterval);

            if (cancelCheck != null && cancelCheck.isCancelled())
            {
                throw new AuthException("Device login cancelled.");
            }

            Map<String, String> form = new LinkedHashMap<String, String>();
            form.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            form.put("client_id", DEVICE_CLIENT_ID);
            form.put("device_code", start.deviceCode);
            HttpResponse response = this.postForm(DEVICE_TOKEN_ENDPOINT, form);
            JsonObject json = parseJsonObject(response.body);

            if (response.statusCode >= 200 && response.statusCode < 300)
            {
                JsonObject payload = this.parseJsonResponse(response, "Microsoft device login poll");
                String accessToken = getString(payload, "access_token");
                String refreshToken = getString(payload, "refresh_token");

                if (isEmpty(accessToken))
                {
                    throw new AuthException("Microsoft device login poll failed: missing access token.");
                }

                if (isEmpty(refreshToken))
                {
                    throw new AuthException("Microsoft device login poll failed: missing refresh token.");
                }

                return this.finishMicrosoftLogin(new OAuthTokens(accessToken, refreshToken));
            }

            String error = json == null ? "" : getString(json, "error");

            if ("authorization_pending".equalsIgnoreCase(error))
            {
                if (listener != null)
                {
                    listener.onProgress("Waiting for browser confirmation...");
                }

                continue;
            }

            if ("slow_down".equalsIgnoreCase(error))
            {
                pollInterval = Math.min(pollInterval + 5, 20);

                if (listener != null)
                {
                    listener.onProgress("Microsoft asked to slow down polling.");
                }

                continue;
            }

            if ("authorization_declined".equalsIgnoreCase(error))
            {
                throw new AuthException("Device login was declined.");
            }

            if ("expired_token".equalsIgnoreCase(error))
            {
                throw new AuthException("Device login expired. Please start again.");
            }

            String detail = json == null ? ("HTTP " + response.statusCode) : this.buildErrorMessage(json, response.statusCode);
            throw new AuthException("Device login failed: " + detail);
        }

        throw new AuthException("Device login timed out. Please start again.");
    }

    private MicrosoftAuthResult finishMicrosoftLogin(OAuthTokens oauth) throws AuthException
    {
        XboxToken xbl = this.authenticateXboxLive(oauth.accessToken);
        XboxToken xsts = this.authorizeXsts(xbl.token);
        String mcToken = this.exchangeMinecraftToken(xsts.uhs, xsts.token);
        Profile profile = this.fetchMinecraftProfile(mcToken);
        return new MicrosoftAuthResult(profile.name, profile.uuid, mcToken, oauth.refreshToken);
    }

    private OAuthTokens exchangeAuthorizationCode(String code) throws AuthException
    {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", CLIENT_ID);
        form.put("code", code);
        form.put("grant_type", "authorization_code");
        form.put("redirect_uri", REDIRECT_URI);
        form.put("scope", OAUTH_SCOPE);
        return this.requestOAuthTokens(form, "Microsoft authorization");
    }

    private OAuthTokens exchangeRefreshToken(String refreshToken) throws AuthException
    {
        if (refreshToken == null || refreshToken.trim().isEmpty())
        {
            throw new AuthException("No refresh token found. Please login with browser once.");
        }

        AuthException firstError = null;

        try
        {
            return this.exchangeRefreshTokenWithLegacyClient(refreshToken);
        }
        catch (AuthException ex)
        {
            firstError = ex;
        }

        try
        {
            return this.exchangeRefreshTokenWithDeviceClient(refreshToken);
        }
        catch (AuthException ex)
        {
            if (firstError != null)
            {
                throw firstError;
            }

            throw ex;
        }
    }

    private OAuthTokens exchangeRefreshTokenWithLegacyClient(String refreshToken) throws AuthException
    {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", CLIENT_ID);
        form.put("refresh_token", refreshToken.trim());
        form.put("grant_type", "refresh_token");
        form.put("redirect_uri", REDIRECT_URI);
        form.put("scope", OAUTH_SCOPE);
        return this.requestOAuthTokens(form, "Microsoft refresh (legacy)");
    }

    private OAuthTokens exchangeRefreshTokenWithDeviceClient(String refreshToken) throws AuthException
    {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put("client_id", DEVICE_CLIENT_ID);
        form.put("refresh_token", refreshToken.trim());
        form.put("grant_type", "refresh_token");
        form.put("scope", DEVICE_SCOPE);
        return this.requestOAuthTokens(form, "Microsoft refresh (device)");
    }

    private OAuthTokens requestOAuthTokens(Map<String, String> form, String step) throws AuthException
    {
        HttpResponse response = this.postForm(TOKEN_ENDPOINT, form);
        JsonObject json = this.parseJsonResponse(response, step);
        String accessToken = getString(json, "access_token");
        String refreshToken = getString(json, "refresh_token");

        if (isEmpty(accessToken))
        {
            throw new AuthException(step + " failed: missing access token.");
        }

        if (isEmpty(refreshToken))
        {
            throw new AuthException(step + " failed: missing refresh token.");
        }

        return new OAuthTokens(accessToken, refreshToken);
    }

    private XboxToken authenticateXboxLive(String msaAccessToken) throws AuthException
    {
        AuthException firstError = null;

        try
        {
            return this.authenticateXboxLiveWithTicket("d=" + msaAccessToken);
        }
        catch (AuthException ex)
        {
            firstError = ex;
        }

        try
        {
            return this.authenticateXboxLiveWithTicket(msaAccessToken);
        }
        catch (AuthException ex)
        {
            if (firstError != null)
            {
                throw firstError;
            }

            throw ex;
        }
    }

    private XboxToken authenticateXboxLiveWithTicket(String rpsTicket) throws AuthException
    {
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", rpsTicket);
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");

        HttpResponse response = this.postJson(XBL_AUTH_ENDPOINT, body);
        JsonObject json = this.parseJsonResponse(response, "Xbox Live authenticate");
        String token = getString(json, "Token");
        String uhs = this.extractUserHash(json);

        if (isEmpty(token) || isEmpty(uhs))
        {
            throw new AuthException("Xbox Live authenticate failed: missing token or user hash.");
        }

        return new XboxToken(token, uhs);
    }

    private XboxToken authorizeXsts(String xblToken) throws AuthException
    {
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(xblToken));
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        properties.add("UserTokens", userTokens);
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        body.addProperty("TokenType", "JWT");

        HttpResponse response = this.postJson(XSTS_AUTH_ENDPOINT, body);
        JsonObject json = this.parseJsonResponse(response, "XSTS authorize");
        String token = getString(json, "Token");
        String uhs = this.extractUserHash(json);

        if (isEmpty(token) || isEmpty(uhs))
        {
            throw new AuthException("XSTS authorize failed: missing token or user hash.");
        }

        return new XboxToken(token, uhs);
    }

    private String exchangeMinecraftToken(String uhs, String xstsToken) throws AuthException
    {
        JsonObject body = new JsonObject();
        body.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        body.addProperty("ensureLegacyEnabled", true);

        HttpResponse response = this.postJson(MC_LOGIN_ENDPOINT, body);
        JsonObject json = this.parseJsonResponse(response, "Minecraft services login");
        String mcToken = getString(json, "access_token");

        if (isEmpty(mcToken))
        {
            throw new AuthException("Minecraft services login failed: missing access token.");
        }

        return mcToken;
    }

    private Profile fetchMinecraftProfile(String mcToken) throws AuthException
    {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + mcToken);
        HttpResponse response = this.getJson(MC_PROFILE_ENDPOINT, headers);
        JsonObject json = this.parseJsonResponse(response, "Minecraft profile");
        String uuid = normalizeUuid(getString(json, "id"));
        String name = getString(json, "name");

        if (isEmpty(uuid) || isEmpty(name))
        {
            throw new AuthException("Minecraft profile fetch failed: no Java profile on this account.");
        }

        return new Profile(name, uuid);
    }

    private String extractAuthorizationCode(String rawInput) throws AuthException
    {
        if (rawInput == null || rawInput.trim().isEmpty())
        {
            throw new AuthException("Please paste redirect URL or authorization code.");
        }

        String input = rawInput.trim();
        String fromUri = this.extractCodeFromUri(input);

        if (!isEmpty(fromUri))
        {
            return fromUri;
        }

        int index = input.indexOf("code=");

        if (index >= 0)
        {
            String value = input.substring(index + 5);
            int amp = value.indexOf('&');

            if (amp >= 0)
            {
                value = value.substring(0, amp);
            }

            String decoded = urlDecode(value);

            if (!isEmpty(decoded))
            {
                return decoded;
            }
        }

        return urlDecode(input);
    }

    private String extractCodeFromUri(String input)
    {
        try
        {
            URI uri = URI.create(input);
            String query = uri.getRawQuery();

            if (query == null || query.isEmpty())
            {
                return "";
            }

            String[] parts = query.split("&");

            for (int i = 0; i < parts.length; i++)
            {
                String part = parts[i];
                int eq = part.indexOf('=');

                if (eq <= 0)
                {
                    continue;
                }

                String key = part.substring(0, eq);

                if (!"code".equalsIgnoreCase(key))
                {
                    continue;
                }

                String rawCode = part.substring(eq + 1);
                return urlDecode(rawCode);
            }
        }
        catch (Exception ignored)
        {
            return "";
        }

        return "";
    }

    private String extractUserHash(JsonObject payload)
    {
        if (payload == null || !payload.has("DisplayClaims"))
        {
            return "";
        }

        JsonObject displayClaims = asObject(payload.get("DisplayClaims"));

        if (displayClaims == null || !displayClaims.has("xui"))
        {
            return "";
        }

        JsonArray xui = asArray(displayClaims.get("xui"));

        if (xui == null || xui.size() == 0)
        {
            return "";
        }

        JsonObject first = asObject(xui.get(0));
        return first == null ? "" : getString(first, "uhs");
    }

    private JsonObject parseJsonResponse(HttpResponse response, String step) throws AuthException
    {
        JsonObject json = parseJsonObject(response.body);

        if (json == null)
        {
            if (response.statusCode >= 200 && response.statusCode < 300)
            {
                throw new AuthException(step + " failed: invalid JSON response.");
            }

            throw new AuthException(step + " failed: HTTP " + response.statusCode + ".");
        }

        if (response.statusCode < 200 || response.statusCode >= 300)
        {
            throw new AuthException(step + " failed: " + this.buildErrorMessage(json, response.statusCode));
        }

        if (json.has("error"))
        {
            throw new AuthException(step + " failed: " + this.buildErrorMessage(json, response.statusCode));
        }

        return json;
    }

    private String buildErrorMessage(JsonObject json, int statusCode)
    {
        String message = firstNonEmpty(
            getString(json, "error_description"),
            getString(json, "errorMessage"),
            getString(json, "Message"),
            getString(json, "message"),
            getString(json, "error")
        );

        if (!isEmpty(message))
        {
            return message;
        }

        if (json.has("XErr"))
        {
            long xerr = safeLong(json.get("XErr"));
            String mapped = mapXErr(xerr);

            if (!isEmpty(mapped))
            {
                return mapped;
            }

            return "XSTS error code: " + xerr;
        }

        return "HTTP " + statusCode;
    }

    private HttpResponse postForm(String endpoint, Map<String, String> form) throws AuthException
    {
        return this.request("POST", endpoint, "application/x-www-form-urlencoded", buildForm(form), null);
    }

    private HttpResponse postJson(String endpoint, JsonObject jsonBody) throws AuthException
    {
        return this.request("POST", endpoint, "application/json", jsonBody.toString(), null);
    }

    private HttpResponse getJson(String endpoint, Map<String, String> headers) throws AuthException
    {
        return this.request("GET", endpoint, null, null, headers);
    }

    private HttpResponse request(String method, String endpoint, String contentType, String body, Map<String, String> headers) throws AuthException
    {
        HttpURLConnection connection = null;

        try
        {
            connection = (HttpURLConnection)(new URL(endpoint)).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod(method);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            if (!isEmpty(contentType))
            {
                connection.setRequestProperty("Content-Type", contentType + "; charset=UTF-8");
            }

            if (headers != null)
            {
                for (Map.Entry<String, String> entry : headers.entrySet())
                {
                    if (!isEmpty(entry.getKey()) && !isEmpty(entry.getValue()))
                    {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (body != null)
            {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

                OutputStream output = connection.getOutputStream();

                try
                {
                    output.write(bytes);
                    output.flush();
                }
                finally
                {
                    output.close();
                }
            }

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 400 ? connection.getInputStream() : connection.getErrorStream();
            String payload = readAll(stream);
            return new HttpResponse(statusCode, payload);
        }
        catch (IOException ex)
        {
            throw new AuthException("Network error: " + ex.getMessage(), ex);
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private static String buildForm(Map<String, String> form) throws AuthException
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : form.entrySet())
        {
            if (!first)
            {
                builder.append('&');
            }

            first = false;
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }

        return builder.toString();
    }

    private static String urlEncode(String value) throws AuthException
    {
        try
        {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        }
        catch (Exception ex)
        {
            throw new AuthException("URL encode failed: " + ex.getMessage(), ex);
        }
    }

    private static String urlDecode(String value)
    {
        try
        {
            return URLDecoder.decode(value == null ? "" : value, "UTF-8");
        }
        catch (Exception ignored)
        {
            return value == null ? "" : value;
        }
    }

    private static String readAll(InputStream inputStream) throws IOException
    {
        if (inputStream == null)
        {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        try
        {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[2048];
            int read;

            while ((read = reader.read(buffer)) >= 0)
            {
                builder.append(buffer, 0, read);
            }

            return builder.toString();
        }
        finally
        {
            reader.close();
        }
    }

    private static JsonObject parseJsonObject(String payload)
    {
        if (payload == null || payload.trim().isEmpty())
        {
            return null;
        }

        try
        {
            JsonElement parsed = JSON.parse(payload);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private static JsonObject asObject(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray asArray(JsonElement element)
    {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key)
    {
        if (object == null || key == null || !object.has(key))
        {
            return "";
        }

        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static long safeLong(JsonElement element)
    {
        if (element == null || element.isJsonNull())
        {
            return 0L;
        }

        try
        {
            return element.getAsLong();
        }
        catch (RuntimeException ignored)
        {
            return 0L;
        }
    }

    private static int safeInt(JsonElement element, int fallback)
    {
        if (element == null || element.isJsonNull())
        {
            return fallback;
        }

        try
        {
            return element.getAsInt();
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static void sleepSeconds(int seconds) throws AuthException
    {
        try
        {
            Thread.sleep(Math.max(1, seconds) * 1000L);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AuthException("Device login interrupted.");
        }
    }

    private static String firstNonEmpty(String... values)
    {
        if (values == null)
        {
            return "";
        }

        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];

            if (!isEmpty(value))
            {
                return value;
            }
        }

        return "";
    }

    private static String normalizeUuid(String uuid)
    {
        if (uuid == null)
        {
            return "";
        }

        return uuid.replace("-", "").trim();
    }

    private static String mapXErr(long xerr)
    {
        if (xerr == 2148916233L)
        {
            return "Xbox account not found for this Microsoft account.";
        }

        if (xerr == 2148916235L)
        {
            return "Xbox account is restricted by region.";
        }

        if (xerr == 2148916236L || xerr == 2148916238L)
        {
            return "Adult verification required in Xbox account settings.";
        }

        return "";
    }

    private static boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    public interface CancelCheck
    {
        boolean isCancelled();
    }

    public interface DeviceProgressListener
    {
        void onProgress(String message);
    }

    public static final class DeviceAuthStart
    {
        private final String deviceCode;
        private final String userCode;
        private final String verificationUri;
        private final int expiresInSeconds;
        private final int intervalSeconds;
        private final String message;

        private DeviceAuthStart(String deviceCode, String userCode, String verificationUri, int expiresInSeconds, int intervalSeconds, String message)
        {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.expiresInSeconds = expiresInSeconds;
            this.intervalSeconds = intervalSeconds;
            this.message = message == null ? "" : message;
        }

        public String getDeviceCode()
        {
            return this.deviceCode;
        }

        public String getUserCode()
        {
            return this.userCode;
        }

        public String getVerificationUri()
        {
            return this.verificationUri;
        }

        public int getExpiresInSeconds()
        {
            return this.expiresInSeconds;
        }

        public int getIntervalSeconds()
        {
            return this.intervalSeconds;
        }

        public String getMessage()
        {
            return this.message;
        }
    }

    private static final class OAuthTokens
    {
        private final String accessToken;
        private final String refreshToken;

        private OAuthTokens(String accessToken, String refreshToken)
        {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    private static final class XboxToken
    {
        private final String token;
        private final String uhs;

        private XboxToken(String token, String uhs)
        {
            this.token = token;
            this.uhs = uhs;
        }
    }

    private static final class Profile
    {
        private final String name;
        private final String uuid;

        private Profile(String name, String uuid)
        {
            this.name = name;
            this.uuid = uuid;
        }
    }

    private static final class HttpResponse
    {
        private final int statusCode;
        private final String body;

        private HttpResponse(int statusCode, String body)
        {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }
    }

    public static class AuthException extends Exception
    {
        public AuthException(String message)
        {
            super(message);
        }

        public AuthException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
