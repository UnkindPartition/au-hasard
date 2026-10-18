package info.ro_che.auhasard;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The result area of a tab (layout/result.xml): either a single value, set large in italic and
 * centered, with a caption, or a sequence as running text.
 */
final class ResultArea {
    private static final float SINGLE_MAX_SP = 170;
    private static final float SINGLE_MIN_SP = 40;

    private final LinearLayout box;
    private final TextView text;
    private final View caption;
    private final TextView captionText;
    private final View captionU;
    private final TextView captionArgs;
    private final Typeface italic;
    private final Typeface regular;

    private boolean single;
    private float sequenceSp;

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
        return text.length() == 0;
    }

    CharSequence text() {
        return text.getText();
    }

    /**
     * Shows a single value with a caption; with non-null {@code args}, the caption ends in
     * "𝒰" followed by them.
     */
    void showSingle(CharSequence value, CharSequence captionPrefix, CharSequence args) {
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

    void showSequence(CharSequence value, float sp) {
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
        float fit = SINGLE_MAX_SP * Math.min(1, 0.95f * width / paint.measureText(text.getText().toString()));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(SINGLE_MIN_SP, fit));
    }

    void save(Bundle state, String key) {
        state.putCharSequence(key + ".text", text.getText());
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
            showSingle(value, state.getCharSequence(key + ".caption"), state.getCharSequence(key + ".args"));
        } else {
            showSequence(value, state.getFloat(key + ".sp"));
        }
    }
}
