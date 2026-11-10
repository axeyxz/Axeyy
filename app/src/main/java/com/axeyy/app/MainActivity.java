package com.axeyy.app;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int bg = 0xff080d17;
    private final int panel = 0xff121d2d;
    private final int blue = 0xff2878e8;
    private DevicePolicyManager dpm;
    private ComponentName admin;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AxeyyAdminReceiver.class);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dpm != null) render();
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button makeButton(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(primary ? blue : 0xff24344a, 18));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void render() {
        boolean inWorkProfile = dpm.isProfileOwnerApp(getPackageName());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(26), dp(40), dp(26), dp(30));
        page.setBackgroundColor(bg);

        TextView logo = text("AXEY", 36, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(.12f);
        page.addView(logo, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView tagline = text("YOUR WORKSPACE", 12, 0xff78b7ff, true);
        tagline.setGravity(Gravity.CENTER);
        page.addView(tagline);

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(1, dp(38)));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(20), dp(20), dp(20), dp(20));
        status.setBackground(rounded(panel, 22));
        TextView statusTitle = text(
            inWorkProfile ? "WORKSPACE AKTIF" : "WORKSPACE ANDROID",
            17, inWorkProfile ? 0xff50dfa8 : Color.WHITE, true);
        status.addView(statusTitle);
        TextView statusDesc = text(
            inWorkProfile
                ? "Profil kerja AXEY terdeteksi. Buka Play Store kerja untuk memasang aplikasi."
                : "Siapkan profil kerja resmi Android. Setelah aktif, AXEY dapat membuka aplikasi di profil kerja.",
            13, 0xffa9bad0, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(8);
        status.addView(statusDesc, descParams);
        page.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button setup = makeButton(
            inWorkProfile ? "STATUS PROFIL KERJA AKTIF" : "BUAT PROFIL KERJA",
            true,
            () -> { if (!inWorkProfile) createWorkProfile(); }
        );
        setup.setEnabled(!inWorkProfile);
        LinearLayout.LayoutParams setupParams = new LinearLayout.LayoutParams(-1, dp(56));
        setupParams.topMargin = dp(20);
        page.addView(setup, setupParams);

        if (inWorkProfile) {
            Button play = makeButton("BUKA GOOGLE PLAY STORE", true, this::openWorkPlayStore);
            LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(-1, dp(56));
            playParams.topMargin = dp(12);
            page.addView(play, playParams);

            Button gmail = makeButton("BUKA GMAIL KERJA", false, this::openWorkGmail);
            LinearLayout.LayoutParams gmailParams = new LinearLayout.LayoutParams(-1, dp(56));
            gmailParams.topMargin = dp(10);
            page.addView(gmail, gmailParams);
        }

        TextView footer = text(
            inWorkProfile
                ? "Akun Google pribadi tetap memerlukan autentikasi resmi. Managed Play memerlukan pendaftaran Android Enterprise."
                : "AXEY menggunakan mekanisme Work Profile bawaan Android.",
            11, 0xff71839b, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(18);
        page.addView(footer, footerParams);

        setContentView(page);
    }

    private void createWorkProfile() {
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            showMessage("Android belum mengizinkan pembuatan profil kerja. Periksa apakah profil kerja lain sudah aktif atau perangkat membatasi fitur ini.");
            return;
        }
        Intent setup = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        setup.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin);
        try {
            startActivity(setup);
        } catch (Exception e) {
            showMessage("Tidak bisa membuka pembuatan profil kerja: " + e.getMessage());
        }
    }

    private void openWorkPlayStore() {
        launchWorkApp("com.android.vending", "Play Store belum tersedia di profil kerja. Perangkat harus mendukung Google Play Services dan Play Store.");
    }

    private void openWorkGmail() {
        launchWorkApp("com.google.android.gm", "Gmail belum tersedia di profil kerja. Pasang melalui Play Store kerja jika tersedia.");
    }

    private void launchWorkApp(String packageName, String errorMessage) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            try {
                startActivity(launch);
                return;
            } catch (Exception ignored) { }
        }
        showMessage(errorMessage);
    }

    private void showMessage(String message) {
        new android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
}
