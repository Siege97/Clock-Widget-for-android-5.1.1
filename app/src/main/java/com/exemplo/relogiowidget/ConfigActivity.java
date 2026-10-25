package com.exemplo.relogiowidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lista os apps instalados para escolher qual abrir ao tocar no relógio.
 * Aparece ao adicionar o widget, ao tocar nele sem app escolhido e na tela de ajustes.
 */
public class ConfigActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private static class AppItem {
        final String pkg;
        final String label;
        final Drawable icon;

        AppItem(String pkg, String label, Drawable icon) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // Se a pessoa sair sem escolher, o Android cancela a adição do widget
        setResult(RESULT_CANCELED,
                new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));

        setTitle(R.string.escolha_app);

        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);

        final List<AppItem> items = new ArrayList<AppItem>();
        for (ResolveInfo info : found) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            items.add(new AppItem(pkg, info.loadLabel(pm).toString(), info.loadIcon(pm)));
        }
        Collections.sort(items, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                return String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label);
            }
        });

        ListView list = new ListView(this);
        list.setAdapter(new AppAdapter(items));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                choose(items.get(position));
            }
        });
        setContentView(list);
    }

    private void choose(AppItem item) {
        getSharedPreferences(ClockWidgetProvider.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(ClockWidgetProvider.KEY_APP, item.pkg)
                .putString(ClockWidgetProvider.KEY_ACTION, ClockWidgetProvider.TAP_APP)
                .apply();

        // Com tela de configuração, o próprio app precisa desenhar o widget novo pela primeira vez
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            ClockWidgetProvider.update(this, AppWidgetManager.getInstance(this), appWidgetId);
        }
        ClockWidgetProvider.updateAll(this);

        setResult(RESULT_OK,
                new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
        finish();
    }

    private class AppAdapter extends ArrayAdapter<AppItem> {
        AppAdapter(List<AppItem> items) {
            super(ConfigActivity.this, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = getLayoutInflater().inflate(R.layout.item_app, parent, false);
            }
            AppItem item = getItem(position);
            ((TextView) row.findViewById(R.id.label)).setText(item.label);
            ((ImageView) row.findViewById(R.id.icon)).setImageDrawable(item.icon);
            return row;
        }
    }
}
