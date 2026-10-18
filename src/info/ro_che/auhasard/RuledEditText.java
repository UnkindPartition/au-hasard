package info.ro_che.auhasard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * A text field drawn like ruled paper, one rule under each line down to the bottom, with the
 * items (non-blank lines) numbered in the left margin.
 */
public final class RuledEditText extends EditText {
    private final Paint rulePaint = new Paint();
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float numberGap;
    private final float border;
    /** The distance between lines: the android:lineHeight attribute, exactly. */
    private final int pitch;

    public RuledEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        rulePaint.setColor(context.getColor(R.color.rule));
        rulePaint.setStrokeWidth(density);
        numberPaint.setColor(context.getColor(R.color.accent));
        numberPaint.setTypeface(getTypeface());
        numberPaint.setTextSize(getTextSize() * 0.75f);
        numberPaint.setTextAlign(Paint.Align.RIGHT);
        numberGap = 10 * density;
        // The width of the box drawn as the background (drawable/box.xml).
        border = 1.5f * density;
        // android:lineHeight counts the font's line gap, which the layout of the lines doesn't,
        // so for fonts with a line gap the lines would end up closer than requested.
        pitch = getLineHeight();
        Paint.FontMetricsInt fm = getPaint().getFontMetricsInt();
        setLineSpacing(pitch - (fm.descent - fm.ascent), 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        if (layout != null) {
            int top = getExtendedPaddingTop();
            int bottom = getScrollY() + getHeight();
            int lines = layout.getLineCount();
            // Each rule lies under a baseline, midway between the descenders of its line and
            // the ascenders of the next. Below the text, the rules continue at the same pitch.
            Paint.FontMetrics fm = getPaint().getFontMetrics();
            float gap = fm.descent + (pitch - (fm.descent - fm.ascent)) / 2;
            // Keep the rules inside the box.
            canvas.save();
            canvas.clipRect(border, getScrollY() + border, getWidth() - border, bottom - border);
            for (int i = 0; ; i++) {
                float y =
                        i < lines
                                ? top + layout.getLineBaseline(i) + gap
                                : top + layout.getLineBaseline(lines - 1) + gap + (i - lines + 1) * pitch;
                if (y >= bottom) {
                    break;
                }
                canvas.drawLine(0, y, getWidth(), y, rulePaint);
            }
            canvas.restore();

            Editable text = getText();
            float x = getCompoundPaddingLeft() - numberGap;
            int item = 0;
            for (int i = 0; i < lines; i++) {
                int start = layout.getLineStart(i);
                if (start > 0 && text.charAt(start - 1) != '\n') {
                    continue;
                }
                int end = start;
                while (end < text.length() && text.charAt(end) != '\n') {
                    end++;
                }
                if (text.subSequence(start, end).toString().trim().isEmpty()) {
                    continue;
                }
                item++;
                canvas.drawText(
                        String.valueOf(item), x, top + layout.getLineBaseline(i), numberPaint);
            }
        }
        super.onDraw(canvas);
    }
}
