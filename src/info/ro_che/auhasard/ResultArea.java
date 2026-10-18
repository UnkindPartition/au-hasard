package info.ro_che.auhasard;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Supplier;

/**
 * The result area of a tab (layout/result.xml): either a single value, set large in italic and
 * centered, with a caption, or a sequence as running text.
 */
final class ResultArea {
    private static final float SINGLE_MAX_SP = 170;
    private static final float SINGLE_MIN_SP = 40;

    /** How long each value of the reel is shown before the next one; it slows down to a stop. */
    private static final long[] REEL_MS = {30, 30, 35, 40, 50, 60, 75};
    private static final long LANDING_MS = 180;

    private final LinearLayout box;
    private final TextView text;
    private final View caption;
    private final TextView captionText;
    private final View captionU;
    private final TextView captionArgs;
    private final Typeface italic;
    private final Typeface regular;

    /** The result, which the text shows except while the reel is rolling. */
    private CharSequence value = "";
    private boolean single;
    private float sequenceSp;
    /** Identifies the current reel, so that a superseded one stops. */
    private int reel;

    ResultArea(View root) {
        box = root.findViewById(R.id.result_box);
        text = root.findViewById(R.id.result_text);
        caption = root.findViewById(R.id.result_caption);
        captionText = root.findViewById(R.id.caption_text);
        captionU = root.findViewById(R.id.caption_u);
        captionArgs = root.findViewById(R.id.caption_args);
        italic = root.getResources().getFont(R.font.lm_roman_italic);
        regular = root.getResources().getFont(R.font.lm_roman);
    }

    boolean isEmpty() {
        return value.length() == 0;
    }

    CharSequence text() {
        return value;
    }

    /**
     * Shows a single value with a caption; with non-null {@code args}, the caption ends in
     * "𝒰" followed by them. Before landing on the value, a reel scrolls through a few others
     * from {@code others}, so that each draw is visible even when the value repeats.
     */
    void showSingle(
            CharSequence value,
            CharSequence captionPrefix,
            CharSequence args,
            Supplier<CharSequence> others) {
        // Announced once it lands, rather than each value of the reel.
        text.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
        setSingle(value, captionPrefix, args);
        roll(others, 0, ++reel);
    }

    void showSequence(CharSequence value, float sp) {
        setSequence(value, sp);
    }

    private void setSingle(CharSequence value, CharSequence captionPrefix, CharSequence args) {
        stopReel();
        this.value = value;
        single = true;
        text.setText(value);
        text.setTypeface(italic);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setLineSpacing(0, 1);
        box.setGravity(Gravity.CENTER);
        captionText.setText(captionPrefix);
        captionU.setVisibility(args == null ? View.GONE : View.VISIBLE);
        captionArgs.setVisibility(args == null ? View.GONE : View.VISIBLE);
        captionArgs.setText(args);
        caption.setVisibility(View.VISIBLE);
        fitSingle();
    }

    private void setSequence(CharSequence value, float sp) {
        stopReel();
        text.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        this.value = value;
        single = false;
        sequenceSp = sp;
        text.setText(value);
        text.setTypeface(regular);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        text.setGravity(Gravity.START);
        text.setLineSpacing(0, 1.3f);
        box.setGravity(Gravity.TOP);
        caption.setVisibility(View.GONE);
    }

    /** Shows step {@code step} of the reel: another value, or finally the result. */
    private void roll(Supplier<CharSequence> others, int step, int id) {
        if (id != reel) {
            return;
        }
        boolean landing = step == REEL_MS.length;
        if (step == 0) {
            // The reel's values are shown faded, at the result's size, on one line, with the
            // caption out of the way.
            text.setMaxLines(1);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setAlpha(0.5f);
            caption.animate().cancel();
            caption.setAlpha(0);
        }
        text.setText(landing ? value : others.get());
        // Each value falls through the whole result area, entering at the top edge and leaving
        // at the bottom; the result drops into place instead, overshooting a little as it stops.
        View area = (View) box.getParent();
        float top = box.getTop() + text.getTop() - area.getScrollY();
        text.animate().cancel();
        text.setTranslationY(-(top + text.getHeight()));
        text.animate()
                .translationY(landing ? 0 : area.getHeight() - top)
                .setDuration(landing ? LANDING_MS : REEL_MS[step])
                .setInterpolator(landing ? new OvershootInterpolator(1.2f) : new LinearInterpolator());
        if (landing) {
            stopReel();
            caption.animate().alpha(1).setDuration(LANDING_MS);
            text.announceForAccessibility(value);
        } else {
            text.postDelayed(() -> roll(others, step + 1, id), REEL_MS[step]);
        }
    }

    /** Puts the text back to normal, keeping a landing animation going. */
    private void stopReel() {
        reel++;
        text.setAlpha(1);
        text.setMaxLines(Integer.MAX_VALUE);
        text.setEllipsize(null);
    }

    /** Sizes a single value to fit on one line if possible, within the size limits. */
    private void fitSingle() {
        int width = text.getWidth() - text.getTotalPaddingLeft() - text.getTotalPaddingRight();
        if (width <= 0) {
            // Not laid out yet.
            text.post(this::fitSingle);
            return;
        }
        float density = text.getResources().getDisplayMetrics().scaledDensity;
        Paint paint = new Paint(text.getPaint());
        paint.setTextSize(SINGLE_MAX_SP * density);
        // A little slack for the overhang of italic glyphs.
        float fit = SINGLE_MAX_SP * Math.min(1, 0.95f * width / paint.measureText(value.toString()));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(SINGLE_MIN_SP, fit));
    }

    void save(Bundle state, String key) {
        state.putCharSequence(key + ".text", value);
        state.putBoolean(key + ".single", single);
        state.putFloat(key + ".sp", sequenceSp);
        state.putCharSequence(key + ".caption", captionText.getText());
        state.putCharSequence(
                key + ".args", captionArgs.getVisibility() == View.VISIBLE ? captionArgs.getText() : null);
    }

    void restore(Bundle state, String key) {
        CharSequence value = state.getCharSequence(key + ".text");
        if (value == null || value.length() == 0) {
            return;
        }
        if (state.getBoolean(key + ".single")) {
            setSingle(value, state.getCharSequence(key + ".caption"), state.getCharSequence(key + ".args"));
        } else {
            setSequence(value, state.getFloat(key + ".sp"));
        }
    }
}
