package info.ro_che.auhasard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private final Draws draws = Draws.fromKernel();

    private SharedPreferences prefs;
    private File savedListsFile;
    private int tab;
    private GestureDetector swipes;
    private boolean swiped;

    private TextView[] tabs;
    private View[] panels;
    private WebView about;
    private EditText minInput;
    private EditText maxInput;
    private TextView numbersNote;
    private ResultArea numbersResult;
    private EditText listInput;
    private View copyButton;
    private ResultArea listResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        savedListsFile = new File(getFilesDir(), "saved_lists.txt");

        tabs = new TextView[] {
            findViewById(R.id.tab_numbers), findViewById(R.id.tab_lists), findViewById(R.id.tab_about)
        };
        about = findViewById(R.id.about_panel);
        panels = new View[] {findViewById(R.id.numbers_panel), findViewById(R.id.lists_panel), about};
        minInput = findViewById(R.id.min);
        maxInput = findViewById(R.id.max);
        numbersNote = findViewById(R.id.numbers_note);
        numbersResult = new ResultArea(findViewById(R.id.numbers_result));
        listInput = findViewById(R.id.list_input);
        copyButton = findViewById(R.id.copy);
        listResult = new ResultArea(findViewById(R.id.list_result));

        // Keep content clear of the system bars and the keyboard.
        findViewById(R.id.root)
                .setOnApplyWindowInsetsListener(
                        (v, insets) -> {
                            Insets i =
                                    Build.VERSION.SDK_INT >= 30
                                            ? insets.getInsets(
                                                    WindowInsets.Type.systemBars()
                                                            | WindowInsets.Type.ime())
                                            : insets.getSystemWindowInsets();
                            v.setPadding(i.left, i.top, i.right, i.bottom);
                            return insets;
                        });

        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            tabs[i].setOnClickListener(v -> switchTab(t));
        }
        swipes = new GestureDetector(this, new SwipeListener());
        findViewById(R.id.draw_number).setOnClickListener(v -> drawNumber());
        findViewById(R.id.shuffle_numbers).setOnClickListener(v -> shuffleNumbers());
        findViewById(R.id.paste).setOnClickListener(v -> paste());
        copyButton.setOnClickListener(v -> copy());
        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.saved).setOnClickListener(v -> showSaved());
        findViewById(R.id.draw_item).setOnClickListener(v -> drawItem());
        findViewById(R.id.shuffle_items).setOnClickListener(v -> shuffleItems());

        minInput.setText(prefs.getString("min", "1"));
        maxInput.setText(prefs.getString("max", "100"));
        listInput.setText(prefs.getString("list", ""));
        copyButton.setEnabled(false);
        showTab(prefs.getInt("tab", 0));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", tab);
        state.putBoolean("noteVisible", numbersNote.getVisibility() == View.VISIBLE);
        numbersResult.save(state, "numbers");
        listResult.save(state, "list");
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        showTab(state.getInt("tab"));
        numbersNote.setVisibility(state.getBoolean("noteVisible") ? View.VISIBLE : View.GONE);
        numbersResult.restore(state, "numbers");
        listResult.restore(state, "list");
        copyButton.setEnabled(!listResult.isEmpty());
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.edit()
                .putString("min", minInput.getText().toString())
                .putString("max", maxInput.getText().toString())
                .putString("list", listInput.getText().toString())
                .putInt("tab", tab)
                .apply();
    }

    /** Fling left or right anywhere to switch tabs. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        swiped = false;
        swipes.onTouchEvent(event);
        if (swiped) {
            // Don't let the view under the finger treat the swipe as a tap.
            event.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(event);
    }

    private final class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent start, MotionEvent end, float vx, float vy) {
            if (start == null) {
                return false;
            }
            float dx = end.getX() - start.getX();
            float dy = end.getY() - start.getY();
            ViewConfiguration config = ViewConfiguration.get(MainActivity.this);
            if (Math.abs(dx) < 3 * Math.abs(dy)
                    || Math.abs(dx) < 4 * config.getScaledTouchSlop()
                    || Math.abs(vx) < 3 * config.getScaledMinimumFlingVelocity()) {
                return false;
            }
            int target = tab + (dx < 0 ? 1 : -1);
            if (target >= 0 && target < tabs.length) {
                swiped = true;
                switchTab(target);
            }
            return swiped;
        }
    }

    /** Shows a tab, sliding it in from the side it's on. */
    private void switchTab(int t) {
        if (t == tab) {
            return;
        }
        View in = panels[t];
        View out = panels[tab];
        float width = ((View) in.getParent()).getWidth();
        float direction = t > tab ? 1 : -1;
        in.animate().cancel();
        out.animate().cancel();
        showTab(t);
        out.setVisibility(View.VISIBLE);
        in.setTranslationX(direction * width);
        out.setTranslationX(0);
        in.animate().translationX(0).setDuration(200);
        out.animate()
                .translationX(-direction * width)
                .setDuration(200)
                .withEndAction(
                        () -> {
                            out.setVisibility(View.GONE);
                            out.setTranslationX(0);
                        });
    }

    private void showTab(int t) {
        tab = t;
        for (int i = 0; i < panels.length; i++) {
            panels[i].animate().cancel();
            panels[i].setVisibility(i == t ? View.VISIBLE : View.GONE);
            panels[i].setTranslationX(0);
            tabs[i].setSelected(i == t);
        }
        if (panels[t] == about && about.getUrl() == null) {
            loadAbout();
        }
    }

    /** Loads the About page; its links open in the browser. */
    private void loadAbout() {
        about.setBackgroundColor(0);
        about.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                        } catch (ActivityNotFoundException e) {
                            // No browser installed.
                        }
                        return true;
                    }
                });
        about.loadUrl("file:///android_asset/" + getString(R.string.about_page));
    }

    // Numbers.

    private void drawNumber() {
        long tap = System.nanoTime();
        long[] range = readRange();
        if (range == null) {
            return;
        }
        long v = draws.number(range[0], range[1], tap);
        numbersNote.setVisibility(View.GONE);
        numbersResult.showSingle(
                String.valueOf(v),
                getString(R.string.drawn_from),
                "(" + range[0] + ", " + range[1] + ")",
                () -> String.valueOf(draws.reelNumber(range[0], range[1])));
    }

    private void shuffleNumbers() {
        long tap = System.nanoTime();
        long[] range = readRange();
        if (range == null) {
            return;
        }
        long[] v = draws.shuffledNumbers(range[0], range[1], tap);
        BigInteger size =
                BigInteger.valueOf(range[1])
                        .subtract(BigInteger.valueOf(range[0]))
                        .add(BigInteger.ONE);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(v[i]);
        }
        if (BigInteger.valueOf(v.length).compareTo(size) < 0) {
            numbersNote.setText(getString(R.string.shuffle_capped, v.length, size.toString()));
            numbersNote.setVisibility(View.VISIBLE);
        } else {
            numbersNote.setVisibility(View.GONE);
        }
        numbersResult.showSequence(text, 22);
    }

    /** The range entered by the user, or null after flagging an error on the input. */
    private long[] readRange() {
        Long min = readNumber(minInput);
        Long max = readNumber(maxInput);
        if (min == null || max == null) {
            return null;
        }
        if (min > max) {
            maxInput.setError(getString(R.string.error_max_below_min));
            return null;
        }
        return new long[] {min, max};
    }

    private Long readNumber(EditText input) {
        try {
            return Long.parseLong(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            input.setError(getString(R.string.error_number));
            return null;
        }
    }

    // Lists.

    private void drawItem() {
        long tap = System.nanoTime();
        List<String> items = readItems();
        if (items == null) {
            return;
        }
        listResult.showSingle(
                draws.item(items, tap),
                getResources().getQuantityString(R.plurals.drawn_from_items, items.size(), items.size()),
                null,
                () -> draws.reelItem(items));
        copyButton.setEnabled(true);
    }

    private void shuffleItems() {
        long tap = System.nanoTime();
        List<String> items = readItems();
        if (items == null) {
            return;
        }
        List<String> shuffled = draws.shuffledItems(items, tap);
        // Each item preceded by its position, small and in the accent color.
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (int i = 0; i < shuffled.size(); i++) {
            if (i > 0) {
                text.append('\n');
            }
            int start = text.length();
            text.append(String.valueOf(i + 1));
            text.setSpan(
                    new ForegroundColorSpan(getColor(R.color.accent)),
                    start,
                    text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(
                    new RelativeSizeSpan(0.75f), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.append('\u2002').append(shuffled.get(i));
        }
        listResult.showSequence(text, 20);
        copyButton.setEnabled(true);
    }

    /** The items of the list, or null after flagging that it is empty. */
    private List<String> readItems() {
        List<String> items = Lists.items(listInput.getText().toString());
        if (items.isEmpty()) {
            listInput.setError(getString(R.string.error_empty_list));
            return null;
        }
        return items;
    }

    private void copy() {
        getSystemService(ClipboardManager.class)
                .setPrimaryClip(
                        ClipData.newPlainText(getString(R.string.app_name), listResult.text().toString()));
        // Android 13+ confirms copies itself.
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void paste() {
        ClipData clip = getSystemService(ClipboardManager.class).getPrimaryClip();
        CharSequence text = clip == null || clip.getItemCount() == 0
                ? null
                : clip.getItemAt(0).coerceToText(this);
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        listInput.setText(text);
        listInput.setError(null);
    }

    private void save() {
        List<String> items = readItems();
        if (items == null) {
            return;
        }
        List<List<String>> lists = loadSaved();
        if (lists == null) {
            return;
        }
        if (lists.contains(items)) {
            Toast.makeText(this, R.string.already_saved, Toast.LENGTH_SHORT).show();
            return;
        }
        lists.add(0, items);
        if (storeSaved(lists)) {
            Toast.makeText(this, R.string.list_saved, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSaved() {
        List<List<String>> lists = loadSaved();
        if (lists == null) {
            return;
        }
        if (lists.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.saved_lists)
                    .setMessage(R.string.no_saved_lists)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.saved_item, previews(lists));
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.saved_lists)
                        .setAdapter(
                                adapter,
                                (d, which) -> {
                                    listInput.setText(TextUtils.join("\n", lists.get(which)));
                                    listInput.setError(null);
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
        dialog.getListView()
                .setOnItemLongClickListener(
                        (parent, view, position, id) -> {
                            confirmDelete(dialog, adapter, lists, position);
                            return true;
                        });
    }

    private void confirmDelete(
            AlertDialog dialog,
            ArrayAdapter<String> adapter,
            List<List<String>> lists,
            int position) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_list)
                .setPositiveButton(
                        R.string.delete,
                        (d, which) -> {
                            lists.remove(position);
                            if (!storeSaved(lists)) {
                                return;
                            }
                            adapter.clear();
                            adapter.addAll(previews(lists));
                            if (lists.isEmpty()) {
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static List<String> previews(List<List<String>> lists) {
        List<String> result = new ArrayList<>();
        for (List<String> items : lists) {
            result.add(TextUtils.join(", ", items));
        }
        return result;
    }

    /** The saved lists, or null after reporting an error. */
    private List<List<String>> loadSaved() {
        try {
            return Lists.load(savedListsFile);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_storage, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private boolean storeSaved(List<List<String>> lists) {
        try {
            Lists.store(savedListsFile, lists);
            return true;
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_storage, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
