package com.exemplo.relogiowidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Tela do ícone do app: o que o toque faz, qual app abrir, rotação, fonte, cor,
 * opacidade e posição do relógio. A faixa escura no topo mostra uma prévia.
 */
public class SettingsActivity extends Activity {

    private static final int REQ_PICK = 1;
    private static final int LIMIT = 5000;   // limite de segurança do deslocamento

    private TextView preview;
    private RadioGroup tapGroup;
    private RadioGroup rotationGroup;
    private RadioGroup fontGroup;
    private Button appButton;
    private Button customButton;
    private EditText offsetX;
    private EditText offsetY;
    private EditText hexField;
    private EditText opacityField;
    private SeekBar opacitySeek;

    private final View[] swatches = new View[ClockOptions.COLORS.length];
    private final int[] fontButtonIds = new int[ClockOptions.FONT_KEYS.length];
    private int selectedRgb = ClockOptions.DEFAULT_COLOR;
    private boolean customSelected = false;
    private boolean updatingOpacity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preview = (TextView) findViewById(R.id.preview);
        tapGroup = (RadioGroup) findViewById(R.id.tap_group);
        rotationGroup = (RadioGroup) findViewById(R.id.rotation_group);
        fontGroup = (RadioGroup) findViewById(R.id.font_group);
        appButton = (Button) findViewById(R.id.btn_app);
        customButton = (Button) findViewById(R.id.btn_custom);
        offsetX = (EditText) findViewById(R.id.offset_x);
        offsetY = (EditText) findViewById(R.id.offset_y);
        hexField = (EditText) findViewById(R.id.hex_field);
        opacityField = (EditText) findViewById(R.id.opacity_field);
        opacitySeek = (SeekBar) findViewById(R.id.opacity_seek);

        SharedPreferences prefs = prefs();
        offsetX.setText(String.valueOf(prefs.getInt(ClockWidgetProvider.KEY_OFFSET_X, 0)));
        offsetY.setText(String.valueOf(prefs.getInt(ClockWidgetProvider.KEY_OFFSET_Y, 0)));
        showCurrentAction();
        showCurrentApp();
        showCurrentRotation(prefs.getInt(ClockWidgetProvider.KEY_ROTATION, ClockOptions.DEFAULT_ROTATION));
        buildFonts(prefs.getString(ClockWidgetProvider.KEY_FONT, ClockOptions.FONT_KEYS[0]));
        buildColors(prefs.getInt(ClockWidgetProvider.KEY_COLOR, ClockOptions.DEFAULT_COLOR));
        setupOpacity(prefs.getInt(ClockWidgetProvider.KEY_OPACITY, ClockOptions.DEFAULT_OPACITY));
        updatePreview();

        appButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(SettingsActivity.this, ConfigActivity.class), REQ_PICK);
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK) {
            // Escolher um app já salva e marca "Abrir um app"
            showCurrentAction();
            showCurrentApp();
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(ClockWidgetProvider.PREFS, Context.MODE_PRIVATE);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------------- Toque e app ----------------

    private void showCurrentAction() {
        String action = prefs().getString(ClockWidgetProvider.KEY_ACTION, ClockWidgetProvider.TAP_APP);
        if (ClockWidgetProvider.TAP_NONE.equals(action)) {
            tapGroup.check(R.id.rb_none);
        } else if (ClockWidgetProvider.TAP_AWAKE.equals(action)) {
            tapGroup.check(R.id.rb_awake);
        } else {
            tapGroup.check(R.id.rb_app);
        }
    }

    private void showCurrentApp() {
        String pkg = prefs().getString(ClockWidgetProvider.KEY_APP, null);
        CharSequence label = null;
        if (pkg != null) {
            PackageManager pm = getPackageManager();
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
            } catch (PackageManager.NameNotFoundException e) {
                label = null;
            }
        }
        appButton.setText(label != null ? label : getString(R.string.escolher_app));
    }

    // ---------------- Rotação ----------------

    private void showCurrentRotation(int saved) {
        int rotation = ClockOptions.rotationOrDefault(saved);
        if (rotation == 0) {
            rotationGroup.check(R.id.rb_rot_0);
        } else if (rotation == 180) {
            rotationGroup.check(R.id.rb_rot_180);
        } else if (rotation == 270) {
            rotationGroup.check(R.id.rb_rot_270);
        } else {
            rotationGroup.check(R.id.rb_rot_90);
        }
    }

    private int selectedRotation() {
        int checked = rotationGroup.getCheckedRadioButtonId();
        if (checked == R.id.rb_rot_0) return 0;
        if (checked == R.id.rb_rot_180) return 180;
        if (checked == R.id.rb_rot_270) return 270;
        return 90;
    }

    // ---------------- Fonte ----------------

    private void buildFonts(String savedKey) {
        int selected = ClockOptions.fontIndex(savedKey);
        for (int i = 0; i < ClockOptions.FONT_KEYS.length; i++) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            fontButtonIds[i] = button.getId();
            // Cada item aparece escrito na própria fonte, como prévia
            button.setText(getString(ClockOptions.FONT_LABELS[i]) + "   12:34");
            button.setTypeface(ClockOptions.typeface(i));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            fontGroup.addView(button, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        fontGroup.check(fontButtonIds[selected]);
        fontGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updatePreview();
            }
        });
    }

    private int selectedFontIndex() {
        int checked = fontGroup.getCheckedRadioButtonId();
        for (int i = 0; i < fontButtonIds.length; i++) {
            if (fontButtonIds[i] == checked) return i;
        }
        return 0;
    }

    // ---------------- Cor ----------------

    private void buildColors(int savedRgb) {
        LinearLayout row1 = (LinearLayout) findViewById(R.id.color_row1);
        LinearLayout row2 = (LinearLayout) findViewById(R.id.color_row2);
        for (int i = 0; i < ClockOptions.COLORS.length; i++) {
            View swatch = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            swatch.setLayoutParams(lp);
            swatch.setContentDescription(getString(ClockOptions.COLOR_NAMES[i]));
            final int index = i;
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedRgb = ClockOptions.COLORS[index];
                    customSelected = false;
                    refreshColorSelection();
                    updatePreview();
                }
            });
            (i < 5 ? row1 : row2).addView(swatch);
            swatches[i] = swatch;
        }

        selectedRgb = savedRgb & 0xFFFFFF;
        customSelected = presetIndex(selectedRgb) < 0;
        if (customSelected) {
            hexField.setText(toHex(selectedRgb));
        }

        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customSelected = true;
                int typed = parseHex(hexField.getText().toString());
                if (typed < 0) {
                    hexField.setText(toHex(selectedRgb));
                } else {
                    selectedRgb = typed;
                }
                refreshColorSelection();
                updatePreview();
            }
        });
        hexField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!customSelected) return;
                int typed = parseHex(s.toString());
                if (typed >= 0) {
                    selectedRgb = typed;
                    updatePreview();
                }
            }
        });
        refreshColorSelection();
    }

    /** Destaca o quadradinho escolhido e mostra o campo HEX só em "Personalizar". */
    private void refreshColorSelection() {
        int selectedPreset = customSelected ? -1 : presetIndex(selectedRgb);
        for (int i = 0; i < swatches.length; i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(0xFF000000 | ClockOptions.COLORS[i]);
            shape.setCornerRadius(dp(8));
            if (i == selectedPreset) {
                shape.setStroke(dp(3), 0xFF000000);
            } else {
                shape.setStroke(dp(1), 0xFF9E9E9E);
            }
            swatches[i].setBackground(shape);
        }
        String label = getString(R.string.personalizar);
        customButton.setText(customSelected ? "✓ " + label : label);
        hexField.setVisibility(customSelected ? View.VISIBLE : View.GONE);
    }

    private static int presetIndex(int rgb) {
        for (int i = 0; i < ClockOptions.COLORS.length; i++) {
            if (ClockOptions.COLORS[i] == (rgb & 0xFFFFFF)) return i;
        }
        return -1;
    }

    private static String toHex(int rgb) {
        return String.format(Locale.US, "#%06X", rgb & 0xFFFFFF);
    }

    /** Lê "#RRGGBB" ou "RRGGBB". Devolve -1 se não for um código de 6 dígitos válido. */
    private static int parseHex(String text) {
        String s = text.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.matches("[0-9a-fA-F]{6}")) {
            return -1;
        }
        return Integer.parseInt(s, 16);
    }

    // ---------------- Opacidade ----------------

    private void setupOpacity(int saved) {
        opacitySeek.setMax(100 - ClockOptions.MIN_OPACITY);
        setOpacity(ClockOptions.clampOpacity(saved));

        opacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                updatingOpacity = true;
                opacityField.setText(String.valueOf(progress + ClockOptions.MIN_OPACITY));
                updatingOpacity = false;
                updatePreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        opacityField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingOpacity) return;
                // Não reescreve o campo enquanto a pessoa digita; só move o controle e a prévia
                opacitySeek.setProgress(currentOpacity() - ClockOptions.MIN_OPACITY);
                updatePreview();
            }
        });
    }

    private void setOpacity(int percent) {
        updatingOpacity = true;
        opacitySeek.setProgress(percent - ClockOptions.MIN_OPACITY);
        opacityField.setText(String.valueOf(percent));
        updatingOpacity = false;
    }

    /** Opacidade atual (10 a 100). Campo vazio ou inválido usa o valor do controle. */
    private int currentOpacity() {
        try {
            return ClockOptions.clampOpacity(Integer.parseInt(opacityField.getText().toString().trim()));
        } catch (NumberFormatException e) {
            return opacitySeek.getProgress() + ClockOptions.MIN_OPACITY;
        }
    }

    // ---------------- Prévia e salvar ----------------

    private void updatePreview() {
        preview.setTypeface(ClockOptions.typeface(selectedFontIndex()));
        preview.setTextColor(ClockOptions.argb(selectedRgb, currentOpacity()));
    }

    private void save() {
        // Valida o HEX antes de salvar qualquer coisa
        int rgb = selectedRgb;
        if (customSelected) {
            int typed = parseHex(hexField.getText().toString());
            if (typed < 0) {
                Toast.makeText(this, R.string.hex_invalido, Toast.LENGTH_LONG).show();
                return;
            }
            rgb = typed;
        }

        String action;
        int checked = tapGroup.getCheckedRadioButtonId();
        if (checked == R.id.rb_none) {
            action = ClockWidgetProvider.TAP_NONE;
        } else if (checked == R.id.rb_awake) {
            action = ClockWidgetProvider.TAP_AWAKE;
        } else {
            action = ClockWidgetProvider.TAP_APP;
        }
        int opacity = currentOpacity();

        prefs().edit()
                .putString(ClockWidgetProvider.KEY_ACTION, action)
                .putInt(ClockWidgetProvider.KEY_OFFSET_X, parse(offsetX))
                .putInt(ClockWidgetProvider.KEY_OFFSET_Y, parse(offsetY))
                .putInt(ClockWidgetProvider.KEY_ROTATION, selectedRotation())
                .putString(ClockWidgetProvider.KEY_FONT, ClockOptions.FONT_KEYS[selectedFontIndex()])
                .putInt(ClockWidgetProvider.KEY_COLOR, rgb)
                .putInt(ClockWidgetProvider.KEY_OPACITY, opacity)
                .apply();

        // Saiu do modo "tela ligada": desliga a trava para não ficar presa sem como desligar
        if (!ClockWidgetProvider.TAP_AWAKE.equals(action) && KeepAwakeService.isRunning()) {
            stopService(new Intent(this, KeepAwakeService.class));
        }

        setOpacity(opacity);   // mostra o valor já ajustado aos limites
        ClockWidgetProvider.updateAll(this);
        Toast.makeText(this, R.string.salvo, Toast.LENGTH_SHORT).show();
    }

    /** Lê um número inteiro (aceita negativo). Vazio ou inválido vira 0. */
    private static int parse(EditText field) {
        String s = field.getText().toString().trim();
        try {
            int value = Integer.parseInt(s);
            return Math.max(-LIMIT, Math.min(LIMIT, value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
