package de.erethon.questsxl.common.script;

import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the state for a single top-level execution frame (one play() or check() call).
 * Stored in a ThreadLocal so nested component calls automatically inherit the context
 * without needing to pass it through every method signature.
 *
 * The context is pushed once at the outermost play()/check() entry point.
 * Nested calls detect an existing context and skip pushing, so depth-tracking
 * ensures only the outermost call creates and clears the context.
 *
 * Variable resolution order:
 *   1. Cache (already resolved this frame)
 *   2. Built-in variables derived from the Quester (player_name, player_x, …)
 *   3. VariableProvider components walking up the parent chain from the originating component
 *
 * Unresolved variables are kept as their literal %name% form and a warning is logged.
 */
public class ExecutionContext {

    private static final Pattern VAR_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");
    private static final ThreadLocal<ExecutionContext> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final Quester quester;
    private final QComponent originComponent;
    private final Map<String, QVariable> cache = new HashMap<>();

    private ExecutionContext(Quester quester, QComponent originComponent) {
        this.quester = quester;
        this.originComponent = originComponent;
        populateBuiltins();
    }

    /**
     * Called at the start of a top-level play()/check() invocation.
     * Returns true if this call created (pushed) the context — the caller must
     * then call {@link #pop()} in a finally block.
     */
    public static boolean push(Quester quester, QComponent component) {
        int depth = DEPTH.get();
        DEPTH.set(depth + 1);
        if (depth == 0) {
            CURRENT.set(new ExecutionContext(quester, component));
            return true;
        }
        return false;
    }

    /** Removes the current context. Only call if {@link #push} returned true. */
    public static void pop() {
        int depth = DEPTH.get() - 1;
        DEPTH.set(depth);
        if (depth == 0) {
            CURRENT.remove();
        }
    }

    /**
     * Opens a shared execution frame that spans multiple {@link #push}/{@link #pop} cycles.
     * Use this with try-with-resources around a condition-checking loop so every sibling
     * condition runs inside the same {@link ExecutionContext} and can see variables
     * published by earlier siblings.
     *
     * <pre>{@code
     * try (var frame = ExecutionContext.frame(quester, owner)) {
     *     for (QCondition c : conditions) {
     *         if (!c.check(quester)) return false;
     *     }
     * }
     * }</pre>
     *
     * The frame is a no-op if a context is already active (nested call).
     */
    public static Frame frame(Quester quester, QComponent owner) {
        boolean pushed = push(quester, owner);
        return new Frame(pushed);
    }

    /** AutoCloseable handle for a shared execution frame. */
    public static final class Frame implements AutoCloseable {
        private Frame(boolean owner) {}

        @Override
        public void close() {
            pop(); // Always matches the push() that frame() always performs
        }
    }

    /**
     * Returns the current execution context, or null if called outside an
     * execution frame (e.g., during load()).
     */
    public static ExecutionContext current() {
        return CURRENT.get();
    }

    /**
     * Merges a provider's variables into the cache.
     * Called from QBaseAction/QBaseCondition after executing the component's logic
     * so child actions can see the freshly-published values.
     */
    public static void publishVariables(Map<String, QVariable> vars) {
        ExecutionContext ctx = CURRENT.get();
        if (ctx != null) {
            ctx.cache.putAll(vars);
        }
    }

    /**
     * Returns a shallow copy of the current variable cache so callers can
     * bridge variables into nested contexts (e.g., per-player execution
     * when an event action fans out messages).
     */
    public static Map<String, QVariable> snapshotVariables() {
        ExecutionContext ctx = CURRENT.get();
        if (ctx == null) {
            return Map.of();
        }
        return new HashMap<>(ctx.cache);
    }

