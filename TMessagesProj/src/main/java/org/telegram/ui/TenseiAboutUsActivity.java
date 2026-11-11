package org.telegram.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TelegramQRCodeWriter;
import org.telegram.messenger.TenseiSecurity;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class TenseiAboutUsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private boolean lastIsDark;
    private boolean isThemeInitialized;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        hasOwnBackground = true;
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewTheme);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didSetNewTheme) {
            checkThemeUpdate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkThemeUpdate();
    }

    private void checkThemeUpdate() {
        boolean currentDark = TenseiTheme.isDark(getContext());
        if (!isThemeInitialized) {
            isThemeInitialized = true;
            lastIsDark = currentDark;
            return;
        }
        if (lastIsDark != currentDark) {
            lastIsDark = currentDark;
            if (fragmentView != null && fragmentView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                int index = parent.indexOfChild(fragmentView);
                ViewGroup.LayoutParams lp = fragmentView.getLayoutParams();
                parent.removeView(fragmentView);
                View newView = createView(getContext());
                parent.addView(newView, index, lp);
                checkSystemBarColors();
            }
        }
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar bar = super.createActionBar(context);
        if (bar != null) {
            bar.setAddToContainer(false);
            bar.setVisibility(View.GONE);
        }
        return bar;
    }

    @Override
    public boolean isLightStatusBar() {
        return !TenseiTheme.isDark(getParentActivity());
    }

    @Override
    public View createView(Context context) {
        if (actionBar != null) {
            actionBar.setAddToContainer(false);
            actionBar.setVisibility(View.GONE);
        }

        final boolean dark = TenseiTheme.isDark(context);
        lastIsDark = dark;
        isThemeInitialized = true;

        final int colorBg = TenseiTheme.getBgColor(context);
        final int colorCard = TenseiTheme.getCardColor(context);
        final int colorTitle = TenseiTheme.getTitleColor(context);
        final int colorSub = TenseiTheme.getSubtitleColor(context);
        final int colorIconBg = TenseiTheme.getIconBgColor(context);
        final int colorDivider = TenseiTheme.getDividerColor(context);
        final int colorChevron = TenseiTheme.getChevronColor(context);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(colorBg);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(colorBg);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        root.addView(scrollView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int topPadding = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(8);
        content.setPadding(AndroidUtilities.dp(16), topPadding,
                AndroidUtilities.dp(16), AndroidUtilities.dp(36));
        scrollView.addView(content,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout backBtn = new FrameLayout(context);
        backBtn.setBackground(Theme.createSimpleSelectorCircleDrawable(
                AndroidUtilities.dp(52), colorIconBg, dark ? 0x22FFFFFF : 0x18000000));
        backBtn.setClickable(true);
        backBtn.setFocusable(true);
        backBtn.setOnClickListener(v -> finishFragment());

        ImageView backIcon = new ImageView(context);
        backIcon.setImageResource(R.drawable.ic_ab_back);
        backIcon.setColorFilter(colorTitle);
        backIcon.setScaleType(ImageView.ScaleType.CENTER);
        backBtn.addView(backIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        LinearLayout.LayoutParams backLp = LayoutHelper.createLinear(52, 52, Gravity.START);
        backLp.bottomMargin = AndroidUtilities.dp(14);
        content.addView(backBtn, backLp);

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("TenseiAbout", R.string.TenseiAbout));
        title.setTextSize(26);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(colorTitle);
        LinearLayout.LayoutParams titleLp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT);
        titleLp.bottomMargin = AndroidUtilities.dp(16);
        content.addView(title, titleLp);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorCard);
        bg.setCornerRadius(AndroidUtilities.dp(20));
        if (dark) {
            bg.setStroke(AndroidUtilities.dp(1), 0xFF242C38);
        }
        card.setBackground(bg);
        content.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT));

        addInfoRow(context, card, LocaleController.getString("TenseiClientName", R.string.TenseiClientName), "TenseiGram", colorTitle, colorSub, true, colorDivider);

        addInfoRow(context, card, LocaleController.getString("TenseiVersion", R.string.TenseiVersion), "13.2.1", colorTitle, colorSub, true, colorDivider);

        addInfoRow(context, card, LocaleController.getString("TenseiPackage", R.string.TenseiPackage), "com.tensei.mod", colorTitle, colorSub, true, colorDivider);

        String devUsername = TenseiSecurity.getDeveloperUsername();
        if (devUsername.startsWith("@")) {
            devUsername = devUsername.substring(1);
        }
        final String devFinal = devUsername;
        addClickableRow(context, card, LocaleController.getString("TenseiDeveloper", R.string.TenseiDeveloper), "@" + devFinal, colorTitle, colorSub, colorChevron, dark,
                () -> Browser.openUrl(getContext(), "https://t.me/" + devFinal), true, colorDivider);

        addClickableRow(context, card, LocaleController.getString("TenseiChannel", R.string.TenseiChannel), "@TenseiGram", colorTitle, colorSub, colorChevron, dark,
                () -> Browser.openUrl(getContext(), "https://t.me/TenseiGram"), true, colorDivider);

        addClickableRow(context, card, LocaleController.getString("TenseiGroup", R.string.TenseiGroup), "@TenseiGramChat", colorTitle, colorSub, colorChevron, dark,
                () -> Browser.openUrl(getContext(), "https://t.me/TenseiGramChat"), true, colorDivider);

        addClickableRow(context, card, LocaleController.getString("TenseiDonate", R.string.TenseiDonate), "koustubhgaikwad7-1@okaxis", colorTitle, colorSub, colorChevron, dark,
                () -> new DonateBottomSheet(context, dark).show(), true, colorDivider);

        addClickableRow(context, card, LocaleController.getString("TenseiGithub", R.string.TenseiGithub), LocaleController.getString("TenseiGithubInfo", R.string.TenseiGithubInfo), colorTitle, colorSub, colorChevron, dark,
                () -> Browser.openUrl(getContext(), "https://github.com/Koustubh12345/TenseiGram"), false, colorDivider);

        fragmentView = root;
        return root;
    }

    private void addInfoRow(Context context, LinearLayout card, String title, String value,
                            int colorTitle, int colorSub, boolean divider, int colorDivider) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16),
                AndroidUtilities.dp(18), AndroidUtilities.dp(16));

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(15);
        t.setTextColor(colorTitle);
        row.addView(t, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(14);
        v.setTextColor(colorSub);
        row.addView(v);

        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT));

        if (divider) {
            View div = new View(context);
            div.setBackgroundColor(colorDivider);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.leftMargin = AndroidUtilities.dp(18);
            lp.rightMargin = AndroidUtilities.dp(18);
            card.addView(div, lp);
        }
    }

    private void addClickableRow(Context context, LinearLayout card, String title, String value,
                                 int colorTitle, int colorSub, int colorChevron, boolean dark,
                                 Runnable onClick, boolean divider, int colorDivider) {
        FrameLayout container = new FrameLayout(context);
        container.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(20), 0, dark ? 0x1AFFFFFF : 0x12000000));
        container.setClickable(true);
        container.setFocusable(true);
        container.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16),
                AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        container.addView(row, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(15);
        t.setTextColor(colorTitle);
        row.addView(t, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(14);
        v.setTextColor(0xFF2979FF);
        row.addView(v, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_tensei_chevron);
        chevron.setColorFilter(colorChevron);
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        row.addView(chevron, LayoutHelper.createLinear(12, 18));

        card.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT));

        if (divider) {
            View div = new View(context);
            div.setBackgroundColor(colorDivider);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.leftMargin = AndroidUtilities.dp(18);
            lp.rightMargin = AndroidUtilities.dp(18);
            card.addView(div, lp);
        }
    }

    public static class DonateBottomSheet extends BottomSheet {
        private final Bitmap qrBitmap;

        public DonateBottomSheet(Context context, boolean dark) {
            super(context, false);
            fixNavigationBar();
            setTitle(LocaleController.getString("TenseiDonateNow", R.string.TenseiDonateNow), true);

            final String upiUri = "upi://pay?pa=koustubhgaikwad7-1@okaxis&pn=Koustubh%20Gaikwad&cu=INR";

            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER_HORIZONTAL);
            content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

            FrameLayout qrFrame = new FrameLayout(context);
            GradientDrawable qrBg = new GradientDrawable();
            qrBg.setColor(Color.WHITE);
            qrBg.setCornerRadius(AndroidUtilities.dp(18));
            qrFrame.setBackground(qrBg);
            qrFrame.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

            ImageView qrImageView = new ImageView(context);
            qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            Bitmap generated = null;
            try {
                HashMap<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 0);
                TelegramQRCodeWriter writer = new TelegramQRCodeWriter();
                generated = writer.encode(upiUri, 768, 768, hints, null);
            } catch (Throwable t) {
                FileLog.e(t);
            }
            this.qrBitmap = generated;
            if (qrBitmap != null) {
                qrImageView.setImageBitmap(qrBitmap);
            }

            qrFrame.addView(qrImageView, LayoutHelper.createFrame(220, 220, Gravity.CENTER));
            content.addView(qrFrame, LayoutHelper.createLinear(252, 252, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

            TextView upiIdView = new TextView(context);
            upiIdView.setText("koustubhgaikwad7-1@okaxis");
            upiIdView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            upiIdView.setTypeface(AndroidUtilities.bold());
            upiIdView.setTextColor(dark ? 0xFFFFFFFF : 0xFF222222);
            upiIdView.setGravity(Gravity.CENTER);
            content.addView(upiIdView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

            TextView subView = new TextView(context);
            subView.setText(LocaleController.getString("TenseiDonateInfo", R.string.TenseiDonateInfo));
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subView.setTextColor(dark ? 0xFF8E959E : 0xFF707579);
            subView.setGravity(Gravity.CENTER);
            content.addView(subView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

            LinearLayout buttonsLayout = new LinearLayout(context);
            buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView saveBtn = new TextView(context);
            saveBtn.setText(LocaleController.getString("TenseiSaveQr", R.string.TenseiSaveQr));
            saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            saveBtn.setTypeface(AndroidUtilities.bold());
            saveBtn.setGravity(Gravity.CENTER);
            saveBtn.setTextColor(0xFF2979FF);
            saveBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                    AndroidUtilities.dp(14),
                    dark ? 0x262979FF : 0x1A2979FF,
                    dark ? 0x4D2979FF : 0x332979FF));
            saveBtn.setOnClickListener(v -> {
                if (qrBitmap != null) {
                    boolean saved = saveBitmapToGallery(context, qrBitmap);
                    Toast.makeText(context, saved ?
                            LocaleController.getString("TenseiQrSaved", R.string.TenseiQrSaved) :
                            LocaleController.getString("TenseiQrSaveError", R.string.TenseiQrSaveError),
                            Toast.LENGTH_SHORT).show();
                }
            });
            buttonsLayout.addView(saveBtn, LayoutHelper.createLinear(0, 48, 1f, 0, 0, 8, 0));

            TextView payBtn = new TextView(context);
            payBtn.setText(LocaleController.getString("TenseiPayNow", R.string.TenseiPayNow));
            payBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            payBtn.setTypeface(AndroidUtilities.bold());
            payBtn.setGravity(Gravity.CENTER);
            payBtn.setTextColor(Color.WHITE);
            payBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                    AndroidUtilities.dp(14), 0xFF2979FF, 0xFF1C68E3));
            payBtn.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUri));
                    Intent chooser = Intent.createChooser(intent, "Pay with UPI");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(chooser);
                } catch (Throwable t) {
                    Toast.makeText(context, LocaleController.getString("TenseiNoUpiApp", R.string.TenseiNoUpiApp), Toast.LENGTH_SHORT).show();
                }
            });
            buttonsLayout.addView(payBtn, LayoutHelper.createLinear(0, 48, 1f, 8, 0, 0, 0));

            content.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(content);
            setCustomView(scrollView);
            setBackgroundColor(dark ? 0xFF1C242F : 0xFFFFFFFF);
            if (getTitleView() != null) {
                getTitleView().setTextColor(dark ? 0xFFFFFFFF : 0xFF000000);
            }
        }

        private static boolean saveBitmapToGallery(Context context, Bitmap bitmap) {
            if (bitmap == null || context == null) return false;
            try {
                String filename = "TenseiGram_Donate_QR_" + System.currentTimeMillis() + ".png";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TenseiGram");
                    ContentResolver resolver = context.getContentResolver();
                    Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (OutputStream os = resolver.openOutputStream(uri)) {
                            if (os != null) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                                os.flush();
                                return true;
                            }
                        }
                    }
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File tenseiDir = new File(dir, "TenseiGram");
                    if (!tenseiDir.exists()) tenseiDir.mkdirs();
                    File file = new File(tenseiDir, filename);
                    try (OutputStream os = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                    }
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    return true;
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
            return false;
        }
    }
}
