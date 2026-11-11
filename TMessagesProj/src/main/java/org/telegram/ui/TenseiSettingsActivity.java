package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TenseiConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TenseiPillSwitch;

import java.util.ArrayList;

public class TenseiSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_RESET = 1;

    private TenseiPillSwitch highSpeedSwitch;
    private TenseiPillSwitch antiDeleteSwitch;
    private TenseiPillSwitch hideTypingSwitch;
    private TenseiPillSwitch hideSeenSwitch;
    private TenseiPillSwitch screenshotSwitch;
    private TenseiPillSwitch forwardSwitch;
    private TenseiPillSwitch hidePhoneSwitch;
    private TenseiPillSwitch hideOnlineSwitch;
    private TenseiPillSwitch vibrationSwitch;
    private View intensityRow;
    private android.widget.SeekBar intensitySeekBar;
    private android.widget.TextView intensityValue;
    private android.widget.TextView stickerLimitValue;

    private boolean lastIsDark;
    private boolean themeInit;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        TenseiConfig.load();
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

    private void checkThemeUpdate() {
        boolean currentDark = TenseiTheme.isDark(getContext());
        if (!themeInit) {
            themeInit = true;
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
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("TenseiSettings", R.string.TenseiSettings));
        if (actionBar.createMenu() != null && actionBar.createMenu().getItem(MENU_RESET) == null) {
            actionBar.createMenu().addItem(MENU_RESET, R.drawable.ic_ab_other);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_RESET) {
                    confirmResetDefaults();
                }
            }
        });

        final boolean dark = TenseiTheme.isDark(context);
        lastIsDark = dark;
        themeInit = true;
        final int colorBg = TenseiTheme.getBgColor(context);
        final int colorCard = TenseiTheme.getCardColor(context);
        final int colorTitle = TenseiTheme.getTitleColor(context);
        final int colorSub = TenseiTheme.getSubtitleColor(context);
        final int colorChevron = TenseiTheme.getChevronColor(context);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(colorBg);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12),
                AndroidUtilities.dp(14), AndroidUtilities.dp(20));
        scrollView.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        content.addView(makeHeaderCard(context, LocaleController.getString("TenseiMods", R.string.TenseiMods),
                LocaleController.getString("TenseiModsInfo", R.string.TenseiModsInfo),
                colorTitle, colorSub));

        LinearLayout modsCard = makeCard(context, colorCard, dark);
        LinearLayout.LayoutParams modsLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        modsLp.topMargin = AndroidUtilities.dp(10);
        content.addView(modsCard, modsLp);

        highSpeedSwitch = addToggleRow(context, modsCard,
                LocaleController.getString("TenseiHighSpeed", R.string.TenseiHighSpeed),
                LocaleController.getString("TenseiHighSpeedInfo", R.string.TenseiHighSpeedInfo),
                TenseiConfig.highSpeedNetworking, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.highSpeedNetworking = checked;
                    TenseiConfig.save();
                });

        antiDeleteSwitch = addToggleRow(context, modsCard,
                LocaleController.getString("TenseiShowDeleted", R.string.TenseiShowDeleted),
                LocaleController.getString("TenseiShowDeletedInfo", R.string.TenseiShowDeletedInfo),
                TenseiConfig.showDeletedMessages, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.showDeletedMessages = checked;
                    TenseiConfig.save();
                });

        screenshotSwitch = addToggleRow(context, modsCard,
                LocaleController.getString("TenseiEnableScreenshot", R.string.TenseiEnableScreenshot),
                LocaleController.getString("TenseiEnableScreenshotInfo", R.string.TenseiEnableScreenshotInfo),
                TenseiConfig.allowScreenshot, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.allowScreenshot = checked;
                    TenseiConfig.save();
                });

        forwardSwitch = addToggleRow(context, modsCard,
                LocaleController.getString("TenseiEnableForward", R.string.TenseiEnableForward),
                LocaleController.getString("TenseiEnableForwardInfo", R.string.TenseiEnableForwardInfo),
                TenseiConfig.allowForward, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.allowForward = checked;
                    TenseiConfig.save();
                });

        vibrationSwitch = addToggleRow(context, modsCard,
                LocaleController.getString("TenseiVibration", R.string.TenseiVibration),
                LocaleController.getString("TenseiVibrationInfo", R.string.TenseiVibrationInfo),
                TenseiConfig.vibrationEnabled, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.vibrationEnabled = checked;
                    TenseiConfig.save();
                    refreshVibrationBar(true);
                });

        intensityRow = makeIntensityRow(context, colorTitle, colorSub);
        modsCard.addView(intensityRow);

        LinearLayout stickerCard = makeCard(context, colorCard, dark);
        LinearLayout.LayoutParams stickerLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        stickerLp.topMargin = AndroidUtilities.dp(10);
        content.addView(stickerCard, stickerLp);
        stickerCard.addView(makeStickerRow(context, colorTitle, colorSub, colorChevron));

        LinearLayout.LayoutParams privacyHeaderLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        privacyHeaderLp.topMargin = AndroidUtilities.dp(10);
        content.addView(makeHeaderCard(context, LocaleController.getString("TenseiPrivacy", R.string.TenseiPrivacy),
                LocaleController.getString("TenseiPrivacyInfo", R.string.TenseiPrivacyInfo),
                colorTitle, colorSub), privacyHeaderLp);

        LinearLayout privacyCard = makeCard(context, colorCard, dark);
        LinearLayout.LayoutParams privacyLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        privacyLp.topMargin = AndroidUtilities.dp(10);
        content.addView(privacyCard, privacyLp);

        hidePhoneSwitch = addToggleRow(context, privacyCard,
                LocaleController.getString("TenseiHidePhone", R.string.TenseiHidePhone),
                LocaleController.getString("TenseiHidePhoneInfo", R.string.TenseiHidePhoneInfo),
                TenseiConfig.hidePhoneFromBots, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.hidePhoneFromBots = checked;
                    TenseiConfig.save();
                });

        hideOnlineSwitch = addToggleRow(context, privacyCard,
                LocaleController.getString("TenseiHideOnline", R.string.TenseiHideOnline),
                LocaleController.getString("TenseiHideOnlineInfo", R.string.TenseiHideOnlineInfo),
                TenseiConfig.hideOnline, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.hideOnline = checked;
                    TenseiConfig.save();
                });

        hideTypingSwitch = addToggleRow(context, privacyCard,
                LocaleController.getString("TenseiHideTyping", R.string.TenseiHideTyping),
                LocaleController.getString("TenseiHideTypingInfo", R.string.TenseiHideTypingInfo),
                TenseiConfig.hideTyping, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.hideTyping = checked;
                    TenseiConfig.save();
                });

        hideSeenSwitch = addToggleRow(context, privacyCard,
                LocaleController.getString("TenseiHideSeen", R.string.TenseiHideSeen),
                LocaleController.getString("TenseiHideSeenInfo", R.string.TenseiHideSeenInfo),
                TenseiConfig.hideSeen, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.hideSeen = checked;
                    TenseiConfig.save();
                });

        updateStickerLimitValue();

        fragmentView = root;
        return root;
    }

    private FrameLayout makeHeaderCard(Context context, String title, String subtitle, int colorTitle, int colorSub) {
        // Header background (TenseiHeaderDrawable) is always light blue, even in
        // dark mode, so the text must always stay dark to remain readable.
        final int headerTitleColor = 0xFF1C1C1E;
        final int headerSubColor = 0xFF636366;
        FrameLayout card = new FrameLayout(context);
        card.setBackground(new TenseiHeaderDrawable());

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16),
                AndroidUtilities.dp(18), AndroidUtilities.dp(16));
        card.addView(inner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(19);
        tvTitle.setTypeface(AndroidUtilities.bold());
        tvTitle.setTextColor(headerTitleColor);
        inner.addView(tvTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(13.5f);
        tvSub.setTextColor(headerSubColor);
        inner.addView(tvSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

        return card;
    }

    private LinearLayout makeCard(Context context, int colorCard, boolean dark) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorCard);
        bg.setCornerRadius(AndroidUtilities.dp(20));
        if (dark) {
            bg.setStroke(AndroidUtilities.dp(1), 0xFF242C38);
        }
        card.setBackground(bg);
        return card;
    }

    private interface OnToggle {
        void onToggle(boolean checked);
    }

    private TenseiPillSwitch addToggleRow(Context context, LinearLayout card, String title, String subtitle,
                                          boolean checked, int colorTitle, int colorSub, OnToggle listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(13),
                AndroidUtilities.dp(16), AndroidUtilities.dp(13));
        row.setClickable(true);
        row.setFocusable(true);
        card.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f);
        textsLp.rightMargin = AndroidUtilities.dp(12);
        row.addView(texts, textsLp);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(16.5f);
        tvTitle.setTypeface(AndroidUtilities.bold());
        tvTitle.setTextColor(colorTitle);
        texts.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(13.2f);
        tvSub.setTextColor(colorSub);
        texts.addView(tvSub, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        TenseiPillSwitch sw = new TenseiPillSwitch(context);
        sw.setChecked(checked, false);
        row.addView(sw, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        sw.setOnCheckedChangeListener((view, isChecked) -> {
            if (listener != null) {
                listener.onToggle(isChecked);
            }
        });
        row.setOnClickListener(v -> sw.performClick());

        return sw;
    }

    private View makeStickerRow(Context context, int colorTitle, int colorSub, int colorChevron) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14),
                AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        row.setClickable(true);
        row.setFocusable(true);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(LocaleController.getString("TenseiStickerLimit", R.string.TenseiStickerLimit));
        tvTitle.setTextSize(16.5f);
        tvTitle.setTextColor(colorTitle);
        row.addView(tvTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        stickerLimitValue = new TextView(context);
        stickerLimitValue.setTextSize(15);
        stickerLimitValue.setTextColor(colorSub);
        LinearLayout.LayoutParams valueLp = LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        valueLp.rightMargin = AndroidUtilities.dp(6);
        row.addView(stickerLimitValue, valueLp);

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_tensei_chevron);
        chevron.setColorFilter(colorChevron);
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        row.addView(chevron, LayoutHelper.createLinear(14, 22));

        row.setOnClickListener(v -> openStickerLimitDialog());

        return row;
    }

    private View makeIntensityRow(Context context, int colorTitle, int colorSub) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6),
                AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(LocaleController.getString("TenseiVibrationIntensity", R.string.TenseiVibrationIntensity));
        tvTitle.setTextSize(15);
        tvTitle.setTextColor(colorTitle);
        topRow.addView(tvTitle, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        intensityValue = new TextView(context);
        intensityValue.setTextSize(14);
        intensityValue.setTextColor(colorSub);
        topRow.addView(intensityValue, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        intensitySeekBar = new android.widget.SeekBar(context);
        intensitySeekBar.setMax(100);
        intensitySeekBar.setProgress(Math.max(0, Math.min(100, TenseiConfig.vibrationIntensity)));
        try {
            android.content.res.ColorStateList tint = android.content.res.ColorStateList.valueOf(0xFF3372E6);
            intensitySeekBar.setProgressTintList(tint);
            intensitySeekBar.setThumbTintList(tint);
        } catch (Throwable ignored) {
        }
        intensitySeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    TenseiConfig.vibrationIntensity = Math.max(0, Math.min(100, progress));
                    TenseiConfig.save();
                    updateIntensityValue();
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                if (TenseiConfig.vibrationEnabled && TenseiConfig.vibrationIntensity > 0) {
                    try {
                        org.telegram.messenger.BotWebViewVibrationEffect.IMPACT_MEDIUM.vibrate();
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
        row.addView(intensitySeekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        refreshVibrationBar();
        return row;
    }

    private void updateIntensityValue() {
        if (intensityValue != null) {
            intensityValue.setText(TenseiConfig.vibrationIntensity + "%");
        }
    }

    private void refreshVibrationBar() {
        refreshVibrationBar(false);
    }

    private void refreshVibrationBar(boolean animate) {
        updateIntensityValue();
        boolean on = TenseiConfig.vibrationEnabled;
        if (intensitySeekBar != null) {
            int pct = Math.max(0, Math.min(100, TenseiConfig.vibrationIntensity));
            if (intensitySeekBar.getProgress() != pct) {
                intensitySeekBar.setProgress(pct);
            }
        }
        setVibrationBarExpanded(on, animate);
    }

    private android.animation.ValueAnimator vibrationBarAnimator;

    private void setVibrationBarExpanded(boolean expand, boolean animate) {
        if (intensityRow == null) {
            return;
        }
        if (vibrationBarAnimator != null) {
            vibrationBarAnimator.cancel();
            vibrationBarAnimator = null;
        }
        if (expand) {
            intensityRow.setVisibility(View.VISIBLE);
            int w = intensityRow.getWidth();
            if (w <= 0 && intensityRow.getParent() instanceof View) {
                w = ((View) intensityRow.getParent()).getWidth();
            }
            if (w <= 0) {
                w = AndroidUtilities.displaySize.x - AndroidUtilities.dp(28);
            }
            intensityRow.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            final int targetHeight = intensityRow.getMeasuredHeight();
            if (!animate || targetHeight <= 0) {
                ViewGroup.LayoutParams lp = intensityRow.getLayoutParams();
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    intensityRow.setLayoutParams(lp);
                }
                intensityRow.setAlpha(1f);
                return;
            }
            vibrationBarAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
            vibrationBarAnimator.setDuration(280);
            vibrationBarAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            vibrationBarAnimator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = intensityRow.getLayoutParams();
                if (lp != null) {
                    lp.height = (int) (targetHeight * fraction);
                    intensityRow.setLayoutParams(lp);
                }
                intensityRow.setAlpha(fraction);
            });
            vibrationBarAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    ViewGroup.LayoutParams lp = intensityRow.getLayoutParams();
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        intensityRow.setLayoutParams(lp);
                    }
                    intensityRow.setAlpha(1f);
                }
            });
            vibrationBarAnimator.start();
        } else {
            final int startH = intensityRow.getHeight();
            if (!animate || startH <= 0) {
                intensityRow.setVisibility(View.GONE);
                return;
            }
            vibrationBarAnimator = android.animation.ValueAnimator.ofFloat(1f, 0f);
            vibrationBarAnimator.setDuration(240);
            vibrationBarAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            vibrationBarAnimator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();
                ViewGroup.LayoutParams lp = intensityRow.getLayoutParams();
                if (lp != null) {
                    lp.height = (int) (startH * fraction);
                    intensityRow.setLayoutParams(lp);
                }
                intensityRow.setAlpha(fraction);
            });
            vibrationBarAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    intensityRow.setVisibility(View.GONE);
                    ViewGroup.LayoutParams lp = intensityRow.getLayoutParams();
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        intensityRow.setLayoutParams(lp);
                    }
                    intensityRow.setAlpha(1f);
                }
            });
            vibrationBarAnimator.start();
        }
    }

    private void updateStickerLimitValue() {
        if (stickerLimitValue != null) {
            stickerLimitValue.setText(TenseiConfig.recentStickersLimit <= 0 ? LocaleController.getString("TenseiTelegramDefault", R.string.TenseiTelegramDefault) : ("" + TenseiConfig.recentStickersLimit));
        }
    }

    private void refreshSwitches() {
        if (highSpeedSwitch != null) {
            highSpeedSwitch.setChecked(TenseiConfig.highSpeedNetworking, false);
        }
        if (antiDeleteSwitch != null) {
            antiDeleteSwitch.setChecked(TenseiConfig.showDeletedMessages, false);
        }
        if (hideTypingSwitch != null) {
            hideTypingSwitch.setChecked(TenseiConfig.hideTyping, false);
        }
        if (hideSeenSwitch != null) {
            hideSeenSwitch.setChecked(TenseiConfig.hideSeen, false);
        }
        if (screenshotSwitch != null) {
            screenshotSwitch.setChecked(TenseiConfig.allowScreenshot, false);
        }
        if (forwardSwitch != null) {
            forwardSwitch.setChecked(TenseiConfig.allowForward, false);
        }
        if (hidePhoneSwitch != null) {
            hidePhoneSwitch.setChecked(TenseiConfig.hidePhoneFromBots, false);
        }
        if (hideOnlineSwitch != null) {
            hideOnlineSwitch.setChecked(TenseiConfig.hideOnline, false);
        }
        if (vibrationSwitch != null) {
            vibrationSwitch.setChecked(TenseiConfig.vibrationEnabled, false);
        }
        refreshVibrationBar();
        updateStickerLimitValue();
    }

    private void confirmResetDefaults() {
        Context ctx = getParentActivity();
        if (ctx == null) {
            ctx = getContext();
        }
        if (ctx == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString("TenseiResetDefaults", R.string.TenseiResetDefaults));
        builder.setMessage(LocaleController.getString("TenseiResetDefaultsInfo", R.string.TenseiResetDefaultsInfo));
        builder.setPositiveButton(LocaleController.getString(R.string.Reset), (di, which) -> {
            TenseiConfig.highSpeedNetworking = false;
            TenseiConfig.showDeletedMessages = false;
            TenseiConfig.hideTyping = false;
            TenseiConfig.hideSeen = false;
            TenseiConfig.allowScreenshot = false;
            TenseiConfig.allowForward = false;
            TenseiConfig.vibrationEnabled = true;
            TenseiConfig.vibrationIntensity = 100;
            TenseiConfig.recentStickersLimit = 0;
            TenseiConfig.hidePhoneFromBots = true;
            TenseiConfig.hideOnline = false;
            TenseiConfig.save();
            refreshSwitches();
            try {
                di.dismiss();
            } catch (Throwable ignored) {
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openStickerLimitDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) {
            ctx = getContext();
        }
        if (ctx == null) {
            return;
        }
        final String[] labels = {LocaleController.getString("TenseiTelegramDefault", R.string.TenseiTelegramDefault), "20", "50", "100", "150", "200"};
        final int[] values = {0, 20, 50, 100, 150, 200};
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(LocaleController.getString("TenseiStickerLimit", R.string.TenseiStickerLimit));
        builder.setItems(labels, (di, which) -> {
            if (which >= 0 && which < values.length) {
                TenseiConfig.recentStickersLimit = values[which];
                TenseiConfig.save();
                updateStickerLimitValue();
            }
            try {
                di.dismiss();
            } catch (Throwable ignored) {
            }
        });
        showDialog(builder.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        TenseiConfig.load();
        refreshSwitches();
        checkThemeUpdate();
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        return themeDescriptions;
    }
}
