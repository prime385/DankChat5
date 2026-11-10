package com.google.android.material.tabs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TabLayout that reports every time it restyles its tab views (it resets their text colour and
 * appearance whenever the selected tab changes, including on every frame of a swipe), so custom
 * tab colours can be re-applied in the same frame instead of flashing the default colour.
 * Lives in TabLayout's package because the swipe code uses a package-private method.
 */
public class LiveAwareTabLayout extends TabLayout {

    @Nullable
    public Runnable onTabViewsRestyled;

    public LiveAwareTabLayout(@NonNull Context context) {
        super(context);
    }

    public LiveAwareTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveAwareTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    void setScrollPosition(int position, float positionOffset, boolean updateSelectedTabView, boolean updateIndicatorPosition, boolean alsoUpdateSelectedTab) {
        super.setScrollPosition(position, positionOffset, updateSelectedTabView, updateIndicatorPosition, alsoUpdateSelectedTab);
        if (updateSelectedTabView && onTabViewsRestyled != null) {
            onTabViewsRestyled.run();
        }
    }

    @Override
    public void selectTab(@Nullable Tab tab, boolean updateIndicator) {
        super.selectTab(tab, updateIndicator);
        if (onTabViewsRestyled != null) {
            onTabViewsRestyled.run();
        }
    }
}
