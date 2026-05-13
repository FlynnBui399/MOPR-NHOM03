package com.example.calculator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import androidx.core.widget.TextViewCompat;
import android.util.TypedValue;
import android.graphics.Color;

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

        // Initialize state
        setStateTyping();
    }

    private void onNumber(String num) {
        if (justCalculated) {
            expression.setLength(0);
            tvResult.setText("0");
            justCalculated = false;
            setStateTyping();
        }
        expression.append(num);
        tvExpression.setText(formatExpression(expression.toString()));
        updateLivePreview();
    }

    private void onOperator(String op) {
        if (justCalculated) {
            justCalculated = false;
            setStateTyping();
        }
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
        updateLivePreview();
    }

    private void onDot() {
        if (justCalculated) { 
            expression.setLength(0); 
            justCalculated = false; 
            setStateTyping();
        }
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
        updateLivePreview();
    }

    private void onParen(String p) {
        if (justCalculated) { 
            expression.setLength(0); 
            justCalculated = false; 
            setStateTyping();
        }
        expression.append(p);
        tvExpression.setText(formatExpression(expression.toString()));
        updateLivePreview();
    }

    private void onClear() {
        expression.setLength(0);
        tvExpression.setText("");
        tvResult.setText("0");
        justCalculated = false;
        setStateTyping();
    }

    private void onDelete() {
        if (justCalculated) {
            justCalculated = false;
            setStateTyping();
        }
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
            tvExpression.setText(formatExpression(expression.toString()));
            updateLivePreview();
        }
        if (expression.length() == 0) {
            tvResult.setText("0");
        }
    }

    private void onEqual() {
        if (expression.length() == 0) return;
        String expr = expression.toString();
        try {
            BigDecimal result = evaluate(expr);
            String resultStr = formatResult(result);
            
            String rawResult = result.stripTrailingZeros().toPlainString();

            tvExpression.setText(formatExpression(expr) + " =");
            tvResult.setText(resultStr);
            // Save the result for continued use
            expression.setLength(0);
            expression.append(rawResult);
            justCalculated = true;
            setStateCalculated();
        } catch (ArithmeticException e) {
            tvExpression.setText(formatExpression(expr) + " =");
            tvResult.setText("Can't divide by zero");
            expression.setLength(0);
            justCalculated = true;
            setStateCalculated();
        } catch (Exception e) {
            tvExpression.setText(formatExpression(expr));
            tvResult.setText("Error");
            expression.setLength(0);
            justCalculated = true;
            setStateCalculated();
        }
    }

    private void setStateTyping() {
        tvExpression.setTextColor(Color.parseColor("#FFFFFF"));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvExpression, 24, 72, 2, TypedValue.COMPLEX_UNIT_SP);
        
        tvResult.setTextColor(Color.parseColor("#999999"));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvResult, 18, 36, 2, TypedValue.COMPLEX_UNIT_SP);
    }

    private void setStateCalculated() {
        tvExpression.setTextColor(Color.parseColor("#999999"));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvExpression, 18, 36, 2, TypedValue.COMPLEX_UNIT_SP);
        
        tvResult.setTextColor(Color.parseColor("#FFFFFF"));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tvResult, 24, 72, 2, TypedValue.COMPLEX_UNIT_SP);
    }

    private void updateLivePreview() {
        if (expression.length() == 0) {
            tvResult.setText("0");
            return;
        }
        try {
            BigDecimal result = evaluate(expression.toString());
            tvResult.setText(formatResult(result));
        } catch (ArithmeticException e) {
            tvResult.setText("Can't divide by zero");
        } catch (Exception e) {
            tvResult.setText("");
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

    private String formatResult(BigDecimal result) {
        result = result.stripTrailingZeros();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        
        // Show up to 20 digits without scientific notation
        if (result.abs().compareTo(new BigDecimal("1E20")) >= 0 || 
           (result.compareTo(BigDecimal.ZERO) != 0 && result.abs().compareTo(new BigDecimal("1E-8")) < 0)) {
            DecimalFormat sciFormatter = new DecimalFormat("0.########E0", symbols);
            return sciFormatter.format(result).replace("E", "e");
        } else {
            DecimalFormat formatter = new DecimalFormat("#,###.##########", symbols);
            return formatter.format(result);
        }
    }

    // ---- Recursive Descent Parser: correct precedence, * / before + - ----
    private int pos;
    private String evalExpr;

    private BigDecimal evaluate(String expr) {
        evalExpr = expr.replace("×", "*").replace("÷", "/").replace("−", "-");
        pos = 0;
        BigDecimal result = parseExpr();
        if (pos != evalExpr.length()) throw new RuntimeException("Unexpected char");
        return result;
    }

    // Addition and subtraction - lowest precedence
    private BigDecimal parseExpr() {
        BigDecimal val = parseTerm();
        while (pos < evalExpr.length()) {
            char c = evalExpr.charAt(pos);
            if (c == '+') { pos++; val = val.add(parseTerm()); }
            else if (c == '-') { pos++; val = val.subtract(parseTerm()); }
            else break;
        }
        return val;
    }

    // Multiplication and division - higher precedence
    private BigDecimal parseTerm() {
        BigDecimal val = parseFactor();
        while (pos < evalExpr.length()) {
            char c = evalExpr.charAt(pos);
            if (c == '*') { pos++; val = val.multiply(parseFactor()); }
            else if (c == '/') {
                pos++;
                BigDecimal d = parseFactor();
                if (d.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
                val = val.divide(d, java.math.MathContext.DECIMAL128);
            }
            else break;
        }
        return val;
    }

    // Number, parentheses, negative number
    private BigDecimal parseFactor() {
        if (pos >= evalExpr.length()) throw new RuntimeException("Unexpected end");
        char c = evalExpr.charAt(pos);
        if (c == '(') {
            pos++;
            BigDecimal val = parseExpr();
            if (pos < evalExpr.length() && evalExpr.charAt(pos) == ')') pos++;
            return val;
        }
        if (c == '-') { pos++; return parseFactor().negate(); }
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
        return new BigDecimal(evalExpr.substring(start, pos));
    }
}