package com.example.calculator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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
        // Nếu vừa tính xong mà bấm số mới → reset
        if (justCalculated) {
            expression.setLength(0);
            tvResult.setText("0");
            justCalculated = false;
        }
        expression.append(num);
        tvExpression.setText(expression.toString());
        // KHÔNG gọi updatePreview — chỉ hiện kết quả khi bấm =
    }

    private void onOperator(String op) {
        // Nếu vừa tính xong → giữ kết quả làm số đầu tiên
        justCalculated = false;
        if (expression.length() > 0) {
            char last = expression.charAt(expression.length() - 1);
            if (last == '+' || last == '−' || last == '×' || last == '÷') {
                // Thay operator cuối
                expression.setCharAt(expression.length() - 1, op.charAt(0));
            } else {
                expression.append(op);
            }
        }
        tvExpression.setText(expression.toString());
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
        tvExpression.setText(expression.toString());
    }

    private void onParen(String p) {
        if (justCalculated) { expression.setLength(0); justCalculated = false; }
        expression.append(p);
        tvExpression.setText(expression.toString());
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
            tvExpression.setText(expression.toString());
        }
        // Nếu xóa hết thì reset màn hình
        if (expression.length() == 0) {
            tvResult.setText("0");
        }
    }

    private void onEqual() {
        if (expression.length() == 0) return;
        String expr = expression.toString();
        try {
            double result = evaluate(expr);
            // Format: bỏ .0 nếu là số nguyên
            String resultStr = (result == Math.floor(result) && !Double.isInfinite(result))
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
            tvExpression.setText(expr + " =");
            tvResult.setText(resultStr);
            // Lưu kết quả để dùng tiếp
            expression.setLength(0);
            expression.append(resultStr);
            justCalculated = true;
        } catch (Exception e) {
            tvExpression.setText(expr);
            tvResult.setText("Error");
            expression.setLength(0);
            justCalculated = true;
        }
    }

    // ---- Recursive Descent Parser: đúng thứ tự ưu tiên × ÷ trước + − ----
    private int pos;
    private String evalExpr;

    private double evaluate(String expr) {
        evalExpr = expr.replace("×", "*").replace("÷", "/").replace("−", "-");
        pos = 0;
        double result = parseExpr();
        if (pos != evalExpr.length()) throw new RuntimeException("Unexpected char");
        return result;
    }

    // Cộng trừ — ưu tiên thấp nhất
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

    // Nhân chia — ưu tiên cao hơn
    private double parseTerm() {
        double val = parseFactor();
        while (pos < evalExpr.length()) {
            char c = evalExpr.charAt(pos);
            if (c == '*') { pos++; val *= parseFactor(); }
            else if (c == '/') {
                pos++;
                double d = parseFactor();
                if (d == 0) throw new ArithmeticException("Chia cho 0");
                val /= d;
            }
            else break;
        }
        return val;
    }

    // Số, ngoặc, số âm
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
        while (pos < evalExpr.length() &&
                (Character.isDigit(evalExpr.charAt(pos)) || evalExpr.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) throw new RuntimeException("Expected number at pos " + pos);
        return Double.parseDouble(evalExpr.substring(start, pos));
    }
}