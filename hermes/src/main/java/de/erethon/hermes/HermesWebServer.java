package de.erethon.hermes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.web.WebAccountService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class HermesWebServer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Hermes plugin;
    private final HermesContentService contentService;
    private final HermesModelFileService modelFileService;
    private final QxlComponentCatalogService catalogService;
    private final QxlAssetCatalogService assetCatalogService;
    private final Path staticRoot;
    private HttpServer server;

    public HermesWebServer(Hermes plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.contentService = new HermesContentService(plugin);
        this.modelFileService = new HermesModelFileService(plugin, Math.max(1024L, config.getLong("maxModelUploadBytes", 26_214_400L)));
        this.catalogService = new QxlComponentCatalogService();
        this.assetCatalogService = new QxlAssetCatalogService();
        String configuredStaticPath = config.getString("staticPath", "");
        String staticPath = configuredStaticPath == null || configuredStaticPath.isBlank()
                ? new java.io.File(plugin.getDataFolder(), "web").getAbsolutePath()
                : configuredStaticPath;
        this.staticRoot = Path.of(staticPath);
    }

    public void start(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 64);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        plugin.getLogger().info("Hermes authoring panel listening on http://" + host + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            server = null;
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
                return;
            }
            handleStatic(exchange, path);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            plugin.getLogger().warning("Hermes web request failed: " + e.getMessage());
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Internal server error"));
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/api/auth/setup/start")) {
            requireMethod(exchange, "POST", () -> {
                JsonObject body = readJson(exchange);
                WebAccountService.SetupStartResult result = accountService().startSetup(requiredString(body, "playerName"), clientIp(exchange)).join();
                sendJson(exchange, result.success() ? 200 : 404, result);
            });
            return;
        }
        if (path.equals("/api/auth/setup/finish")) {
            requireMethod(exchange, "POST", () -> {
                JsonObject body = readJson(exchange);
                WebAccountService.LoginResult result = accountService().finishSetup(requiredString(body, "token"), requiredString(body, "password").toCharArray()).join();
                if (result.success()) {
                    setSessionCookie(exchange, result.sessionToken());
                }
                sendJson(exchange, result.success() ? 200 : 401, result);
            });
            return;
        }
        if (path.equals("/api/auth/login")) {
            requireMethod(exchange, "POST", () -> {
                JsonObject body = readJson(exchange);
                WebAccountService.LoginResult result = accountService().login(requiredString(body, "playerName"), requiredString(body, "password").toCharArray()).join();
                if (result.success()) {
                    setSessionCookie(exchange, result.sessionToken());
                }
                sendJson(exchange, result.success() ? 200 : 401, result);
            });
            return;
        }
        if (path.equals("/api/auth/logout")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> {
                accountService().logout(user.playerId()).join();
                clearSessionCookie(exchange);
                sendJson(exchange, 200, Map.of("success", true));
            });
            return;
        }
        if (path.equals("/api/me")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, userDto(user)));
            return;
        }
        if (path.equals("/api/server/status")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, serverStatus()));
            return;
        }
        if (path.equals("/api/catalog")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, catalogService.catalog()));
            return;
        }
        if (path.equals("/api/assets")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.catalog()));
            return;
        }
        if (path.equals("/api/assets/items")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.items()));
            return;
        }
        if (path.equals("/api/assets/mobs")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.mobs()));
            return;
        }
        if (path.equals("/api/assets/quests")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.quests()));
            return;
        }
        if (path.equals("/api/assets/events")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.events()));
            return;
        }
        if (path.equals("/api/assets/dialogues")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.dialogues()));
            return;
        }
        if (path.equals("/api/assets/hecate/classes")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.hecateClasses()));
            return;
        }
        if (path.equals("/api/assets/hecate/traitlines")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.hecateTraitlines()));
            return;
        }
        if (path.equals("/api/assets/spellbook/spells")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.spellbookSpells()));
            return;
        }
        if (path.equals("/api/assets/spellbook/traits")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.spellbookTraits()));
            return;
        }
        if (path.equals("/api/assets/spellbook/effects")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.spellbookEffects()));
            return;
        }
        if (path.equals("/api/assets/worlds")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.worlds()));
            return;
        }
        if (path.equals("/api/assets/materials")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.materials()));
            return;
        }
        if (path.equals("/api/assets/attributes")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, assetCatalogService.attributes()));
            return;
        }
        if (path.equals("/api/content/types")) {
            requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, contentService.listTypes()));
            return;
        }
        if (path.equals("/api/content/batch/publish")) {
            requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                JsonArray rawItems = body.has("items") && body.get("items").isJsonArray() ? body.getAsJsonArray("items") : new JsonArray();
                List<Map<String, String>> items = rawItems.asList().stream()
                        .filter(element -> element.isJsonObject())
                        .map(element -> {
                            JsonObject item = element.getAsJsonObject();
                            return Map.of(
                                    "type", stringValue(item, "type"),
                                    "path", stringValue(item, "path"),
                                    "yaml", stringValue(item, "yaml")
                            );
                        })
                        .toList();
                sendJson(exchange, 200, contentService.publishBatch(items).join());
            }));
            return;
        }
        if (path.startsWith("/api/content/")) {
            handleContentApi(exchange, path.substring("/api/content/".length()));
            return;
        }
        if (path.startsWith("/api/files/")) {
            handleFileApi(exchange, path.substring("/api/files/".length()));
            return;
        }
        if (path.startsWith("/api/git")) {
            handleGitApi(exchange, path.substring("/api/git".length()));
            return;
        }
        if (path.equals("/api/users")) {
            requireUser(exchange, HermesWebRole.ADMIN, user -> sendJson(exchange, 200, plugin.getRoleService().listWebUsers().join().stream().map(this::userDto).toList()));
            return;
        }
        if (path.equals("/api/users/role")) {
            requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.ADMIN, user -> {
                JsonObject body = readJson(exchange);
                UUID playerId = UUID.fromString(requiredString(body, "playerId"));
                HermesWebRole role = HermesWebRole.fromDatabase(requiredString(body, "role"));
                sendJson(exchange, 200, userDto(plugin.getRoleService().setWebRole(playerId, role).join()));
            }));
            return;
        }
        sendJson(exchange, 404, Map.of("error", "Not found"));
    }

    private void handleGitApi(HttpExchange exchange, String relative) throws IOException {
        String action = relative == null || relative.isBlank() ? "/status" : relative;
        switch (action) {
            case "/status" -> requireMethod(exchange, "GET", () -> requireUser(exchange, HermesWebRole.VIEWER, user ->
                    sendJson(exchange, 200, plugin.getGitExportService().status())));
            case "/refresh" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.VIEWER, user ->
                    sendJson(exchange, 200, plugin.getGitExportService().refresh().join())));
            case "/push" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                String commitMessage = body.has("commitMessage") && !body.get("commitMessage").isJsonNull()
                        ? body.get("commitMessage").getAsString()
                        : "";
                sendJson(exchange, 200, plugin.getGitExportService().push(commitMessage, user.playerName()).join());
            }));
            case "/clear" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.ADMIN, user ->
                    sendJson(exchange, 200, plugin.getGitExportService().clearQueue())));
            case "/config" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.ADMIN, user -> {
                JsonObject body = readJson(exchange);
                Map<String, String> values = new HashMap<>();
                for (String key : List.of("enabled", "repoUrl", "branch", "token", "authorName", "authorEmail", "clonePath")) {
                    if (body.has(key) && !body.get(key).isJsonNull()) {
                        values.put(key, body.get(key).getAsString());
                    }
                }
                sendJson(exchange, 200, plugin.getGitExportService().updateConfig(values));
            }));
            default -> sendJson(exchange, 404, Map.of("error", "Not found"));
        }
    }

    private Map<String, Object> serverStatus() {
        if (Bukkit.isPrimaryThread()) {
            return collectServerStatus();
        }
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                future.complete(collectServerStatus());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future.join();
    }

    private Map<String, Object> collectServerStatus() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long usedMemory = memory.getHeapMemoryUsage().getUsed();
        long maxMemory = memory.getHeapMemoryUsage().getMax();
        Map<String, Object> status = new HashMap<>();
        status.put("name", Bukkit.getServer().getName());
        status.put("version", Bukkit.getServer().getVersion());
        status.put("bukkitVersion", Bukkit.getBukkitVersion());
        status.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        status.put("maxPlayers", Bukkit.getMaxPlayers());
        status.put("worlds", Bukkit.getWorlds().stream().map(world -> Map.of(
                "name", world.getName(),
                "players", world.getPlayers().size(),
                "loadedChunks", world.getLoadedChunks().length,
                "entities", world.getEntities().size()
        )).toList());
        status.put("tps", doubleArrayInvoke(Bukkit.getServer(), "getTPS"));
        status.put("mspt", doubleInvoke(Bukkit.getServer(), "getAverageTickTime"));
        status.put("memoryUsed", usedMemory);
        status.put("memoryMax", maxMemory);
        status.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        return status;
    }

    private double[] doubleArrayInvoke(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value instanceof double[] values) {
                return values;
            }
        } catch (Throwable ignored) {
        }
        return new double[0];
    }

    private double doubleInvoke(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    private void handleContentApi(HttpExchange exchange, String relative) throws IOException {
        String[] parts = relative.split("/", 2);
        HermesContentType type = HermesContentType.byId(parts[0]);
        String action = parts.length > 1 ? parts[1] : "";
        if (action.isBlank()) {
            requireMethod(exchange, "GET", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, contentService.listItems(type))));
            return;
        }
        switch (action) {
            case "read" -> requireMethod(exchange, "GET", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, contentService.read(type, query(exchange).getOrDefault("path", "")))));
            case "draft" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, contentService.saveDraft(type, requiredString(body, "path"), stringValue(body, "yaml")));
            }));
            case "validate" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, contentService.validate(type, requiredString(body, "path"), stringValue(body, "yaml")));
            }));
            case "publish" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, contentService.publish(type, requiredString(body, "path"), stringValue(body, "yaml")).join());
            }));
            case "delete" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, contentService.delete(type, requiredString(body, "path")).join());
            }));
            case "move" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, contentService.move(type, requiredString(body, "path"), requiredString(body, "nextPath")).join());
            }));
            default -> sendJson(exchange, 404, Map.of("error", "Not found"));
        }
    }

    private void handleFileApi(HttpExchange exchange, String relative) throws IOException {
        String[] parts = relative.split("/", 2);
        String type = parts[0];
        String action = parts.length > 1 ? parts[1] : "";
        if (!type.equals("daedalusModels")) {
            sendJson(exchange, 404, Map.of("error", "Not found"));
            return;
        }
        if (action.isBlank()) {
            requireMethod(exchange, "GET", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> sendJson(exchange, 200, modelFileService.listModels())));
            return;
        }
        switch (action) {
            case "download" -> requireMethod(exchange, "GET", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> {
                String itemPath = query(exchange).getOrDefault("path", "");
                byte[] bytes = modelFileService.read(itemPath);
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Disposition", "attachment; filename=\"" + Path.of(itemPath).getFileName() + "\"");
                sendBytes(exchange, 200, "application/octet-stream", bytes);
            }));
            case "upload" -> requireMethod(exchange, "PUT", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                Map<String, String> query = query(exchange);
                String itemPath = query.getOrDefault("path", "");
                boolean overwrite = Boolean.parseBoolean(query.getOrDefault("overwrite", "false"));
                sendJson(exchange, 200, modelFileService.upload(itemPath, exchange.getRequestBody().readAllBytes(), overwrite).join());
            }));
            case "validate" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.VIEWER, user -> {
                Map<String, String> query = query(exchange);
                String itemPath = query.getOrDefault("path", "");
                byte[] bytes = exchange.getRequestBody().readAllBytes();
                if (bytes.length == 0 && !itemPath.isBlank()) {
                    bytes = modelFileService.read(itemPath);
                }
                sendJson(exchange, 200, modelFileService.validate(itemPath, bytes));
            }));
            case "move" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, modelFileService.move(requiredString(body, "path"), requiredString(body, "nextPath")).join());
            }));
            case "delete" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user -> {
                JsonObject body = readJson(exchange);
                sendJson(exchange, 200, modelFileService.delete(requiredString(body, "path")).join());
            }));
            case "reload" -> requireMethod(exchange, "POST", () -> requireUser(exchange, HermesWebRole.EDITOR, user ->
                    sendJson(exchange, 200, modelFileService.reload().join())));
            default -> sendJson(exchange, 404, Map.of("error", "Not found"));
        }
    }

    private void requireUser(HttpExchange exchange, HermesWebRole requiredRole, AuthenticatedHandler handler) throws IOException {
        Optional<UUID> playerId = accountService().authenticate(cookie(exchange, WebAccountService.SESSION_COOKIE)).join();
        if (playerId.isEmpty()) {
            playerId = authenticateProxyUser(exchange);
        }
        if (playerId.isEmpty()) {
            sendJson(exchange, 401, Map.of("error", "Not authenticated"));
            return;
        }
        HermesWebUser user = effectiveUser(plugin.getRoleService().getWebUser(playerId.get()).join());
        if (!hasRole(user.role(), requiredRole)) {
            sendJson(exchange, 403, Map.of("error", "Missing Hermes web role"));
            return;
        }
        handler.handle(user);
    }

    private WebAccountService accountService() {
        WebAccountService service = Hecate.getInstance().getWebAccountService();
        if (service == null) {
            throw new IllegalStateException("Hecate web account service is not available.");
        }
        return service;
    }

    private HermesWebUser effectiveUser(HermesWebUser stored) {
        if (stored.role().canView()) {
            return stored;
        }
        Player player = Bukkit.getPlayer(stored.playerId());
        if (player == null) {
            return stored;
        }
        if (player.hasPermission("hermes.web.admin") || player.hasPermission("qxl.web.admin") || player.hasPermission("qxl.admin")) {
            return new HermesWebUser(stored.playerId(), stored.playerName(), HermesWebRole.ADMIN);
        }
        if (player.hasPermission("hermes.web.editor") || player.hasPermission("qxl.web.editor")) {
            return new HermesWebUser(stored.playerId(), stored.playerName(), HermesWebRole.EDITOR);
        }
        if (player.hasPermission("hermes.web.viewer") || player.hasPermission("qxl.web.viewer")) {
            return new HermesWebUser(stored.playerId(), stored.playerName(), HermesWebRole.VIEWER);
        }
        return stored;
    }

    private Optional<UUID> authenticateProxyUser(HttpExchange exchange) {
        String playerId = firstHeader(exchange, "X-Hermes-Proxy-Player-Id");
        String expires = firstHeader(exchange, "X-Hermes-Proxy-Expires");
        String signature = firstHeader(exchange, "X-Hermes-Proxy-Signature");
        if (playerId == null || expires == null || signature == null) {
            return Optional.empty();
        }
        try {
            long expiresAt = Long.parseLong(expires);
            if (expiresAt < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            String expected = signProxyPayload(playerId + ":" + expires);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(playerId));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String signProxyPayload(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hecateSessionSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String hecateSessionSecret() {
        File webConfigFile = new File(Hecate.getInstance().getDataFolder(), "web.yml");
        return YamlConfiguration.loadConfiguration(webConfigFile).getString("sessionSecret", "");
    }

    private String firstHeader(HttpExchange exchange, String name) {
        List<String> values = exchange.getRequestHeaders().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private boolean hasRole(HermesWebRole actual, HermesWebRole required) {
        return switch (required) {
            case NONE -> true;
            case VIEWER -> actual.canView();
            case EDITOR -> actual.canEdit();
            case ADMIN -> actual.canAdmin();
        };
    }

    private Map<String, Object> userDto(HermesWebUser user) {
        return Map.of(
                "playerId", user.playerId().toString(),
                "playerName", user.playerName() == null ? "" : user.playerName(),
                "role", user.role().name()
        );
    }

    private void handleStatic(HttpExchange exchange, String path) throws IOException {
        String relative = path.equals("/") ? "index.html" : path.substring(1);
        Path target = staticRoot.resolve(relative).normalize();
        if (Files.isDirectory(target)) {
            target = target.resolve("index.html");
        }
        if (!target.startsWith(staticRoot.normalize()) || !Files.isRegularFile(target)) {
            Path index = staticRoot.resolve("index.html");
            if (Files.isRegularFile(index)) {
                target = index;
            } else if (sendResource(exchange, relative) || sendResource(exchange, "index.html")) {
                return;
            } else {
                sendBytes(exchange, 200, "text/html; charset=utf-8", fallbackHtml().getBytes(StandardCharsets.UTF_8));
                return;
            }
        }
        sendBytes(exchange, 200, contentType(target), Files.readAllBytes(target));
    }

    private boolean sendResource(HttpExchange exchange, String relative) throws IOException {
        String normalized = relative == null || relative.isBlank() ? "index.html" : relative;
        if (normalized.contains("..")) {
            return false;
        }
        try (var stream = getClass().getClassLoader().getResourceAsStream("web/" + normalized)) {
            if (stream == null) {
                return false;
            }
            sendBytes(exchange, 200, contentType(Path.of(normalized)), stream.readAllBytes());
            return true;
        }
    }

    private static String fallbackHtml() {
        return """
                <!doctype html>
                <html><head><meta charset="utf-8"><link rel="icon" href="https://erethon.de/favicon.ico"><title>Erethon</title></head>
                <body style="background:#111318;color:#f4f4f5;font-family:system-ui;margin:40px">
                <h1>Erethon</h1>
                <p>Hermes API server is running. The web panel is deployed separately.</p>
                </body></html>
                """;
    }

    private static void requireMethod(HttpExchange exchange, String method, ThrowingRunnable runnable) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            sendJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        runnable.run();
    }

    private static JsonObject readJson(HttpExchange exchange) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || object.get(key).getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return object.get(key).getAsString();
    }

    private static String stringValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return object.get(key).getAsString();
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String clientIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return normalizeClientIp(forwarded.split(",")[0].trim());
        }
        return normalizeClientIp(exchange.getRemoteAddress().getAddress().getHostAddress());
    }

    private static String normalizeClientIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        String normalized = ip.trim();
        if (normalized.equals("0:0:0:0:0:0:0:1") || normalized.equals("::1") || normalized.equals("localhost")) {
            return "127.0.0.1";
        }
        if (normalized.startsWith("::ffff:")) {
            return normalized.substring("::ffff:".length());
        }
        return normalized;
    }

    private static String cookie(HttpExchange exchange, String name) {
        List<String> cookies = exchange.getRequestHeaders().getOrDefault("Cookie", List.of());
        for (String header : cookies) {
            for (String cookie : header.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals(name)) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private static void setSessionCookie(HttpExchange exchange, String token) {
        exchange.getResponseHeaders().add("Set-Cookie", WebAccountService.SESSION_COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=2592000");
    }

    private static void clearSessionCookie(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Set-Cookie", WebAccountService.SESSION_COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        sendBytes(exchange, status, "application/json; charset=utf-8", GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }

    private interface ThrowingRunnable {
        void run() throws IOException;
    }

    private interface AuthenticatedHandler {
        void handle(HermesWebUser user) throws IOException;
    }
}
