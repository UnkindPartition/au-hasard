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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        if (layout != null) {
            int top = getExtendedPaddingTop();
            int lineHeight = getLineHeight();
            int bottom = getScrollY() + getHeight();
            for (int y = top + lineHeight; y < bottom; y += lineHeight) {
                canvas.drawLine(0, y, getWidth(), y, rulePaint);
            }

            Editable text = getText();
            int lines = layout.getLineCount();
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
