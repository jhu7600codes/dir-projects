package io.github.gohoski.notpipe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.ui.AutoCompleteAdapter;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 28.10.2026.
 * YouTube-style search page: opened from the search button in the top bar, shows recent searches
 * (or live suggestions while typing) and hands the query back to MainActivity.
 */

public class SearchActivity extends Activity {
    public static final String EXTRA_QUERY = "query";

    private EditText field;
    private AutoCompleteAdapter suggestionSource;
    private final List<String> items = new ArrayList<String>();
    private final List<Boolean> recent = new ArrayList<Boolean>();
    private ArrayAdapter<String> adapter;

    /**
     * Opens the search page.
     * @param query text to start with, may be null
     */
    public static void open(Activity activity, String query) {
        Intent intent = new Intent(activity, SearchActivity.class);
        intent.putExtra(EXTRA_QUERY, query);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle(R.string.search);
        field = (EditText) findViewById(R.id.search_query);
        ListView list = (ListView) findViewById(R.id.search_suggestions);

        final float density = getResources().getDisplayMetrics().density;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextSize(16);
                row.setCompoundDrawablesWithIntrinsicBounds(recent.get(position).booleanValue()
                        ? android.R.drawable.ic_menu_recent_history : android.R.drawable.ic_menu_search, 0, 0, 0);
                row.setCompoundDrawablePadding((int) (8 * density));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                submit(items.get(position));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                field.setText(items.get(position));
                field.setSelection(field.getText().length());
                return true;
            }
        });

        suggestionSource = new AutoCompleteAdapter(this);
        suggestionSource.setOnSuggestionsLoadedListener(new AutoCompleteAdapter.OnSuggestionsLoadedListener() {
            @Override
            public void onSuggestionsLoaded() {
                if (field.getText().toString().trim().length() == 0) return;
                List<String> mine = LocalLibrary.getInstance().findSearches(field.getText().toString().trim());
                items.clear();
                recent.clear();
                for (int i = 0; i < suggestionSource.getCount(); i++) {
                    String s = suggestionSource.getItem(i);
                    items.add(s);
                    recent.add(Boolean.valueOf(mine.contains(s)));
                }
                adapter.notifyDataSetChanged();
            }
        });

        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (text.length() == 0) {
                    showRecent();
                } else {
                    showMatchingRecent(text);
                    suggestionSource.requestSuggestions(text);
                }
            }
        });
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submit(field.getText().toString());
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.search_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit(field.getText().toString());
            }
        });

        String query = getIntent().getStringExtra(EXTRA_QUERY);
        if (query != null && query.length() > 0) {
            field.setText(query);
            field.setSelection(query.length());
        } else {
            showRecent();
        }
        field.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void showRecent() {
        items.clear();
        recent.clear();
        List<String> searches = LocalLibrary.getInstance().findSearches("");
        for (int i = 0; i < searches.size(); i++) {
            items.add(searches.get(i));
            recent.add(Boolean.TRUE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showMatchingRecent(String text) {
        items.clear();
        recent.clear();
        List<String> searches = LocalLibrary.getInstance().findSearches(text);
        for (int i = 0; i < searches.size() && i < 5; i++) {
            items.add(searches.get(i));
            recent.add(Boolean.TRUE);
        }
        adapter.notifyDataSetChanged();
    }

    private void submit(String query) {
        query = query.trim();
        if (query.length() == 0) return;
        suggestionSource.setSearchActive(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_QUERY, query);
        startActivity(intent);
        finish();
    }
}
