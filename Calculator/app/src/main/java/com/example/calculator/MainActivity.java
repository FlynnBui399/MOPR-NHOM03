package com.example.calculator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult;
    private StringBuilder expression = new StringBuilder();
    private boolean justCalculated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tv_expression);
        tvResult     = findViewById(R.id.tv_result);

        int[] numIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9};
        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> onNumber(((Button) v).getText().toString()));
        }

        findViewById(R.id.btn_add).setOnClickListener(v -> onOperator("+"));
        findViewById(R.id.btn_sub).setOnClickListener(v -> onOperator("−"));
        findViewById(R.id.btn_mul).setOnClickListener(v -> onOperator("×"));
        findViewById(R.id.btn_div).setOnClickListener(v -> onOperator("÷"));

        findViewById(R.id.btn_dot).setOnClickListener(v -> onDot());
        findViewById(R.id.btn_paren_open).setOnClickListener(v -> onParen("("));
        findViewById(R.id.btn_paren_close).setOnClickListener(v -> onParen(")"));
        findViewById(R.id.btn_clear).setOnClickListener(v -> onClear());
        findViewById(R.id.btn_del).setOnClickListener(v -> onDelete());
        findViewById(R.id.btn_equal).setOnClickListener(v -> onEqual());
    }

    private void onNumber(String num) {
        // If a calculation was just completed and a new number is pressed, reset
        if (justCalculated) {
            expression.setLength(0);
            tvResult.setText("0");
            justCalculated = false;
        }
        expression.append(num);
        tvExpression.setText(formatExpression(expression.toString()));
        // Do NOT call updatePreview - only show result when = is pressed
    }

    private void onOperator(String op) {
        // If a calculation was just completed, keep the result as the first operand
        justCalculated = false;
        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (last == '+' || last == '−' || last == '×' || last == '÷') {
                // Replace the last operator
                expression.setCharAt(expression.length() - 1, op.charAt(0));
            } else {
                expression.append(op);
            }
        }
        tvExpression.setText(formatExpression(expression.toString()));
    }

    private void onDot() {
        if (justCalculated) { expression.setLength(0); justCalculated = false; }
        String expr = expression.toString();
        int lastOp = Math.max(
                Math.max(expr.lastIndexOf('+'), expr.lastIndexOf('−')),
                Math.max(expr.lastIndexOf('×'), expr.lastIndexOf('÷')));
        String current = expr.substring(lastOp + 1);
        if (!current.contains(".")) {
            if (current.isEmpty()) expression.append("0");
            expression.append(".");
        }
        tvExpression.setText(formatExpression(expression.toString()));
    }

    private void onParen(String p) {
        if (justCalculated) { expression.setLength(0); justCalculated = false; }
        expression.append(p);
        tvExpression.setText(formatExpression(expression.toString()));
    }

    private void onClear() {
        expression.setLength(0);
        tvExpression.setText("");
        tvResult.setText("0");
        justCalculated = false;
    }

    private void onDelete() {
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
            tvExpression.setText(formatExpression(expression.toString()));
        }
        // If everything is deleted, reset the display
        if (expression.length() == 0) {
            tvResult.setText("0");
        }
    }

    private void onEqual() {
        if (expression.length() == 0) return;
        String expr = expression.toString();
        try {
            double result = evaluate(expr);
            String resultStr = formatResult(result);
            
            String rawResult = (result == Math.floor(result) && !Double.isInfinite(result))
                    ? String.valueOf((long) result)
                    : String.valueOf(result);

            tvExpression.setText(formatExpression(expr) + " =");
            tvResult.setText(resultStr);
            // Save the result for continued use
            expression.setLength(0);
            expression.append(rawResult);
            justCalculated = true;
        } catch (ArithmeticException e) {
            tvExpression.setText(formatExpression(expr) + " =");
            tvResult.setText("Can't divide by zero");
            expression.setLength(0);
            justCalculated = true;
        } catch (Exception e) {
            tvExpression.setText(formatExpression(expr));
            tvResult.setText("Error");
            expression.setLength(0);
            justCalculated = true;
        }
    }

    private String formatExpression(String expr) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentNumber = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == 'E' || c == 'e' || (currentNumber.length() > 0 && (c == '-' || c == '+') && (expr.charAt(i - 1) == 'E' || expr.charAt(i - 1) == 'e'))) {
                currentNumber.append(c);
            } else {
                if (currentNumber.length() > 0) {
                    result.append(formatNumberWithCommas(currentNumber.toString()));
                    currentNumber.setLength(0);
                }
                result.append(c);
            }
        }
        if (currentNumber.length() > 0) {
            result.append(formatNumberWithCommas(currentNumber.toString()));
        }
        return result.toString();
    }

    private String formatNumberWithCommas(String numStr) {
        if (numStr.isEmpty()) return "";
        if (numStr.contains("E") || numStr.contains("e") || numStr.equals("Infinity") || numStr.equals("-Infinity") || numStr.equals("NaN")) {
            return numStr;
        }
        try {
            String[] parts = numStr.split("\\.", -1);
            String intPart = parts[0];
            String decPart = parts.length > 1 ? parts[1] : "";
            if (intPart.isEmpty() || intPart.equals("-")) {
                return numStr;
            }
            BigDecimal iPart = new BigDecimal(intPart);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);
            String formattedInt = formatter.format(iPart);
            if (numStr.contains(".")) {
                return formattedInt + "." + decPart;
            } else {
                return formattedInt;
            }
        } catch (Exception e) {
            return numStr;
        }
    }

    private String formatResult(double result) {
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            return "Error";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        if (Math.abs(result) >= 1E11 || (Math.abs(result) > 0 && Math.abs(result) < 1E-7)) {
            DecimalFormat sciFormatter = new DecimalFormat("0.#####E0", symbols);
            return sciFormatter.format(result).replace("E", "e");
        } else {
            DecimalFormat formatter = new DecimalFormat("#,###.########", symbols);
            return formatter.format(result);
        }
    }

    // ---- Recursive Descent Parser: correct precedence, * / before + - ----
    private int pos;
    private String evalExpr;

    private double evaluate(String expr) {
        evalExpr = expr.replace("×", "*").replace("÷", "/").replace("−", "-");
        pos = 0;
        double result = parseExpr();
        if (pos != evalExpr.length()) throw new RuntimeException("Unexpected char");
        return result;
    }

    // Addition and subtraction - lowest precedence
    private double parseExpr() {
        double val = parseTerm();
        while (pos < evalExpr.length()) {
            char c = evalExpr.charAt(pos);
            if (c == '+') { pos++; val += parseTerm(); }
            else if (c == '-') { pos++; val -= parseTerm(); }
            else break;
        }
        return val;
    }

    // Multiplication and division - higher precedence
    private double parseTerm() {
        double val = parseFactor();
        while (pos < evalExpr.length()) {
            char c = evalExpr.charAt(pos);
            if (c == '*') { pos++; val *= parseFactor(); }
            else if (c == '/') {
                pos++;
                double d = parseFactor();
                if (d == 0) throw new ArithmeticException("Division by zero");
                val /= d;
            }
            else break;
        }
        return val;
    }

    // Number, parentheses, negative number
    private double parseFactor() {
        if (pos >= evalExpr.length()) throw new RuntimeException("Unexpected end");
        char c = evalExpr.charAt(pos);
        if (c == '(') {
            pos++;
            double val = parseExpr();
            if (pos < evalExpr.length() && evalExpr.charAt(pos) == ')') pos++;
            return val;
        }
        if (c == '-') { pos++; return -parseFactor(); }
        int start = pos;
        while (pos < evalExpr.length()) {
            char ch = evalExpr.charAt(pos);
            if (Character.isDigit(ch) || ch == '.' || ch == 'E' || ch == 'e') {
                pos++;
            } else if ((ch == '+' || ch == '-') && pos > 0 && 
                      (evalExpr.charAt(pos-1) == 'E' || evalExpr.charAt(pos-1) == 'e')) {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) throw new RuntimeException("Expected number at pos " + pos);
        return Double.parseDouble(evalExpr.substring(start, pos));
    }
}