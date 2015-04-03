package com.sahil_madan.guitarprotabs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class StartSearch extends ActionBarActivity {

    public static final String EXTRA_SEARCH_INPUT = "com.sahil_madan.guitarprotabs.SEARCH_INPUT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        EditText searchbar = (EditText) findViewById(R.id.gp_searchbar);
        searchbar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    executeSearch();
                    return true;
                }
                return false;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSearchClicked(View view)
    {
        executeSearch();
    }

    public void executeSearch() {
        EditText searchbar = (EditText) findViewById(R.id.gp_searchbar);
        String search_input = searchbar.getText().toString();

        if (search_input.isEmpty()) {
            Toast.makeText(this, getString(R.string.start_screen_empty_search_warn), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, SearchResultsAndDownload.class);
        intent.putExtra(EXTRA_SEARCH_INPUT, search_input);
        startActivity(intent);
    }
}
