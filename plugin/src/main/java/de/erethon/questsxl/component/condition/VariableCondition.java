package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.script.VariableProvider;

import java.util.Map;

/**
 * Evaluates a comparison between two values.
 *
 * <p>Type {@code number} compares the two sides as doubles using
 * {@code ==  !=  <  <=  >  >=}.</p>
 *
 * <p>Type {@code string} compares the two sides as strings (case-sensitive by default) using
 * {@code ==  !=  contains  starts_with  ends_with}.</p>
 *
 * <p>Both {@code left} and {@code right} are resolved through the current
 * {@link ExecutionContext}, so variable tokens like {@code %score_value%} are
 * substituted before comparison. For the {@code number} type the resolved
 * strings are additionally evaluated as math expressions, so
 * {@code left: "%progress% * 2"} works as expected.</p>
 *
 * <p>The resolved values are exposed as {@code %left%} and {@code %right%}
 * to child actions ({@code onSuccess} / {@code onFail}).</p>
 *
 * <pre>
 * # number example
 * enough_progress:
 *   type: variable
 *   left: "%progress%"
 *   operator: ">="
 *   right: "10"
 *   value_type: number
 *
 * # string example
 *  name_check:
 *   type: variable
 *   left: "%player_name%"
 *   operator: "starts_with"
 *   right: "Steve"
 *   value_type: string
 * </pre>
 */
@QLoadableDoc(
        value = "variable",
        description = "Evaluates a comparison between two values. Supports variable tokens on both sides.",
        shortExample = "variable: left=%progress%; operator=>=; right=10; value_type=number",
        longExample = {
                "variable:",
                "  left: \"%progress%\"",
                "  operator: \">=\"",
                "  right: \"10\"",
                "  value_type: number"
        }
)
public class VariableCondition extends QBaseCondition implements VariableProvider {

    /** Supported value types. */
    public enum ValueType { NUMBER, STRING }

    /** Comparison operators. Which ones are valid depends on {@link ValueType}. */
    public enum Operator {
        EQ("=="),
        NEQ("!="),
        LT("<"),
        LTE("<="),
        GT(">"),
        GTE(">="),
        CONTAINS("contains"),
        STARTS_WITH("starts_with"),
        ENDS_WITH("ends_with");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public static Operator fromString(String raw) {
            String trimmed = raw.trim();
            for (Operator op : values()) {
                if (op.symbol.equalsIgnoreCase(trimmed)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: '" + raw + "'");
        }
    }

    @QParamDoc(name = "left", description = "The left-hand side. Supports %variables% and math expressions (for number type).", required = true)
    private String rawLeft;

    @QParamDoc(name = "right", description = "The right-hand side. Supports %variables% and math expressions (for number type).", required = true)
    private String rawRight;

    @QParamDoc(name = "operator", description = "The comparison operator: ==, !=, <, <=, >, >= (number); ==, !=, contains, starts_with, ends_with (string).", required = true)
    private Operator operator;

    @QParamDoc(name = "value_type", description = "The type to compare as: 'number' or 'string'.", def = "number")
    private ValueType valueType;

    @QParamDoc(name = "case_sensitive", description = "Whether string comparison is case-sensitive.", def = "true")
    private boolean caseSensitive;

    // Snapshot of resolved values — populated during checkInternal, exposed via provideVariables
    private String resolvedLeft = "";
    private String resolvedRight = "";

    @Override
    protected boolean checkInternal(Quester quester) {
        ExecutionContext ctx = ExecutionContext.current();

        if (valueType == ValueType.NUMBER) {
            double left  = ctx != null ? ctx.resolveDouble(rawLeft)  : parseDoubleQuiet(rawLeft);
            double right = ctx != null ? ctx.resolveDouble(rawRight) : parseDoubleQuiet(rawRight);

            // Strip trailing ".0" for whole numbers so %left% / %right% read cleanly
            resolvedLeft  = formatDouble(left);
            resolvedRight = formatDouble(right);

            boolean result = switch (operator) {
                case EQ  -> left == right;
                case NEQ -> left != right;
                case LT  -> left <  right;
                case LTE -> left <= right;
                case GT  -> left >  right;
                case GTE -> left >= right;
                default  -> {
                    logUnsupportedOperator(operator, valueType);
                    yield false;
                }
            };
            return result ? success(quester) : fail(quester);
        } else {
            // STRING
            String left  = ctx != null ? ctx.resolveString(rawLeft)  : rawLeft;
            String right = ctx != null ? ctx.resolveString(rawRight) : rawRight;

            resolvedLeft  = left  != null ? left  : "";
            resolvedRight = right != null ? right : "";

            String cmpLeft  = caseSensitive ? resolvedLeft  : resolvedLeft.toLowerCase();
            String cmpRight = caseSensitive ? resolvedRight : resolvedRight.toLowerCase();

            boolean result = switch (operator) {
                case EQ          -> cmpLeft.equals(cmpRight);
                case NEQ         -> !cmpLeft.equals(cmpRight);
                case CONTAINS    -> cmpLeft.contains(cmpRight);
                case STARTS_WITH -> cmpLeft.startsWith(cmpRight);
                case ENDS_WITH   -> cmpLeft.endsWith(cmpRight);
                default          -> {
                    logUnsupportedOperator(operator, valueType);
                    yield false;
                }
            };
            return result ? success(quester) : fail(quester);
        }
    }

    /** Exposes %left% and %right% with their resolved values to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of(
                "left",  new QVariable(resolvedLeft),
                "right", new QVariable(resolvedRight)
        );
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        rawLeft  = cfg.getString("left");
        rawRight = cfg.getString("right");

        String opRaw = cfg.getString("operator", "==");
        try {
            operator = Operator.fromString(opRaw);
        } catch (IllegalArgumentException e) {
            operator = Operator.EQ;
        }

        String typRaw = cfg.getString("value_type", "number");
        try {
            valueType = ValueType.valueOf(typRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            valueType = ValueType.NUMBER;
        }

        caseSensitive = cfg.getBoolean("case_sensitive", true);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static double parseDoubleQuiet(String raw) {
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static void logUnsupportedOperator(Operator op, ValueType type) {
        de.erethon.questsxl.QuestsXL.get().getLogger().warning(
                "[VariableCondition] Operator '" + op.symbol + "' is not supported for type " + type + " — condition will always fail.");
    }
}

