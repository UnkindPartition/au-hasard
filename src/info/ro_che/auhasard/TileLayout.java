package info.ro_che.auhasard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A tile with an icon above a label, centered together. When the tile is too short for both,
 * the label is hidden so that the icon stays centered.
 */
public final class TileLayout extends LinearLayout {
    public TileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Screen readers announce the label even when it's hidden.
        setContentDescription(label().getText());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        View icon = getChildAt(0);
        TextView label = label();
        label.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int needed =
                getPaddingTop()
                        + icon.getMeasuredHeight()
                        + ((LayoutParams) label.getLayoutParams()).topMargin
                        + label.getMeasuredHeight()
                        + getPaddingBottom();
        int visibility = needed <= getMeasuredHeight() ? VISIBLE : GONE;
        if (label.getVisibility() != visibility) {
            label.setVisibility(visibility);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private TextView label() {
        return (TextView) getChildAt(1);
    }
}
