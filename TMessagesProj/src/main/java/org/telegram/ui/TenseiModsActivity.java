package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TenseiPillSwitch;

public class TenseiModsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ValueAnimator subContainerAnimator;
    private boolean lastIsDark;
    private boolean isThemeInitialized;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        TenseiConfig.load();
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
        final int colorIconTint = TenseiTheme.getIconTintColor(context);
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
        int topPadding = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(4);
        content.setPadding(AndroidUtilities.dp(14), topPadding,
                AndroidUtilities.dp(14), AndroidUtilities.dp(20));
        scrollView.addView(content,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout backBtn = new FrameLayout(context);
        backBtn.setBackground(Theme.createSimpleSelectorCircleDrawable(
                AndroidUtilities.dp(44), colorIconBg, dark ? 0x22FFFFFF : 0x18000000));
        backBtn.setClickable(true);
        backBtn.setFocusable(true);
        backBtn.setOnClickListener(v -> finishFragment());

        ImageView backIcon = new ImageView(context);
        backIcon.setImageResource(R.drawable.ic_ab_back);
        backIcon.setColorFilter(colorTitle);
        backIcon.setScaleType(ImageView.ScaleType.CENTER);
        backBtn.addView(backIcon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));

        LinearLayout.LayoutParams backLp = LayoutHelper.createLinear(44, 44, Gravity.START);
        backLp.bottomMargin = AndroidUtilities.dp(10);
        content.addView(backBtn, backLp);

        ImageView heroCard = new ImageView(context);
        heroCard.setImageResource(dark ? R.drawable.tensei_hero_card_dark : R.drawable.tensei_hero_card);
        heroCard.setScaleType(ImageView.ScaleType.FIT_XY);
        heroCard.setAdjustViewBounds(true);
        LinearLayout.LayoutParams heroLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        heroLp.bottomMargin = AndroidUtilities.dp(2);
        content.addView(heroCard, heroLp);

        LinearLayout groupCard = new LinearLayout(context);
        groupCard.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable groupBg = new GradientDrawable();
        groupBg.setColor(colorCard);
        groupBg.setCornerRadius(AndroidUtilities.dp(22));
        if (dark) {
            groupBg.setStroke(AndroidUtilities.dp(1), 0xFF242C38);
        }
        groupCard.setBackground(groupBg);

        LinearLayout.LayoutParams groupLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        groupLp.topMargin = AndroidUtilities.dp(10);
        content.addView(groupCard, groupLp);

        LinearLayout subContainer = new LinearLayout(context);
        subContainer.setOrientation(LinearLayout.VERTICAL);
        subContainer.setClipChildren(true);
        subContainer.setClipToPadding(true);

        View rowCommands = makeListToggleRow(context, R.drawable.ic_tensei_terminal,
                LocaleController.getString("TenseiCommands", R.string.TenseiCommands), LocaleController.getString("TenseiCommandsInfo", R.string.TenseiCommandsInfo), TenseiConfig.commandsEnabled,
                colorIconBg, colorIconTint, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.commandsEnabled = checked;
                    TenseiConfig.save();
                    animateSubContainer(subContainer, scrollView, checked);
                });
        groupCard.addView(rowCommands, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subContainer.addView(makeDivider(context, colorDivider));

        View rowPing = makeListToggleRow(context, R.drawable.ic_tensei_wifi,
                LocaleController.getString("TenseiPing", R.string.TenseiPing), LocaleController.getString("TenseiPingInfo", R.string.TenseiPingInfo), TenseiConfig.pingEnabled,
                colorIconBg, colorIconTint, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.pingEnabled = checked;
                    TenseiConfig.save();
                });
        subContainer.addView(rowPing, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subContainer.addView(makeDivider(context, colorDivider));

        View rowAi = makeListToggleRow(context, R.drawable.ic_tensei_brain,
                LocaleController.getString("TenseiAiMode", R.string.TenseiAiMode), LocaleController.getString("TenseiAiModeInfo", R.string.TenseiAiModeInfo), TenseiConfig.aiModeEnabled,
                colorIconBg, colorIconTint, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.aiModeEnabled = checked;
                    TenseiConfig.save();
                });
        subContainer.addView(rowAi, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subContainer.addView(makeDivider(context, colorDivider));

        View rowYtdl = makeListToggleRow(context, R.drawable.ic_tensei_ytdl,
                LocaleController.getString("TenseiYtdl", R.string.TenseiYtdl), LocaleController.getString("TenseiYtdlInfo", R.string.TenseiYtdlInfo), TenseiConfig.ytdlEnabled,
                colorIconBg, colorIconTint, colorTitle, colorSub,
                checked -> {
                    TenseiConfig.ytdlEnabled = checked;
                    TenseiConfig.save();
                });
        subContainer.addView(rowYtdl, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subContainer.addView(makeDivider(context, colorDivider));

        View rowBot = makeListClickRow(context, R.drawable.ic_tensei_bot,
                LocaleController.getString("TenseiRunBotLocally", R.string.TenseiRunBotLocally),
                LocaleController.getString("TenseiRunBotLocallyInfo", R.string.TenseiRunBotLocallyInfo),
                colorIconBg, colorIconTint, colorTitle, colorSub, colorChevron,
                v -> presentFragment(new TenseiBotRunnerActivity()));
        subContainer.addView(rowBot, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subContainer.setVisibility(TenseiConfig.commandsEnabled ? View.VISIBLE : View.GONE);
        groupCard.addView(subContainer, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        View settingsRow = makeNavCard(context, R.drawable.ic_tensei_gear,
                LocaleController.getString("TenseiSettings", R.string.TenseiSettings), LocaleController.getString("TenseiConfigureSettings", R.string.TenseiConfigureSettings),
                colorCard, colorIconBg, colorIconTint, colorTitle, colorSub, colorChevron, dark,
                v -> presentFragment(new TenseiSettingsActivity()));
        LinearLayout.LayoutParams srLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        srLp.topMargin = AndroidUtilities.dp(10);
        content.addView(settingsRow, srLp);

        View aboutRow = makeNavCard(context, R.drawable.ic_tensei_people,
                LocaleController.getString("TenseiAbout", R.string.TenseiAbout), LocaleController.getString("TenseiAboutInfo", R.string.TenseiAboutInfo),
                colorCard, colorIconBg, colorIconTint, colorTitle, colorSub, colorChevron, dark,
                v -> presentFragment(new TenseiAboutUsActivity()));
        LinearLayout.LayoutParams arLp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        arLp.topMargin = AndroidUtilities.dp(8);
        content.addView(aboutRow, arLp);

        fragmentView = root;
        return root;
    }

    private void animateSubContainer(LinearLayout subContainer, ScrollView scrollView, boolean expand) {
        if (subContainerAnimator != null) {
            subContainerAnimator.cancel();
        }

        if (expand) {
            subContainer.setVisibility(View.VISIBLE);
            int w = subContainer.getWidth();
            if (w <= 0) {
                w = AndroidUtilities.displaySize.x - AndroidUtilities.dp(28);
            }
            subContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int targetHeight = subContainer.getMeasuredHeight();
            if (targetHeight <= 0) {
                targetHeight = AndroidUtilities.dp(260);
            }

            final int finalTargetHeight = targetHeight;
            final int startH = subContainer.getHeight();
            subContainerAnimator = ValueAnimator.ofFloat(0f, 1f);
            subContainerAnimator.setDuration(320);
            subContainerAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            subContainerAnimator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();
                int h = (int) (startH + (finalTargetHeight - startH) * fraction);
                ViewGroup.LayoutParams lp = subContainer.getLayoutParams();
                if (lp != null) {
                    lp.height = h;
                    subContainer.setLayoutParams(lp);
                }
                subContainer.setAlpha(fraction);
            });
            subContainerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewGroup.LayoutParams lp = subContainer.getLayoutParams();
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        subContainer.setLayoutParams(lp);
                    }
                    subContainer.setAlpha(1f);
                }
            });
            subContainerAnimator.start();

            if (scrollView != null) {
                scrollView.post(() -> {
                    scrollView.smoothScrollBy(0, finalTargetHeight);
                });
            }
        } else {
            int startH = subContainer.getHeight();
            if (startH <= 0) {
                subContainer.setVisibility(View.GONE);
                return;
            }
            final int initialH = startH;
            subContainerAnimator = ValueAnimator.ofFloat(1f, 0f);
            subContainerAnimator.setDuration(280);
            subContainerAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            subContainerAnimator.addUpdateListener(animation -> {
                float fraction = (float) animation.getAnimatedValue();
                int h = (int) (initialH * fraction);
                ViewGroup.LayoutParams lp = subContainer.getLayoutParams();
                if (lp != null) {
                    lp.height = h;
                    subContainer.setLayoutParams(lp);
                }
                subContainer.setAlpha(fraction);
            });
            subContainerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    subContainer.setVisibility(View.GONE);
                    ViewGroup.LayoutParams lp = subContainer.getLayoutParams();
                    if (lp != null) {
                        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        subContainer.setLayoutParams(lp);
                    }
                    subContainer.setAlpha(1f);
                }
            });
            subContainerAnimator.start();

            if (scrollView != null) {
                scrollView.post(() -> {
                    scrollView.smoothScrollBy(0, -initialH / 3);
                });
            }
        }
    }

    private interface OnToggle {
        void onToggle(boolean checked);
    }

    private View makeListToggleRow(Context context, int iconRes, String title, String subtitle,
                                   boolean checked, int colorIconBg, int colorIconTint,
                                   int colorTitle, int colorSub, OnToggle listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(13),
                AndroidUtilities.dp(16), AndroidUtilities.dp(13));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(colorIconBg);
        iconWrap.setBackground(iconBg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(colorIconTint);
        iconWrap.addView(icon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(42, 42));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = LayoutHelper.createLinear(0,
                LayoutHelper.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
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
        row.addView(sw, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT));

        sw.setOnCheckedChangeListener((view, isChecked) -> {
            if (listener != null) {
                listener.onToggle(isChecked);
            }
        });

        return row;
    }

    private View makeListClickRow(Context context, int iconRes, String title, String subtitle,
                                  int colorIconBg, int colorIconTint,
                                  int colorTitle, int colorSub, int colorChevron,
                                  View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(13),
                AndroidUtilities.dp(16), AndroidUtilities.dp(13));
        row.setBackground(Theme.getSelectorDrawable(false));
        row.setClickable(true);
        row.setFocusable(true);
        if (onClick != null) {
            row.setOnClickListener(onClick);
        }

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(colorIconBg);
        iconWrap.setBackground(iconBg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(colorIconTint);
        iconWrap.addView(icon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(42, 42));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = LayoutHelper.createLinear(0,
                LayoutHelper.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
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

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_tensei_chevron);
        chevron.setColorFilter(colorChevron);
        row.addView(chevron, LayoutHelper.createLinear(18, 18));

        return row;
    }

    private View makeDivider(Context context, int colorDivider) {
        View divider = new View(context);
        divider.setBackgroundColor(colorDivider);
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 1);
        lp.leftMargin = AndroidUtilities.dp(16);
        lp.rightMargin = AndroidUtilities.dp(16);
        divider.setLayoutParams(lp);
        return divider;
    }

    private View makeNavCard(Context context, int iconRes, String title, String subtitle,
                             int colorCard, int colorIconBg, int colorIconTint,
                             int colorTitle, int colorSub, int colorChevron, boolean dark,
                             View.OnClickListener onClick) {
        FrameLayout container = new FrameLayout(context);
        container.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(22), colorCard, dark ? 0x1AFFFFFF : 0x12000000));
        container.setClickable(true);
        container.setFocusable(true);
        if (onClick != null) {
            container.setOnClickListener(onClick);
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14),
                AndroidUtilities.dp(18), AndroidUtilities.dp(14));
        container.addView(row, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(colorIconBg);
        iconWrap.setBackground(iconBg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(colorIconTint);
        iconWrap.addView(icon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(42, 42));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = LayoutHelper.createLinear(0,
                LayoutHelper.WRAP_CONTENT, 1f);
        textsLp.leftMargin = AndroidUtilities.dp(14);
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

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.ic_tensei_chevron);
        chevron.setColorFilter(colorChevron);
        chevron.setScaleType(ImageView.ScaleType.CENTER);
        row.addView(chevron, LayoutHelper.createLinear(14, 22));

        return container;
    }
}
