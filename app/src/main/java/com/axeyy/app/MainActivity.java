package com.axeyy.app;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "axey_preferences";
    private static final String KEY_DARK = "dark_mode";
    private DevicePolicyManager dpm;
    private ComponentName admin;
    private SharedPreferences preferences;
    private boolean dark;
    private int background, surface, textPrimary, textSecondary, accent, buttonSecondary;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AxeyyAdminReceiver.class);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        dark = preferences.getBoolean(KEY_DARK, true);
        applyPalette();
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dpm != null) render();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void applyPalette() {
        if (dark) {
            background = 0xff080d17;
            surface = 0xff121d2d;
            textPrimary = 0xfff5f8ff;
            textSecondary = 0xffa9bad0;
            accent = 0xff2878e8;
            buttonSecondary = 0xff24344a;
        } else {
            background = 0xfff4f7fb;
            surface = 0xffffffff;
            textPrimary = 0xff111827;
            textSecondary = 0xff536174;
            accent = 0xff1769d2;
            buttonSecondary = 0xffe2eaf5;
        }
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
            dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button actionButton(String title, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(title);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary || dark ? Color.WHITE : textPrimary);
        button.setBackground(rounded(primary ? accent : buttonSecondary, 16));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void addButton(LinearLayout parent, String title, boolean primary, Runnable action) {
        Button button = actionButton(title, primary, action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        params.topMargin = dp(10);
        parent.addView(button, params);
    }

    private void addSection(LinearLayout parent, String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(surface, 20));
        TextView heading = label(title, 16, textPrimary, true);
        card.addView(heading);
        TextView detail = label(description, 13, textSecondary, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(7);
        card.addView(detail, detailParams);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(14);
        parent.addView(card, cardParams);
    }

    private void render() {
        boolean active = dpm != null && dpm.isProfileOwnerApp(getPackageName());
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(26), dp(22), dp(28));
        scroll.addView(page);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = label("AXEY", 31, textPrimary, true);
        logo.setLetterSpacing(.12f);
        header.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        Button theme = actionButton(dark ? "☀ Terang" : "☾ Gelap", false, this::toggleTheme);
        header.addView(theme, new LinearLayout.LayoutParams(-2, dp(44)));
        page.addView(header);

        TextView subtitle = label("WORKSPACE MANAGER", 11, accent, true);
        subtitle.setLetterSpacing(.14f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(3);
        page.addView(subtitle, subtitleParams);

        addSection(page,
            active ? "● WORK PROFILE AKTIF" : "○ WORK PROFILE BELUM AKTIF",
            active
                ? "AXEY berjalan di profil kerja Android. Aplikasi kerja dikelola terpisah dari profil pribadi."
                : "Buat profil kerja resmi Android untuk memisahkan aplikasi dan data kerja dari profil pribadi."
        );

        if (!active) {
            addButton(page, "BUAT WORK PROFILE", true, this::createWorkProfile);
        } else {
            addButton(page, "BUKA GOOGLE PLAY STORE KERJA", true,
                () -> launchWorkApp("com.android.vending",
                    "Play Store belum terpasang atau belum tersedia di profil kerja."));
            addButton(page, "BUKA GMAIL KERJA", false,
                () -> launchWorkApp("com.google.android.gm",
                    "Gmail belum terpasang. Jika Play Store kerja tersedia, pasang Gmail dari sana."));
            addButton(page, "PENGATURAN WORK PROFILE", false, this::openWorkProfileSettings);
            addButton(page, "KELOLA APLIKASI KERJA", false, this::openAppManagementSettings);
            addInstalledAppsSection(page);
        }

        addSection(page, "KEAMANAN & PEMISAHAN",
            "AXEY memakai fitur Work Profile bawaan Android. Data profil kerja tetap dipisahkan oleh sistem Android."
        );
        addSection(page, "INFO PERANGKAT",
            "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") • " +
            (active ? "AXEY terdaftar sebagai pengelola profil kerja." : "Profil kerja belum dibuat.") +
            "\\nTarget kompatibilitas aplikasi: Android 9 ke atas."
        );
        addSection(page, "CATATAN GOOGLE PLAY",
            "Tombol Play Store hanya membuka aplikasi jika sudah tersedia di profil kerja. Managed Google Play memerlukan penyiapan Android Enterprise oleh organisasi atau penyedia EMM."
        );

        TextView footer = label("AXEY Work • Android 9+", 11, textSecondary, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(22);
        page.addView(footer, footerParams);

        setContentView(scroll);
    }


    private void addInstalledAppsSection(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(surface, 20));

        TextView heading = label("APLIKASI DI WORK PROFILE", 16, textPrimary, true);
        card.addView(heading);
        TextView hint = label("Daftar aplikasi yang terpasang di profil kerja ini.", 12, textSecondary, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(5);
        card.addView(hint, hintParams);

        Button refresh = actionButton("SEGARKAN DAFTAR", false, this::render);
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(-1, dp(42));
        refreshParams.topMargin = dp(10);
        card.addView(refresh, refreshParams);

        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolved = pm.queryIntentActivities(launcher, 0);
        Collections.sort(resolved, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
            String.valueOf(a.loadLabel(pm)), String.valueOf(b.loadLabel(pm))));

        Set<String> seen = new HashSet<>();
        int count = 0;
        for (ResolveInfo info : resolved) {
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName) || !seen.add(packageName)) continue;
            Intent launch = pm.getLaunchIntentForPackage(packageName);
            if (launch == null) continue;

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(2));

            ImageView icon = new ImageView(this);
            Drawable appIcon = info.loadIcon(pm);
            icon.setImageDrawable(appIcon);
            icon.setContentDescription(String.valueOf(info.loadLabel(pm)));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(38), dp(38));
            row.addView(icon, iconParams);

            TextView appName = label(String.valueOf(info.loadLabel(pm)), 14, textPrimary, true);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
            nameParams.leftMargin = dp(12);
            row.addView(appName, nameParams);

            Button detailsButton = actionButton("INFO", false, () -> {
                try {
                    Intent details = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    details.setData(Uri.parse("package:" + packageName));
                    startActivity(details);
                } catch (Exception e) {
                    showMessage("Info aplikasi tidak dapat dibuka dari profil ini.");
                }
            });
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(dp(66), dp(42));
            infoParams.rightMargin = dp(6);
            detailsButton.setContentDescription("Info aplikasi " + String.valueOf(info.loadLabel(pm)));
            row.addView(detailsButton, infoParams);

            Button open = actionButton("BUKA", true, () -> {
                try { startActivity(launch); }
                catch (Exception e) { showMessage("Aplikasi tidak bisa dibuka dari profil ini."); }
            });
            open.setContentDescription("Buka aplikasi " + String.valueOf(info.loadLabel(pm)));
            row.addView(open, new LinearLayout.LayoutParams(dp(76), dp(42)));
            card.addView(row);
            count++;
        }

        heading.setText("APLIKASI DI WORK PROFILE (" + count + ")");

        if (count == 0) {
            TextView empty = label("Belum ada aplikasi lain yang terdeteksi di profil kerja.", 13, textSecondary, false);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(-1, -2);
            emptyParams.topMargin = dp(12);
            card.addView(empty, emptyParams);
            Button manage = actionButton("CARI / KELOLA APLIKASI", false, this::openAppManagementSettings);
            LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(-1, dp(44));
            manageParams.topMargin = dp(10);
            card.addView(manage, manageParams);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(14);
        parent.addView(card, cardParams);
    }

    private void toggleTheme() {
        dark = !dark;
        preferences.edit().putBoolean(KEY_DARK, dark).apply();
        applyPalette();
        render();
    }

    private void createWorkProfile() {
        if (dpm == null || !dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            showMessage("Android belum mengizinkan pembuatan Work Profile. Periksa apakah profil kerja lain sudah aktif atau perangkat membatasi fitur ini.");
            return;
        }
        Intent setup = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        setup.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin);
        try {
            startActivity(setup);
        } catch (Exception e) {
            showMessage("Tidak bisa membuka proses Work Profile: " + e.getMessage());
        }
    }

    private void launchWorkApp(String packageName, String error) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) {
                showMessage(error);
                return;
            }
            startActivity(launch);
        } catch (Exception e) {
            showMessage(error + "\n" + e.getMessage());
        }
    }

    private void openWorkProfileSettings() {
        try {
            Intent settings = new Intent("android.settings.MANAGED_PROFILE_SETTINGS");
            if (settings.resolveActivity(getPackageManager()) == null) {
                settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
            }
            startActivity(settings);
        } catch (Exception e) {
            showMessage("Pengaturan profil kerja tidak dapat dibuka di perangkat ini.");
        }
    }

    private void openAppManagementSettings() {
        try {
            Intent settings = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            if (settings.resolveActivity(getPackageManager()) == null) {
                settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
            }
            startActivity(settings);
        } catch (Exception e) {
            showMessage("Pengelolaan aplikasi tidak dapat dibuka di perangkat ini.");
        }
    }

    private void showMessage(String message) {
        new android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
}