    /**
     * Replaces all %varname% tokens in the raw string.
     * Tokens that cannot be resolved remain as their literal %name% form.
     */
    public String resolveString(String raw) {
        if (raw == null || !raw.contains("%")) {
            return raw;
        }
        Matcher matcher = VAR_PATTERN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            QVariable var = resolve(name);
            if (var != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(var.asString()));
            } else {
                // Warn and leave the token as-is
                QuestsXL.get().getErrors().add(new FriendlyError(
                        "VariableResolver",
                        "Unresolved variable: %" + name + "%",
                        "No VariableProvider in the component tree exposes this variable.",
                        "Component: " + (originComponent != null ? originComponent.getClass().getSimpleName() : "unknown")
                ));
                matcher.appendReplacement(sb, Matcher.quoteReplacement("%" + name + "%"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Resolves and evaluates a numeric expression from the raw string.
     * Variable tokens are substituted first, then the result is evaluated as a
     * math expression supporting {@code + - * / % ( )} and unary minus.
     * Examples: {@code "5"}, {@code "%group_size% * 2"}, {@code "(%score% + 1) / 3"}
     * Returns 0.0 on failure.
     */
    public double resolveDouble(String raw) {
        if (raw == null) return 0.0;
        String resolved = resolveString(raw).trim();
        try {
            return MathEval.eval(resolved);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public long resolveLong(String raw) {
        return (long) resolveDouble(raw);
    }

    public int resolveInt(String raw) {
        return (int) resolveDouble(raw);
    }

    public boolean resolveBoolean(String raw) {
        if (raw == null) return false;
        return Boolean.parseBoolean(resolveString(raw));
    }


     private static final class MathEval {

        private final String expr;
        private int pos;

        private MathEval(String expr) {
            this.expr = expr;
            this.pos = 0;
        }

        static double eval(String expression) {
            return new MathEval(expression.replaceAll("\\s+", "")).parseExpression();
        }

        private double parseExpression() {
            double result = parseTerm();
            while (pos < expr.length()) {
                char c = expr.charAt(pos);
                if (c == '+') { pos++; result += parseTerm(); }
                else if (c == '-') { pos++; result -= parseTerm(); }
                else break;
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < expr.length()) {
                char c = expr.charAt(pos);
                if (c == '*') { pos++; result *= parseFactor(); }
                else if (c == '/') { pos++; result /= parseFactor(); }
                else if (c == '%') { pos++; result %= parseFactor(); }
                else break;
            }
            return result;
        }

        private double parseFactor() {
            if (pos < expr.length() && expr.charAt(pos) == '-') {
                pos++;
                return -parseFactor();
            }
            if (pos < expr.length() && expr.charAt(pos) == '+') {
                pos++;
                return parseFactor();
            }
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++; // consume '('
                double result = parseExpression();
                if (pos < expr.length() && expr.charAt(pos) == ')') pos++; // consume ')'
                return result;
            }
            return parseNumber();
        }

        private double parseNumber() {
            int start = pos;
            while (pos < expr.length()) {
                char c = expr.charAt(pos);
                if (Character.isDigit(c) || c == '.' || (pos == start && c == '-')) pos++;
                else break;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Expected number at position " + pos + " in: " + expr);
            }
            return Double.parseDouble(expr.substring(start, pos));
        }
    }

    private QVariable resolve(String name) {
        // 1. Cache hit
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        // 2. Walk parent chain for VariableProvider components
        QComponent current = originComponent;
        while (current != null) {
            if (current instanceof VariableProvider provider) {
                Map<String, QVariable> provided = provider.provideVariables(quester);
                if (provided != null && provided.containsKey(name)) {
                    QVariable found = provided.get(name);
                    cache.putAll(provided); // cache the whole batch
                    return found;
                }
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Populates built-in variables from the Quester.
     * These are always available without any VariableProvider.
     */
    private void populateBuiltins() {
        if (quester == null) return;
        cache.put("quester_name", new QVariable(quester.getName()));
        if (quester.getLocation() != null) {
            cache.put("quester_x", new QVariable(quester.getLocation().getBlockX()));
            cache.put("quester_y", new QVariable(quester.getLocation().getBlockY()));
            cache.put("quester_z", new QVariable(quester.getLocation().getBlockZ()));
            cache.put("quester_world", new QVariable(
                    quester.getLocation().getWorld() != null ? quester.getLocation().getWorld().getName() : ""));
        }
        // Player-specific builtins
        if (quester instanceof QPlayer qp) {
            addPlayerBuiltins(qp);
        }
        // Event-scoped builtins: fall back to the last instigator's player context if present.
        if (quester instanceof QEvent event && event.getLastInstigator() != null) {
            addPlayerBuiltins(event.getLastInstigator());
        }
    }

    private void addPlayerBuiltins(QPlayer qp) {
        cache.put("player_name", new QVariable(qp.getName()));
        Player p = qp.getPlayer();
        if (p != null) {
            cache.put("player_health", new QVariable(p.getHealth()));
            org.bukkit.attribute.AttributeInstance maxHp = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            cache.put("player_max_health", new QVariable(maxHp != null ? maxHp.getValue() : 20.0));
            cache.put("player_level", new QVariable(p.getLevel()));
            cache.put("player_food", new QVariable(p.getFoodLevel()));
            org.bukkit.Location loc = p.getLocation();
            cache.put("player_x", new QVariable(loc.getBlockX()));
            cache.put("player_y", new QVariable(loc.getBlockY()));
            cache.put("player_z", new QVariable(loc.getBlockZ()));
            cache.put("player_world", new QVariable(loc.getWorld() != null ? loc.getWorld().getName() : ""));
        }
    }
}

