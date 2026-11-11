package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TenseiEditHistory;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import java.util.List;

public class TenseiEditViewerActivity extends BaseFragment {
    private long dialogId;
    private int mid;
    private String current;
    public TenseiEditViewerActivity(long d, int m, String c) {
        dialogId = d;
        mid = m;
        current = c;
    }
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("TenseiViewEdits", R.string.TenseiViewEdits));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        ScrollView sv = new ScrollView(context);
        root.addView(sv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        sv.addView(col, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        List<String> h = TenseiEditHistory.get(dialogId, mid, current);
        if (h == null || h.isEmpty()) {
            TextView t = new TextView(context);
            t.setText(LocaleController.getString("TenseiNoEditHistory", R.string.TenseiNoEditHistory));
            t.setTextSize(15);
            t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            t.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            t.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
            col.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        } else {
            for (int i = 0; i < h.size(); i++) {
                String s = h.get(i);
                LinearLayout card = new LinearLayout(context);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
                TextView label = new TextView(context);
                boolean last = (i == h.size() - 1);
                label.setText(last ? LocaleController.getString("TenseiAfterEdit", R.string.TenseiAfterEdit) : (LocaleController.getString("TenseiBeforeEdit", R.string.TenseiBeforeEdit) + " " + (i + 1)));
                label.setTextSize(12);
                label.setTypeface(AndroidUtilities.bold());
                label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                card.addView(label);
                TextView body = new TextView(context);
                body.setText((s == null || s.isEmpty()) ? LocaleController.getString("TenseiEmpty", R.string.TenseiEmpty) : s);
                body.setTextSize(15);
                body.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                body.setPadding(0, AndroidUtilities.dp(6), 0, 0);
                card.addView(body);
                LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
                lp.bottomMargin = AndroidUtilities.dp(10);
                col.addView(card, lp);
            }
        }
        fragmentView = root;
        return root;
    }
}
