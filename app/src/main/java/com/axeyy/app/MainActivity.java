package com.axeyy.app;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "axey_preferences";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_HIDDEN_APPS = "hidden_work_profile_apps";
    private static final int REQUEST_CREATE_WORK_PROFILE = 4101;
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

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_WORK_PROFILE) return;

        if (resultCode == RESULT_OK) {
            showMessage(
                "Wizard Android mengembalikan RESULT_OK untuk tahap awal provisioning. " +
                "Ini belum menjamin seluruh proses selesai. Setelah wizard menutup, buka ikon AXEY " +
                "yang memiliki tanda koper di Work Profile. Di sana status harus menunjukkan " +
                "PROFILE OWNER AKTIF. Jika belum, salin Diagnostik Perangkat untuk pemeriksaan."
            );
        } else {
            showMessage(
                "Pembuatan Work Profile dibatalkan atau prasyarat Android belum terpenuhi. " +
                "Android tidak mengonfirmasi tahap awal provisioning. Periksa pesan pada wizard, " +
                "lalu coba lagi hanya jika Android mengizinkannya."
            );
        }
    }

    private boolean isManagedProfileContext() {
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
        return userManager != null && userManager.isManagedProfile();
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
        boolean managedProfileContext = isManagedProfileContext();
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
            active ? "● WORK PROFILE AKTIF" : managedProfileContext ? "● BERJALAN DI WORK PROFILE" : "○ WORK PROFILE BELUM AKTIF",
            active
                ? "AXEY berjalan sebagai pengelola profil kerja Android. Aplikasi dan data kerja dipisahkan dari profil pribadi."
                : managedProfileContext
                    ? "AXEY berjalan di Work Profile Android, tetapi bukan Profile Owner. Kemampuan administrasi AXEY terbatas."
                    : "Buat profil kerja resmi Android untuk memisahkan aplikasi dan data kerja dari profil pribadi."
        );

        if (!active && managedProfileContext) {
            addSection(page, "STATUS PENGELOLA: TERBATAS",
                "Android mengidentifikasi sesi ini sebagai Work Profile, tetapi AXEY bukan Profile Owner. Gunakan pengaturan profil atau hubungi administrator yang mengelolanya. AXEY tidak akan mencoba membuat Work Profile bertingkat.");
            addButton(page, "TAMBAH AKUN GOOGLE KERJA", false, this::addGoogleWorkAccount);
            addButton(page, "PENGATURAN WORK PROFILE", true, this::openWorkProfileSettings);
        } else if (!active) {
            boolean managedUsersSupported = getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS);
            boolean provisioningAllowed = canProvisionManagedProfile();
            String provisioningMessage;
            if (provisioningAllowed) {
                provisioningMessage = "Android saat ini mengizinkan proses pembuatan Work Profile. Lanjutkan melalui wizard resmi Android.";
            } else if (dpm == null) {
                provisioningMessage = "Layanan DevicePolicyManager tidak tersedia. AXEY tidak dapat memulai provisioning pada sesi ini.";
            } else if (managedUsersSupported) {
                provisioningMessage = "Perangkat mendukung managed users, tetapi Android saat ini menolak provisioning. Periksa apakah sudah ada Work Profile atau pembatasan administrator.";
            } else {
                provisioningMessage = "Perangkat tidak melaporkan fitur managed users. AXEY tidak dapat memaksa pembuatan Work Profile.";
            }
            addSection(page,
                provisioningAllowed ? "STATUS SETUP: SIAP" : "STATUS SETUP: TERBATAS",
                provisioningMessage);
            addButton(page, "BUAT WORK PROFILE", true, this::createWorkProfile);
        } else {
            addButton(page, "TAMBAH AKUN GOOGLE KERJA", false, this::addGoogleWorkAccount);
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
            "\nTarget kompatibilitas aplikasi: Android 9 ke atas."
        );
        addSection(page, "CATATAN GOOGLE PLAY",
            "Tombol Play Store hanya membuka aplikasi jika sudah tersedia di profil kerja. Managed Google Play memerlukan penyiapan Android Enterprise oleh organisasi atau penyedia EMM."
        );
        addEnterpriseIntegrationSection(page);

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
        TextView hint = label("Daftar aplikasi di profil kerja. Aplikasi pengguna dapat disembunyikan dan dipulihkan tanpa menghapus datanya.", 12, textSecondary, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(5);
        card.addView(hint, hintParams);

        Button refresh = actionButton("SEGARKAN DAFTAR", false, this::render);
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(-1, dp(42));
        refreshParams.topMargin = dp(10);
        card.addView(refresh, refreshParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(textPrimary);
        search.setHintTextColor(textSecondary);
        search.setHint("Cari aplikasi kerja...");
        search.setPadding(dp(13), 0, dp(13), 0);
        search.setBackground(rounded(buttonSecondary, 12));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(46));
        searchParams.topMargin = dp(10);
        card.addView(search, searchParams);

        TextView noResults = label("Tidak ada aplikasi yang cocok dengan pencarian.", 12, textSecondary, false);
        noResults.setVisibility(View.GONE);
        LinearLayout.LayoutParams noResultsParams = new LinearLayout.LayoutParams(-1, -2);
        noResultsParams.topMargin = dp(10);
        card.addView(noResults, noResultsParams);

        List<View> appRows = new java.util.ArrayList<>();
        List<String> appNames = new java.util.ArrayList<>();

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
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(10), 0, dp(8));

            LinearLayout summary = new LinearLayout(this);
            summary.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(this);
            Drawable appIcon = info.loadIcon(pm);
            icon.setImageDrawable(appIcon);
            icon.setContentDescription(String.valueOf(info.loadLabel(pm)));
            summary.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

            LinearLayout appText = new LinearLayout(this);
            appText.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams appTextParams = new LinearLayout.LayoutParams(0, -2, 1);
            appTextParams.leftMargin = dp(12);
            TextView appName = label(String.valueOf(info.loadLabel(pm)), 14, textPrimary, true);
            appText.addView(appName);
            TextView packageLabel = label(packageName, 10, textSecondary, false);
            LinearLayout.LayoutParams packageParams = new LinearLayout.LayoutParams(-1, -2);
            packageParams.topMargin = dp(2);
            appText.addView(packageLabel, packageParams);
            summary.addView(appText, appTextParams);
            row.addView(summary);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
            actionsParams.topMargin = dp(8);
            row.addView(actions, actionsParams);

            Button detailsButton = actionButton("INFO", false, () -> {
                try {
                    Intent details = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    details.setData(Uri.parse("package:" + packageName));
                    startActivity(details);
                } catch (Exception e) {
                    showMessage("Info aplikasi tidak dapat dibuka dari profil ini.");
                }
            });
            detailsButton.setContentDescription("Info aplikasi " + String.valueOf(info.loadLabel(pm)));
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            infoParams.rightMargin = dp(6);
            actions.addView(detailsButton, infoParams);

            Button open = actionButton("BUKA", true, () -> {
                try { startActivity(launch); }
                catch (Exception e) { showMessage("Aplikasi tidak bisa dibuka dari profil ini."); }
            });
            open.setContentDescription("Buka aplikasi " + String.valueOf(info.loadLabel(pm)));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            boolean userInstalled = (info.activityInfo.applicationInfo.flags
                & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0;
            boolean protectedPackage =
                "com.android.settings".equals(packageName)
                || "com.android.systemui".equals(packageName)
                || "com.android.vending".equals(packageName)
                || "com.google.android.gms".equals(packageName)
                || "com.google.android.gsf".equals(packageName)
                || "com.android.packageinstaller".equals(packageName)
                || "com.google.android.packageinstaller".equals(packageName)
                || "com.android.permissioncontroller".equals(packageName)
                || "com.google.android.permissioncontroller".equals(packageName);
            if (userInstalled && !protectedPackage) {
                openParams.rightMargin = dp(6);
            }
            actions.addView(open, openParams);

            if (userInstalled && !protectedPackage) {
                Button hide = actionButton("SEMBUNYI", false,
                    () -> confirmHideWorkApp(packageName, String.valueOf(info.loadLabel(pm))));
                hide.setContentDescription("Sembunyikan aplikasi " + String.valueOf(info.loadLabel(pm)));
                actions.addView(hide, new LinearLayout.LayoutParams(0, dp(42), 1));

                Button reset = actionButton("RESET DATA", false,
                    () -> confirmResetWorkApp(packageName, String.valueOf(info.loadLabel(pm))));
                reset.setContentDescription("Reset data aplikasi " + String.valueOf(info.loadLabel(pm)));
                LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, dp(42));
                resetParams.topMargin = dp(7);
                row.addView(reset, resetParams);
            }

            card.addView(row);
            appRows.add(row);
            appNames.add((String.valueOf(info.loadLabel(pm)) + " " + packageName)
                .toLowerCase(java.util.Locale.ROOT));
            count++;
        }

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable value) {
                String query = value.toString().trim().toLowerCase(java.util.Locale.ROOT);
                int matches = 0;
                for (int i = 0; i < appRows.size(); i++) {
                    boolean match = appNames.get(i).contains(query);
                    appRows.get(i).setVisibility(match ? View.VISIBLE : View.GONE);
                    if (match) matches++;
                }
                noResults.setVisibility(!appRows.isEmpty() && matches == 0 ? View.VISIBLE : View.GONE);
            }
        });

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
        addHiddenWorkAppsSection(parent);
    }

    private void confirmResetWorkApp(String packageName, String appLabel) {
        if (dpm == null || !dpm.isProfileOwnerApp(getPackageName())) {
            showMessage("Reset data hanya tersedia ketika AXEY menjadi Profile Owner Work Profile ini.");
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Reset data " + appLabel + "?")
            .setMessage(
                "Semua data lokal aplikasi, termasuk cache, database, pengaturan, dan sesi login akan dihapus. " +
                "Aplikasi tetap terpasang. Akun Google pada Work Profile tidak dihapus, tetapi akun di dalam aplikasi ini mungkin perlu login ulang. " +
                "Data yang belum dicadangkan dapat hilang permanen."
            )
            .setNegativeButton("BATAL", null)
            .setPositiveButton("RESET DATA", (dialog, which) -> {
                try {
                    boolean accepted = dpm.clearApplicationUserData(
                        admin,
                        packageName,
                        getMainExecutor(),
                        (clearedPackage, succeeded) -> {
                            if (succeeded) {
                                showMessage("Data " + appLabel + " berhasil direset. Aplikasi tetap terpasang; buka kembali untuk memulai dari kondisi awal.");
                            } else {
                                showMessage("Android tidak berhasil menghapus data " + appLabel + ".");
                            }
                            render();
                        }
                    );
                    if (!accepted) {
                        showMessage("Android tidak menerima permintaan reset data untuk aplikasi ini.");
                    }
                } catch (SecurityException e) {
                    showMessage("AXEY tidak memiliki izin Profile Owner untuk mereset aplikasi ini.");
                } catch (RuntimeException e) {
                    showMessage("Reset data tidak dapat dimulai: " + e.getMessage());
                }
            })
            .show();
    }

    private void confirmHideWorkApp(String packageName, String appLabel) {
        if (dpm == null || !dpm.isProfileOwnerApp(getPackageName())) {
            showMessage("AXEY harus menjadi Profile Owner pada Work Profile ini untuk menyembunyikan aplikasi.");
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Sembunyikan aplikasi kerja?")
            .setMessage(appLabel + " (" + packageName + ") akan disembunyikan hanya dari Work Profile. Aplikasi dan datanya tidak dihapus. Lu dapat memulihkannya dari bagian aplikasi tersembunyi di AXEY.")
            .setNegativeButton("BATAL", null)
            .setPositiveButton("SEMBUNYIKAN", (dialog, which) -> setWorkAppHidden(packageName, true))
            .show();
    }

    private void setWorkAppHidden(String packageName, boolean hidden) {
        if (dpm == null || !dpm.isProfileOwnerApp(getPackageName())) {
            showMessage("AXEY bukan Profile Owner pada profil ini. Tidak ada perubahan yang dilakukan.");
            return;
        }
        try {
            boolean updated = dpm.setApplicationHidden(admin, packageName, hidden);
            if (!updated) {
                showMessage("Android tidak dapat memperbarui status aplikasi " + packageName + ".");
                return;
            }
            Set<String> hiddenApps = new HashSet<>(
                preferences.getStringSet(KEY_HIDDEN_APPS, Collections.emptySet()));
            if (hidden) hiddenApps.add(packageName);
            else hiddenApps.remove(packageName);
            preferences.edit().putStringSet(KEY_HIDDEN_APPS, hiddenApps).apply();
            Toast.makeText(this,
                hidden ? "Aplikasi disembunyikan dari Work Profile." : "Aplikasi dipulihkan di Work Profile.",
                Toast.LENGTH_SHORT).show();
            render();
        } catch (SecurityException e) {
            showMessage("Android menolak perubahan aplikasi. Pastikan AXEY masih menjadi Profile Owner.");
        } catch (RuntimeException e) {
            showMessage("Perubahan aplikasi gagal: " + e.getMessage());
        }
    }

    private Set<String> getActuallyHiddenWorkApps() {
        Set<String> hiddenApps = new HashSet<>(
            preferences.getStringSet(KEY_HIDDEN_APPS, Collections.emptySet()));
        if (dpm == null || !dpm.isProfileOwnerApp(getPackageName())) return hiddenApps;

        boolean changed = false;
        for (String packageName : new HashSet<>(hiddenApps)) {
            try {
                if (!dpm.isApplicationHidden(admin, packageName)) {
                    hiddenApps.remove(packageName);
                    changed = true;
                }
            } catch (SecurityException e) {
                // Keep the cached entry if Android temporarily rejects the policy query.
            }
        }
        if (changed) {
            preferences.edit().putStringSet(KEY_HIDDEN_APPS, hiddenApps).apply();
        }
        return hiddenApps;
    }

    private void addHiddenWorkAppsSection(LinearLayout parent) {
        Set<String> hiddenApps = getActuallyHiddenWorkApps();
        if (hiddenApps.isEmpty()) return;

        List<String> sortedPackages = new java.util.ArrayList<>(hiddenApps);
        Collections.sort(sortedPackages, String.CASE_INSENSITIVE_ORDER);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(surface, 20));
        card.addView(label("APLIKASI TERSEMBUNYI OLEH AXEY (" + sortedPackages.size() + ")", 15, textPrimary, true));
        TextView hint = label("Aplikasi tetap terpasang dan datanya tidak dihapus. Pulihkan kapan saja dari sini.", 12, textSecondary, false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(6);
        card.addView(hint, hintParams);

        for (String packageName : sortedPackages) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(9), 0, dp(2));
            TextView name = label(packageName, 12, textPrimary, false);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button restore = actionButton("PULIHKAN", true,
                () -> setWorkAppHidden(packageName, false));
            row.addView(restore, new LinearLayout.LayoutParams(dp(104), dp(42)));
            card.addView(row);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(14);
        parent.addView(card, cardParams);
    }

    private void addEnterpriseIntegrationSection(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(rounded(surface, 20));

        card.addView(label("JALUR INTEGRASI ENTERPRISE", 16, textPrimary, true));

        TextView status = label("STATUS: BELUM DIHUBUNGKAN", 12, accent, true);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(8);
        card.addView(status, statusParams);

        boolean managedUsersSupported = getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS);
        boolean localProfileOwner = dpm != null && dpm.isProfileOwnerApp(getPackageName());
        boolean managedProfileContext = isManagedProfileContext();

        TextView deviceCapability = label(
            "DUKUNGAN ANDROID: " + (managedUsersSupported
                ? "FITUR WORK PROFILE TERDETEKSI"
                : "FITUR WORK PROFILE TIDAK TERDETEKSI"),
            12, textPrimary, true);
        LinearLayout.LayoutParams capabilityParams = new LinearLayout.LayoutParams(-1, -2);
        capabilityParams.topMargin = dp(10);
        card.addView(deviceCapability, capabilityParams);

        TextView ownerStatus = label(
            "STATUS AXEY: " + (localProfileOwner
                ? "PROFILE OWNER AKTIF"
                : "BELUM MENJADI PROFILE OWNER"),
            12, textSecondary, true);
        LinearLayout.LayoutParams ownerParams = new LinearLayout.LayoutParams(-1, -2);
        ownerParams.topMargin = dp(6);
        card.addView(ownerStatus, ownerParams);

        TextView profileContextStatus = label(
            "KONTEKS ANDROID: " + (managedProfileContext ? "WORK PROFILE" : "BUKAN WORK PROFILE"),
            12, textSecondary, true);
        LinearLayout.LayoutParams profileContextParams = new LinearLayout.LayoutParams(-1, -2);
        profileContextParams.topMargin = dp(6);
        card.addView(profileContextStatus, profileContextParams);

        TextView detail = label(
            "Deteksi fitur managed users hanya menunjukkan dukungan sistem, bukan pendaftaran enterprise. Work Profile AXEY saat ini dikelola secara lokal dan belum terhubung ke organisasi atau Managed Google Play. Akun Google pribadi tidak otomatis menjadi akun kerja terkelola. Android Management API memerlukan enrollment token dan Android Device Policy; AxeyyAdminReceiver saat ini bukan DPC untuk jalur tersebut.",
            13, textSecondary, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(7);
        card.addView(detail, detailParams);

        Button checklist = actionButton("SALIN CHECKLIST ENTERPRISE", false, this::copyEnterpriseChecklist);
        LinearLayout.LayoutParams checklistParams = new LinearLayout.LayoutParams(-1, dp(46));
        checklistParams.topMargin = dp(12);
        card.addView(checklist, checklistParams);

        Button guide = actionButton("BUKA PANDUAN RESMI", false, this::openEnterpriseGuide);
        LinearLayout.LayoutParams guideParams = new LinearLayout.LayoutParams(-1, dp(46));
        guideParams.topMargin = dp(10);
        card.addView(guide, guideParams);

        Button diagnostics = actionButton("SALIN DIAGNOSTIK PERANGKAT", false, this::copyDeviceDiagnostics);
        LinearLayout.LayoutParams diagnosticsParams = new LinearLayout.LayoutParams(-1, dp(46));
        diagnosticsParams.topMargin = dp(10);
        card.addView(diagnostics, diagnosticsParams);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(14);
        parent.addView(card, cardParams);
    }

    private void copyEnterpriseChecklist() {
        String checklist =
            "AXEY Work - Android Enterprise readiness\n" +
            "Current status: not connected to an enterprise or Managed Google Play.\n\n" +
            "Required setup:\n" +
            "1. Create a Google Cloud project and enable the Android Management API.\n" +
            "2. Complete the Android Enterprise enterprise-binding flow for the organization.\n" +
            "3. Run an EMM backend that securely authenticates to Google APIs and creates policies and enrollment tokens.\n" +
            "4. Enroll the device through Android Device Policy using a backend-generated enrollment token.\n" +
            "5. Configure managed Google Play apps in the enterprise policy.\n\n" +
            "Important: Android Management API supports Android Device Policy as its DPC. AXEY's current AxeyyAdminReceiver is a separate local DPC and cannot act as the Android Management API DPC. A separate supported enrollment flow is required.\n" +
            "Do not place service-account keys, passwords, or enrollment tokens in the APK.\n" +
            "This report contains no account credentials or tokens.";

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            showMessage("Clipboard tidak tersedia di perangkat ini.");
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("AXEY Enterprise checklist", checklist));
        Toast.makeText(this, "Checklist enterprise disalin.", Toast.LENGTH_SHORT).show();
    }

    private String getAppVersionLabel() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = info.versionName == null ? "unknown" : info.versionName;
            return versionName + " (code " + info.getLongVersionCode() + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "TIDAK TERSEDIA";
        }
    }

    private void copyDeviceDiagnostics() {
        boolean managedUsersSupported = getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS);
        boolean policyManagerAvailable = dpm != null;
        boolean localProfileOwner = policyManagerAvailable
            && dpm.isProfileOwnerApp(getPackageName());
        boolean managedProfileContext = isManagedProfileContext();

        String provisioningState;
        if (localProfileOwner) {
            provisioningState = "SUDAH PROFILE OWNER (PROVISIONING BARU TIDAK DIPERLUKAN)";
        } else if (!managedUsersSupported) {
            provisioningState = "TIDAK DIDUKUNG OLEH SISTEM";
        } else if (!policyManagerAvailable) {
            provisioningState = "LAYANAN TIDAK TERSEDIA";
        } else {
            try {
                provisioningState = dpm.isProvisioningAllowed(
                    DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
                    ? "DIIZINKAN" : "DIBLOKIR";
            } catch (RuntimeException e) {
                provisioningState = "TIDAK DAPAT DIPERIKSA";
            }
        }

        String report =
            "AXEY Work - Diagnostik perangkat\n" +
            "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
            "Konteks Android: " + (managedProfileContext ? "WORK PROFILE" : "BUKAN WORK PROFILE") + "\n" +
            "Versi AXEY: " + getAppVersionLabel() + "\n" +
            "Kompatibilitas minimum aplikasi: Android 9 (API 28)\n" +
            "Fitur managed users: " + (managedUsersSupported ? "TERDETEKSI" : "TIDAK TERDETEKSI") + "\n" +
            "DevicePolicyManager: " + (policyManagerAvailable ? "TERSEDIA" : "TIDAK TERSEDIA") + "\n" +
            "AXEY Profile Owner pada profil saat ini: " + (localProfileOwner ? "YA" : "TIDAK") + "\n" +
            "Provisioning Work Profile baru: " + provisioningState + "\n" +
            "Managed Google Play: BELUM TERHUBUNG\n\n" +
            "Catatan: status provisioning hanya menggambarkan izin membuat profil baru saat ini; bukan bukti bahwa profil kerja yang sudah ada bermasalah. " +
            "Managed Google Play memerlukan pendaftaran Android Enterprise dan backend EMM.\n" +
            "Privasi: laporan ini tidak menyertakan email, ID akun, password, atau token.";

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            showMessage("Clipboard tidak tersedia di perangkat ini.");
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("AXEY device diagnostics", report));
        Toast.makeText(this, "Diagnostik perangkat disalin tanpa data akun atau rahasia.", Toast.LENGTH_SHORT).show();
    }

    private void openEnterpriseGuide() {
        try {
            Intent guide = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://developers.google.com/android/management/introduction"));
            startActivity(guide);
        } catch (Exception e) {
            showMessage("Panduan Android Enterprise tidak dapat dibuka. Periksa browser dan koneksi internet.");
        }
    }

    private void toggleTheme() {
        dark = !dark;
        preferences.edit().putBoolean(KEY_DARK, dark).apply();
        applyPalette();
        render();
    }

    private boolean canProvisionManagedProfile() {
        return dpm != null
            && getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
            && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
    }

    private void createWorkProfile() {
        if (dpm == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)) {
            showMessage("Perangkat ini tidak melaporkan dukungan Work Profile Android. AXEY memerlukan fitur managed users dari sistem.");
            return;
        }
        if (!canProvisionManagedProfile()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Work Profile belum bisa dibuat")
                .setMessage("Android sedang menolak proses pembuatan profil. Pastikan tidak ada profil kerja lain, akun/perangkat tidak dikelola organisasi lain, dan fitur Work Profile didukung oleh sistem.")
                .setNegativeButton("NANTI", null)
                .setPositiveButton("PENGATURAN", (dialog, which) -> {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); }
                    catch (Exception e) { showMessage("Pengaturan Android tidak dapat dibuka."); }
                })
                .show();
            return;
        }
        Intent setup = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        setup.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            admin.flattenToString()
        );
        if (setup.resolveActivity(getPackageManager()) == null) {
            showMessage("Wizard resmi Android untuk membuat Work Profile tidak tersedia di perangkat ini.");
            return;
        }
        try {
            startActivityForResult(setup, REQUEST_CREATE_WORK_PROFILE);
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

    private void addGoogleWorkAccount() {
        Intent addAccount = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
        addAccount.putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
        try {
            startActivity(addAccount);
        } catch (Exception e) {
            new android.app.AlertDialog.Builder(this)
                .setMessage("Layar tambah akun Google tidak tersedia. Buka Pengaturan Work Profile, lalu pilih Akun > Tambahkan akun > Google. Jika penambahan akun dibatasi administrator, hubungi pengelola perangkat.")
                .setNegativeButton("NANTI", null)
                .setPositiveButton("BUKA PENGATURAN", (dialog, which) -> openWorkProfileSettings())
                .show();
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
