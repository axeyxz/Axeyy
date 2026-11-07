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
    private LinearLayout root;

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

    private void render() {
        boolean inWorkProfile = dpm.isProfileOwnerApp(getPackageName());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(26), dp(48), dp(26), dp(30));
        page.setBackgroundColor(bg);

        TextView logo = text("AXEYY", 36, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(.12f);
        page.addView(logo, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView tagline = text("YOUR WORKSPACE", 12, 0xff78b7ff, true);
        tagline.setGravity(Gravity.CENTER);
        page.addView(tagline);

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(1, dp(58)));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(20), dp(20), dp(20), dp(20));
        status.setBackground(rounded(panel, 22));
        TextView statusTitle = text(
            inWorkProfile ? "WORK PROFILE SIAP" : "RUANG KERJA ANDROID",
            17, inWorkProfile ? 0xff50dfa8 : Color.WHITE, true);
        status.addView(statusTitle);
        TextView statusDesc = text(
            inWorkProfile
                ? "AXEYY sedang berjalan di profil kerja."
                : "Buat profil kerja resmi Android untuk memisahkan Gmail dan aplikasi kerja.",
            13, 0xffa9bad0, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(8);
        status.addView(statusDesc, descParams);
        page.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button action = new Button(this);
        action.setAllCaps(false);
        action.setTextColor(Color.WHITE);
        action.setTextSize(15);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setBackground(rounded(blue, 18));
        action.setText(inWorkProfile ? "BUKA GMAIL KERJA" : "BUAT PROFIL KERJA");
        action.setOnClickListener(v -> {
            if (inWorkProfile) openWorkGmail();
            else createWorkProfile();
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(58));
        actionParams.topMargin = dp(22);
        page.addView(action, actionParams);

        TextView footer = text(
            inWorkProfile ? "Gmail akan dibuka di ruang kerja ini." :
                "Profil kerja dikelola langsung oleh sistem Android.",
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

    private void openWorkGmail() {
        Intent gmail = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
        if (gmail != null) {
            try {
                startActivity(gmail);
                return;
            } catch (Exception ignored) { }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.google.android.gm")));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gm")));
            } catch (Exception ex) {
                showMessage("Gmail belum terpasang di profil kerja. Buka Play Store di profil kerja lalu pasang Gmail.");
            }
        }
    }

    private void showMessage(String message) {
        new android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
}
