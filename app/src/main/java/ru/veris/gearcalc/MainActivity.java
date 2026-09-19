package ru.veris.gearcalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(20, 24, 28);
    private static final int CARD = Color.rgb(31, 37, 43);
    private static final int FIELD = Color.rgb(42, 49, 56);
    private static final int BORDER = Color.rgb(63, 72, 82);
    private static final int TEXT = Color.rgb(245, 247, 249);
    private static final int MUTED = Color.rgb(176, 186, 196);
    private static final int ACCENT = Color.rgb(21, 177, 204);
    private static final int WARN = Color.rgb(255, 193, 7);
    private static final int BAD = Color.rgb(255, 120, 120);

    private Spinner machineSpinner, toothTypeSpinner, helixSpinner, hobHandSpinner, cutMethodSpinner;
    private EditText zInput, moduleInput, startsInput, betaDegInput, betaMinInput, betaSecInput;
    private LinearLayout helixBox, resultsBox;
    private TextView statusText, resultTitle, indexingFormula, indexingRatio, indexingError,
            indexingMount, differentialFormula, differentialRatio, differentialError, differentialMount,
            setupNote, candidatesText;
    private GearDiagramView indexingDiagram, differentialDiagram;
    private SharedPreferences prefs;

    private MachineConfig machine;
    private Candidate indexingCandidate;
    private Candidate differentialCandidate;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("veris_gear_calc", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(16), dp(14), dp(16), dp(28));
        scroll.addView(main, new ScrollView.LayoutParams(-1, -2));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.veris_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        brandTextLp.setMargins(dp(10), 0, 0, 0);
        TextView brand = text("ВЕРИС", 15, ACCENT, true);
        brand.setLetterSpacing(0.08f);
        brandText.addView(brand);
        brandText.addView(text("Калькулятор гитары", 25, TEXT, true));
        brandRow.addView(brandText, brandTextLp);
        main.addView(brandRow);

        TextView sub = text("Подбор сменных колёс для зубофрезерных станков", 13, MUTED, false);
        LinearLayout.LayoutParams subLp = lp(-1, -2);
        subLp.setMargins(0, dp(4), 0, dp(14));
        main.addView(sub, subLp);

        LinearLayout inputCard = card();
        main.addView(inputCard, lp(-1, -2));

        machineSpinner = spinner(new String[]{"53А50Н", "5Е32"});
        inputCard.addView(labeled("Модель станка", machineSpinner));

        toothTypeSpinner = spinner(new String[]{"Прямой зуб", "Косой зуб"});
        inputCard.addView(labeled("Тип зуба", toothTypeSpinner));

        LinearLayout zr = row();
        zInput = numberField("56", false);
        startsInput = numberField("1", false);
        zr.addView(labeled("Число зубьев z", zInput), halfLeft());
        zr.addView(labeled("Заходов фрезы k", startsInput), halfRight());
        inputCard.addView(zr);

        moduleInput = numberField("2,5", true);
        inputCard.addView(labeled("Нормальный модуль mₙ, мм", moduleInput));

        helixBox = new LinearLayout(this);
        helixBox.setOrientation(LinearLayout.VERTICAL);
        inputCard.addView(helixBox, lp(-1, -2));

        LinearLayout angleRow = row();
        betaDegInput = numberField("12", false);
        betaMinInput = numberField("0", false);
        betaSecInput = numberField("0", true);
        angleRow.addView(labeled("β, °", betaDegInput), third(0));
        angleRow.addView(labeled("мин", betaMinInput), third(1));
        angleRow.addView(labeled("сек", betaSecInput), third(2));
        helixBox.addView(angleRow);

        helixSpinner = spinner(new String[]{"Правый наклон", "Левый наклон"});
        hobHandSpinner = spinner(new String[]{"Правая фреза", "Левая фреза"});
        cutMethodSpinner = spinner(new String[]{"Встречное фрезерование", "Попутное фрезерование"});
        helixBox.addView(labeled("Направление зуба", helixSpinner));
        helixBox.addView(labeled("Направление витков фрезы", hobHandSpinner));
        helixBox.addView(labeled("Метод фрезерования", cutMethodSpinner));

        statusText = text("", 13, BAD, false);
        statusText.setVisibility(View.GONE);
        LinearLayout.LayoutParams stLp = lp(-1, -2);
        stLp.setMargins(0, dp(10), 0, 0);
        inputCard.addView(statusText, stLp);

        Button calculate = primaryButton("Рассчитать");
        calculate.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams calcLp = lp(-1, dp(52));
        calcLp.setMargins(0, dp(14), 0, 0);
        inputCard.addView(calculate, calcLp);

        LinearLayout buttonRow = row();
        Button stock = secondaryButton("Шестерни в наличии");
        stock.setOnClickListener(v -> showStockDialog());
        Button clear = secondaryButton("Сбросить");
        clear.setOnClickListener(v -> resetInputs());
        buttonRow.addView(stock, halfLeftHeight());
        buttonRow.addView(clear, halfRightHeight());
        LinearLayout.LayoutParams brLp = lp(-1, -2);
        brLp.setMargins(0, dp(10), 0, 0);
        inputCard.addView(buttonRow, brLp);

        resultsBox = new LinearLayout(this);
        resultsBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resLp = lp(-1, -2);
        resLp.setMargins(0, dp(14), 0, 0);
        main.addView(resultsBox, resLp);

        resultTitle = text("Результат", 21, TEXT, true);
        resultsBox.addView(resultTitle);

        LinearLayout idxCard = card();
        LinearLayout.LayoutParams idxLp = lp(-1, -2);
        idxLp.setMargins(0, dp(10), 0, 0);
        resultsBox.addView(idxCard, idxLp);
        idxCard.addView(sectionTitle("Гитара деления"));
        indexingFormula = valueText();
        idxCard.addView(indexingFormula);
        indexingRatio = bigValue();
        idxCard.addView(indexingRatio);
        indexingError = smallValue();
        idxCard.addView(indexingError);
        indexingDiagram = new GearDiagramView(this);
        idxCard.addView(indexingDiagram, lp(-1, dp(205)));
        indexingMount = bodyText();
        idxCard.addView(indexingMount);

        LinearLayout diffCard = card();
        diffCard.setTag("diffCard");
        LinearLayout.LayoutParams diffLp = lp(-1, -2);
        diffLp.setMargins(0, dp(12), 0, 0);
        resultsBox.addView(diffCard, diffLp);
        diffCard.addView(sectionTitle("Гитара дифференциала"));
        differentialFormula = valueText();
        diffCard.addView(differentialFormula);
        differentialRatio = bigValue();
        diffCard.addView(differentialRatio);
        differentialError = smallValue();
        diffCard.addView(differentialError);
        differentialDiagram = new GearDiagramView(this);
        diffCard.addView(differentialDiagram, lp(-1, dp(205)));
        differentialMount = bodyText();
        diffCard.addView(differentialMount);

        LinearLayout noteCard = card();
        LinearLayout.LayoutParams noteLp = lp(-1, -2);
        noteLp.setMargins(0, dp(12), 0, 0);
        resultsBox.addView(noteCard, noteLp);
        noteCard.addView(sectionTitle("Установка и проверка"));
        setupNote = bodyText();
        noteCard.addView(setupNote);
        candidatesText = bodyText();
        LinearLayout.LayoutParams candLp = lp(-1, -2);
        candLp.setMargins(0, dp(10), 0, 0);
        noteCard.addView(candidatesText, candLp);

        TextView passportNote = text(
                "Расчёт выполняется офлайн. 53А50Н: паспорт 53А50/53А50Н/53А80/53А80Н. " +
                        "5Е32: профиль станка 5Е32. Схемы в приложении монтажные, не в масштабе.",
                12, MUTED, false);
        LinearLayout.LayoutParams pLp = lp(-1, -2);
        pLp.setMargins(0, dp(14), 0, 0);
        main.addView(passportNote, pLp);

        machineSpinner.setSelection(prefs.getInt("last_machine", 0));
        toothTypeSpinner.setSelection(prefs.getInt("last_tooth_type", 0));
        toothTypeSpinner.setOnItemSelectedListener(new SimpleSelection() {
            @Override public void selected(int position) {
                helixBox.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
        });
        machineSpinner.setOnItemSelectedListener(new SimpleSelection() {
            @Override public void selected(int position) {
                machine = position == 0 ? MachineConfig.m53() : MachineConfig.m5e32();
            }
        });

        machine = machineSpinner.getSelectedItemPosition() == 0 ? MachineConfig.m53() : MachineConfig.m5e32();
        helixBox.setVisibility(toothTypeSpinner.getSelectedItemPosition() == 1 ? View.VISIBLE : View.GONE);
        calculate();
        setContentView(scroll);
    }

    private void calculate() {
        machine = machineSpinner.getSelectedItemPosition() == 0 ? MachineConfig.m53() : MachineConfig.m5e32();
        int z = (int) parse(zInput.getText().toString());
        int k = (int) parse(startsInput.getText().toString());
        double mn = parse(moduleInput.getText().toString());
        boolean helical = toothTypeSpinner.getSelectedItemPosition() == 1;
        double beta = parseAngle();

        if (z <= 0 || k <= 0 || Double.isNaN(mn) || mn <= 0 ||
                (helical && (Double.isNaN(beta) || beta <= 0 || beta >= 90))) {
            statusText.setText("Проверьте входные данные: z, k и модуль должны быть больше нуля; для косого зуба β — от 0° до 90°.");
            statusText.setVisibility(View.VISIBLE);
            resultsBox.setVisibility(View.GONE);
            return;
        }

        statusText.setVisibility(View.GONE);
        resultsBox.setVisibility(View.VISIBLE);
        prefs.edit()
                .putInt("last_machine", machineSpinner.getSelectedItemPosition())
                .putInt("last_tooth_type", toothTypeSpinner.getSelectedItemPosition())
                .apply();

        double idxTarget = (z <= 161 ? 24.0 : 48.0) * k / z;
        List<Candidate> idx = GearSolver.solve(idxTarget, available(machine), 5);
        indexingCandidate = idx.isEmpty() ? null : idx.get(0);

        String ef = z <= 161 ? machine.lowEf : machine.highEf;
        indexingFormula.setText(
                "Требуемое отношение: " + (z <= 161 ? "24·k/z" : "48·k/z") +
                        " = " + fmt(idxTarget, 8) + "   •   перебор e/f: " + ef);

        if (indexingCandidate == null) {
            indexingRatio.setText("Нет комбинации");
            indexingError.setText("Проверьте набор шестерён в наличии.");
            indexingMount.setText("");
            indexingDiagram.setCandidate(null, false);
        } else {
            fillResult(indexingCandidate, indexingRatio, indexingError, indexingMount, indexingDiagram, false);
        }

        View diffCard = resultsBox.findViewWithTag("diffCard");
        List<Candidate> diffs = Collections.emptyList();

        if (helical) {
            diffCard.setVisibility(View.VISIBLE);
            double diffTarget = 7.95775 * Math.sin(Math.toRadians(beta)) / (mn * k);
            diffs = GearSolver.solve(diffTarget, available(machine), 5);
            differentialCandidate = diffs.isEmpty() ? null : diffs.get(0);
            differentialFormula.setText(
                    "Требуемое отношение: 7,95775·sinβ/(mₙ·k) = " + fmt(diffTarget, 8));

            if (differentialCandidate == null) {
                differentialRatio.setText("Нет комбинации");
                differentialError.setText("Проверьте набор шестерён в наличии.");
                differentialMount.setText("");
                differentialDiagram.setCandidate(null, true);
            } else {
                fillResult(differentialCandidate, differentialRatio, differentialError,
                        differentialMount, differentialDiagram, true);
            }
        } else {
            diffCard.setVisibility(View.GONE);
            differentialCandidate = null;
        }

        resultTitle.setText(
                machine.name + " • z=" + z + " • mₙ=" + fmt(mn, 3) +
                        (helical ? " • β=" + angleText(beta) : " • прямой зуб"));
        setupNote.setText(buildSetupNote(helical, beta));
        candidatesText.setText(buildAlternatives(idx, diffs, helical));
    }

    private void fillResult(Candidate c, TextView ratio, TextView error, TextView mount,
                            GearDiagramView diagram, boolean diff) {
        ratio.setText(c.expression());
        double ppm = c.relError * 1_000_000.0;
        String quality = c.relError <= 1e-6 ? "очень точно"
                : (c.relError <= 1e-4 ? "точно" : "проверьте допуск");
        error.setText(
                "Фактическое отношение " + fmt(c.actual, 9) +
                        " • ошибка " + fmt(c.relError * 100.0, 6) + "% (" +
                        fmt(ppm, 1) + " ppm) • " + quality);

        String labelA = diff ? "a₁" : "A";
        String labelB = diff ? "b₁" : "B";
        String labelC = diff ? "c₁" : "C";
        String labelD = diff ? "d₁" : "D";

        if (c.twoGear) {
            mount.setText(
                    labelA + " = " + c.a + " — ведущая; " +
                            labelB + " = " + c.b + " — ведомая. Вторую пару не ставить. " +
                            "Промежуточное колесо, если оно требуется только для направления вращения, " +
                            "передаточное отношение не меняет.");
        } else {
            mount.setText(
                    labelA + " = " + c.a + " — ведущая; сцепить с " +
                            labelB + " = " + c.b + ". На одном валу с " +
                            labelB + " поставить " + labelC + " = " + c.c + "; " +
                            labelC + " сцепить с " + labelD + " = " + c.d + " — ведомой.");
        }
        diagram.setCandidate(c, diff);
    }

    private String buildSetupNote(boolean helical, double beta) {
        if (!helical) {
            return "Для прямозубого колеса используется гитара деления. После установки " +
                    "прокрутите станок вручную и убедитесь в свободном зацеплении сменных колёс. " +
                    "Схема показывает порядок A→B, B и C на одной оси, C→D.";
        }

        String gearHelix = helixSpinner.getSelectedItemPosition() == 0 ? "правый" : "левый";
        String hob = hobHandSpinner.getSelectedItemPosition() == 0 ? "правая" : "левая";
        String method = cutMethodSpinner.getSelectedItemPosition() == 0 ? "встречное" : "попутное";

        String recommendation = helixSpinner.getSelectedItemPosition() == hobHandSpinner.getSelectedItemPosition()
                ? "Направление зуба и фрезы одинаковое — особенно внимательно проверьте направление дифференциала и промежуточные колёса по схеме станка."
                : "Направление зуба и фрезы противоположное — это предпочтительная комбинация для 53А50Н по паспортной памятке.";

        return "Косой зуб: " + gearHelix + " наклон, " + hob.toLowerCase() +
                " фреза, " + method + " фрезерование, β=" + angleText(beta) + ". " +
                recommendation +
                " Перед запуском вручную проверьте направление вращения стола и работу дифференциала. " +
                "Паразитное колесо применяется только для изменения направления и не входит в расчёт отношения.";
    }

    private String buildAlternatives(List<Candidate> idx, List<Candidate> diff, boolean helical) {
        StringBuilder s = new StringBuilder("Ближайшие варианты");
        s.append("\nДеление: ");
        appendCandidates(s, idx);
        if (helical) {
            s.append("\nДифференциал: ");
            appendCandidates(s, diff);
        }
        return s.toString();
    }

    private void appendCandidates(StringBuilder s, List<Candidate> list) {
        int count = Math.min(3, list.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) s.append("  •  ");
            Candidate c = list.get(i);
            s.append(c.expression())
                    .append(" (Δ ")
                    .append(fmt(c.relError * 100, 5))
                    .append("%)");
        }
        if (count == 0) s.append("нет");
    }

    private void showStockDialog() {
        machine = machineSpinner.getSelectedItemPosition() == 0
                ? MachineConfig.m53() : MachineConfig.m5e32();

        final LinkedHashMap<Integer, Integer> stock = loadStock(machine);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(8));
        scroll.addView(box);

        Map<Integer, Integer> max = machine.maxCounts();
        for (Map.Entry<Integer, Integer> entry : max.entrySet()) {
            int teeth = entry.getKey();
            int maximum = entry.getValue();

            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            TextView lab = text(teeth + " зубьев", 16, TEXT, false);
            r.addView(lab, new LinearLayout.LayoutParams(0, dp(46), 1f));

            Button count = secondaryButton("");
            count.setMinWidth(dp(92));

            Runnable refresh = () -> count.setText(stock.get(teeth) + " / " + maximum);
            refresh.run();

            count.setOnClickListener(v -> {
                int now = stock.get(teeth);
                now--;
                if (now < 0) now = maximum;
                stock.put(teeth, now);
                refresh.run();
            });

            r.addView(count, new LinearLayout.LayoutParams(dp(100), dp(42)));
            box.addView(r);
        }

        new AlertDialog.Builder(this)
                .setTitle(machine.name + " — шестерни в наличии")
                .setMessage("Нажимайте на количество: 2→1→0→2. Калькулятор учитывает реальное число одинаковых колёс.")
                .setView(scroll)
                .setPositiveButton("Сохранить", (d, w) -> {
                    saveStock(machine, stock);
                    calculate();
                })
                .setNeutralButton("Паспортный комплект", (d, w) -> {
                    resetStock(machine);
                    calculate();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private LinkedHashMap<Integer, Integer> loadStock(MachineConfig m) {
        LinkedHashMap<Integer, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : m.maxCounts().entrySet()) {
            out.put(
                    e.getKey(),
                    prefs.getInt("stock_" + m.key + "_" + e.getKey(), e.getValue()));
        }
        return out;
    }

    private void saveStock(MachineConfig m, Map<Integer, Integer> stock) {
        SharedPreferences.Editor ed = prefs.edit();
        for (Map.Entry<Integer, Integer> e : stock.entrySet()) {
            ed.putInt("stock_" + m.key + "_" + e.getKey(), e.getValue());
        }
        ed.apply();
    }

    private void resetStock(MachineConfig m) {
        SharedPreferences.Editor ed = prefs.edit();
        for (Integer teeth : m.maxCounts().keySet()) {
            ed.remove("stock_" + m.key + "_" + teeth);
        }
        ed.apply();
    }

    private List<Integer> available(MachineConfig m) {
        List<Integer> out = new ArrayList<>();
        Map<Integer, Integer> stock = loadStock(m);
        for (Map.Entry<Integer, Integer> e : stock.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private void resetInputs() {
        zInput.setText("56");
        moduleInput.setText("2,5");
        startsInput.setText("1");
        betaDegInput.setText("12");
        betaMinInput.setText("0");
        betaSecInput.setText("0");
        toothTypeSpinner.setSelection(0);
        helixSpinner.setSelection(0);
        hobHandSpinner.setSelection(0);
        cutMethodSpinner.setSelection(0);
        calculate();
    }

    private double parseAngle() {
        double d = parse(betaDegInput.getText().toString());
        double m = parse(betaMinInput.getText().toString());
        double s = parse(betaSecInput.getText().toString());

        if (Double.isNaN(d) || Double.isNaN(m) || Double.isNaN(s) ||
                m < 0 || m >= 60 || s < 0 || s >= 60) {
            return Double.NaN;
        }
        return d + m / 60.0 + s / 3600.0;
    }

    private String angleText(double beta) {
        int d = (int) Math.floor(beta);
        double rem = (beta - d) * 60;
        int m = (int) Math.floor(rem);
        int s = (int) Math.round((rem - m) * 60);

        if (s == 60) {
            s = 0;
            m++;
        }
        if (m == 60) {
            m = 0;
            d++;
        }
        return d + "° " + m + "′ " + s + "″";
    }

    private double parse(String s) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private String fmt(double v, int max) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("ru", "RU"));
        sym.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat("0." + repeat('#', max), sym);
        return f.format(v);
    }

    private String repeat(char c, int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append(c);
        return s.toString();
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        l.setBackground(roundRect(CARD, BORDER, 16));
        return l;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private View labeled(String label, View child) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView l = text(label, 12, MUTED, false);
        box.addView(l);

        LinearLayout.LayoutParams cp = lp(-1, child instanceof Spinner ? dp(50) : dp(48));
        cp.setMargins(0, dp(3), 0, 0);
        box.addView(child, cp);

        LinearLayout.LayoutParams bp = lp(-1, -2);
        bp.setMargins(0, dp(5), 0, dp(5));
        box.setLayoutParams(bp);

        return box;
    }

    private EditText numberField(String initial, boolean decimal) {
        EditText e = new EditText(this);
        e.setText(initial);
        e.setTextColor(TEXT);
        e.setTextSize(19);
        e.setTypeface(Typeface.DEFAULT_BOLD);
        e.setSingleLine(true);
        e.setSelectAllOnFocus(true);
        e.setPadding(dp(12), 0, dp(12), 0);
        e.setBackground(roundRect(FIELD, BORDER, 10));

        int type = InputType.TYPE_CLASS_NUMBER;
        if (decimal) type |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        e.setInputType(type);
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, values) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(TEXT);
                v.setTextSize(16);
                v.setPadding(dp(10), 0, dp(10), 0);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setTextColor(Color.BLACK);
                v.setTextSize(16);
                v.setPadding(dp(14), dp(12), dp(14), dp(12));
                return v;
            }
        };

        s.setAdapter(a);
        s.setBackground(roundRect(FIELD, BORDER, 10));
        return s;
    }

    private Button primaryButton(String label) {
        Button b = button(label);
        b.setTextColor(Color.rgb(8, 24, 30));
        b.setBackground(roundRect(ACCENT, ACCENT, 12));
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = button(label);
        b.setTextColor(TEXT);
        b.setBackground(roundRect(FIELD, BORDER, 12));
        return b;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        return b;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 17, TEXT, true);
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.setMargins(0, 0, 0, dp(6));
        t.setLayoutParams(p);
        return t;
    }

    private TextView valueText() {
        return text("", 13, MUTED, false);
    }

    private TextView bigValue() {
        TextView t = text("—", 28, ACCENT, true);
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.setMargins(0, dp(6), 0, 0);
        t.setLayoutParams(p);
        return t;
    }

    private TextView smallValue() {
        return text("", 12, MUTED, false);
    }

    private TextView bodyText() {
        TextView t = text("", 14, TEXT, false);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams halfLeft() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(0, 0, dp(5), 0);
        return p;
    }

    private LinearLayout.LayoutParams halfRight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(dp(5), 0, 0, 0);
        return p;
    }

    private LinearLayout.LayoutParams halfLeftHeight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.setMargins(0, 0, dp(5), 0);
        return p;
    }

    private LinearLayout.LayoutParams halfRightHeight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
        p.setMargins(dp(5), 0, 0, 0);
        return p;
    }

    private LinearLayout.LayoutParams third(int i) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(i == 0 ? 0 : dp(4), 0, i == 2 ? 0 : dp(4), 0);
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private abstract class SimpleSelection implements AdapterView.OnItemSelectedListener {
        public abstract void selected(int position);

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    static class MachineConfig {
        final String key, name, lowEf, highEf;
        final int[] gears;

        MachineConfig(String key, String name, String lowEf, String highEf, int[] gears) {
            this.key = key;
            this.name = name;
            this.lowEf = lowEf;
            this.highEf = highEf;
            this.gears = gears;
        }

        static MachineConfig m53() {
            return new MachineConfig(
                    "53a50n",
                    "53А50Н",
                    "54/54 = 1:1",
                    "36/72 = 1:2",
                    new int[]{
                            23,24,25,25,30,33,34,35,37,40,40,41,43,45,47,48,
                            50,53,55,58,59,60,61,62,65,66,67,70,70,71,73,75,
                            79,80,83,85,87,89,90,92,95,97,98,100
                    });
        }

        static MachineConfig m5e32() {
            return new MachineConfig(
                    "5e32",
                    "5Е32",
                    "36/36 = 1:1",
                    "24/48 = 1:2",
                    new int[]{
                            23,24,25,25,30,33,34,35,37,40,41,43,45,47,48,50,
                            53,55,57,58,59,60,61,62,65,67,70,71,73,75,79,80,
                            83,85,89,90,92,95,97,98,100
                    });
        }

        LinkedHashMap<Integer, Integer> maxCounts() {
            LinkedHashMap<Integer, Integer> m = new LinkedHashMap<>();
            for (int g : gears) {
                m.put(g, m.containsKey(g) ? m.get(g) + 1 : 1);
            }
            return m;
        }
    }

    static class Candidate {
        int a, b, c, d;
        boolean twoGear;
        double actual, relError;

        Candidate(int a, int b, int c, int d, boolean two, double target) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.twoGear = two;
            this.actual = two
                    ? ((double) a / b)
                    : ((double) a / b) * ((double) c / d);
            this.relError = Math.abs(actual - target) / Math.abs(target);
        }

        String expression() {
            return twoGear
                    ? a + " / " + b
                    : a + " / " + b + " × " + c + " / " + d;
        }
    }

    static class GearSolver {
        static List<Candidate> solve(double target, List<Integer> gears, int limit) {
            List<Candidate> best = new ArrayList<>();
            if (target <= 0 || gears.size() < 2) return best;

            for (int i = 0; i < gears.size(); i++) {
                for (int j = 0; j < gears.size(); j++) {
                    if (i == j) continue;
                    offer(best, new Candidate(
                            gears.get(i), gears.get(j), 0, 0, true, target), limit);
                }
            }

            for (int i = 0; i < gears.size(); i++) {
                for (int j = 0; j < gears.size(); j++) {
                    if (i == j) continue;

                    double first = (double) gears.get(i) / gears.get(j);

                    for (int k = 0; k < gears.size(); k++) {
                        if (k == i || k == j) continue;

                        double need = target / first;
                        int bestL = -1;
                        double bestErr = Double.MAX_VALUE;

                        for (int l = 0; l < gears.size(); l++) {
                            if (l == i || l == j || l == k) continue;
                            double r = (double) gears.get(k) / gears.get(l);
                            double e = Math.abs(r - need);
                            if (e < bestErr) {
                                bestErr = e;
                                bestL = l;
                            }
                        }

                        if (bestL >= 0) {
                            offer(best, new Candidate(
                                    gears.get(i), gears.get(j),
                                    gears.get(k), gears.get(bestL),
                                    false, target), limit);
                        }
                    }
                }
            }

            Collections.sort(best, cmp());
            return dedupe(best, limit);
        }

        static void offer(List<Candidate> list, Candidate c, int limit) {
            list.add(c);
            if (list.size() > limit * 10) {
                Collections.sort(list, cmp());
                while (list.size() > limit * 5) {
                    list.remove(list.size() - 1);
                }
            }
        }

        static Comparator<Candidate> cmp() {
            return (x, y) -> {
                int e = Double.compare(x.relError, y.relError);
                if (e != 0) return e;
                if (x.twoGear != y.twoGear) return x.twoGear ? -1 : 1;
                return Integer.compare(
                        x.a + x.b + x.c + x.d,
                        y.a + y.b + y.c + y.d);
            };
        }

        static List<Candidate> dedupe(List<Candidate> in, int limit) {
            List<Candidate> out = new ArrayList<>();
            for (Candidate c : in) {
                boolean same = false;
                for (Candidate o : out) {
                    if (c.expression().equals(o.expression())) {
                        same = true;
                        break;
                    }
                }
                if (!same) out.add(c);
                if (out.size() >= limit) break;
            }
            return out;
        }
    }

    class GearDiagramView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Candidate c;
        boolean differential;

        GearDiagramView(Activity a) {
            super(a);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setCandidate(Candidate c, boolean differential) {
            this.c = c;
            this.differential = differential;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.TRANSPARENT);

            int w = getWidth();
            int h = getHeight();

            if (c == null) {
                p.setColor(MUTED);
                p.setTextSize(dp(14));
                canvas.drawText("Нет схемы", dp(10), h / 2f, p);
                return;
            }

            p.setTypeface(Typeface.DEFAULT_BOLD);
            String[] labels = differential
                    ? new String[]{"a₁", "b₁", "c₁", "d₁"}
                    : new String[]{"A", "B", "C", "D"};

            float cy = h * 0.53f;

            if (c.twoGear) {
                drawGear(canvas, w * 0.32f, cy, c.a, labels[0]);
                drawGear(canvas, w * 0.68f, cy, c.b, labels[1]);
                drawMesh(canvas, w * 0.32f, w * 0.68f, cy);
                drawCaption(canvas, "ведущая", w * 0.32f, h - 8);
                drawCaption(canvas, "ведомая", w * 0.68f, h - 8);
            } else {
                float x1 = w * .18f;
                float x2 = w * .40f;
                float x3 = w * .60f;
                float x4 = w * .82f;

                drawGear(canvas, x1, cy, c.a, labels[0]);
                drawGear(canvas, x2, cy, c.b, labels[1]);
                drawGear(canvas, x3, cy, c.c, labels[2]);
                drawGear(canvas, x4, cy, c.d, labels[3]);

                drawMesh(canvas, x1, x2, cy);
                drawShaft(canvas, x2, x3, cy);
                drawMesh(canvas, x3, x4, cy);

                drawCaption(canvas, "ведущая", x1, h - 8);
                drawCaption(canvas, "один вал", (x2 + x3) / 2, h - 8);
                drawCaption(canvas, "ведомая", x4, h - 8);
            }

            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(dp(11));
            p.setColor(MUTED);
            canvas.drawText("Схема, не в масштабе", dp(4), dp(14), p);
        }

        void drawGear(Canvas canvas, float x, float y, int teeth, String label) {
            float r = dp(31) + Math.min(dp(12), teeth / 8f);

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(48, 66, 75));
            canvas.drawCircle(x, y, r, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setColor(ACCENT);
            canvas.drawCircle(x, y, r, p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(BG);
            canvas.drawCircle(x, y, dp(8), p);

            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(TEXT);
            p.setTextSize(dp(14));
            p.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(label + " " + teeth, x, y + dp(5), p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        void drawMesh(Canvas canvas, float x1, float x2, float y) {
            p.setColor(WARN);
            p.setStrokeWidth(dp(2));
            p.setStyle(Paint.Style.STROKE);

            Path path = new Path();
            path.moveTo(x1 + dp(34), y - dp(4));
            path.lineTo(x2 - dp(34), y + dp(4));
            canvas.drawPath(path, p);

            p.setStyle(Paint.Style.FILL);
        }

        void drawShaft(Canvas canvas, float x1, float x2, float y) {
            p.setColor(MUTED);
            p.setStrokeWidth(dp(5));
            canvas.drawLine(x1, y - dp(48), x2, y - dp(48), p);
            canvas.drawLine(x1, y - dp(48), x1, y - dp(35), p);
            canvas.drawLine(x2, y - dp(48), x2, y - dp(35), p);
        }

        void drawCaption(Canvas canvas, String s, float x, float y) {
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(10));
            p.setTypeface(Typeface.DEFAULT);
            p.setColor(MUTED);
            canvas.drawText(s, x, y, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }
}
