package net.olejon.mdapp;

/*

Copyright 2015 Ole Jon Bjørkum

This file is part of LegeAppen.

LegeAppen is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

LegeAppen is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LegeAppen.  If not, see <http://www.gnu.org/licenses/>.

*/

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import java.net.URLEncoder;
import java.util.ArrayList;

public class NasjonaleRetningslinjerActivity extends ActionBarActivity
{
    private static final int VOICE_SEARCH_REQUEST_CODE = 1;

    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private InputMethodManager mInputMethodManager;

    private LinearLayout mToolbarSearchLayout;
    private EditText mToolbarSearchEditText;
    private FloatingActionButton mFloatingActionButton;
    private ListView mListView;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Input manager
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Layout
        setContentView(R.layout.activity_nasjonale_retningslinjer);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.nasjonale_retningslinjer_toolbar);
        toolbar.setTitle(getString(R.string.nasjonale_retningslinjer_title));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbarSearchLayout = (LinearLayout) findViewById(R.id.nasjonale_retningslinjer_toolbar_search_layout);
        mToolbarSearchEditText = (EditText) findViewById(R.id.nasjonale_retningslinjer_toolbar_search);

        mToolbarSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                if(i == EditorInfo.IME_ACTION_SEARCH || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                {
                    mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                    search(mToolbarSearchEditText.getText().toString().trim());

                    return true;
                }

                return false;
            }
        });

        ImageButton imageButton = (ImageButton) findViewById(R.id.nasjonale_retningslinjer_toolbar_clear_search);

        imageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mToolbarSearchEditText.setText("");
            }
        });

        // Floating action button
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.nasjonale_retningslinjer_fab);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(mToolbarSearchLayout.getVisibility() == View.VISIBLE)
                {
                    mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                    search(mToolbarSearchEditText.getText().toString().trim());
                }
                else
                {
                    mToolbarSearchLayout.setVisibility(View.VISIBLE);
                    mToolbarSearchEditText.requestFocus();

                    mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.SHOW_IMPLICIT, 0);
                }
            }
        });

        // List
        mListView = (ListView) findViewById(R.id.nasjonale_retningslinjer_list);

        View listViewEmpty = findViewById(R.id.nasjonale_retningslinjer_list_empty);
        mListView.setEmptyView(listViewEmpty);

        View listViewHeader = getLayoutInflater().inflate(R.layout.activity_nasjonale_retningslinjer_list_subheader, mListView, false);
        mListView.addHeaderView(listViewHeader, null, false);
    }

    // Activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == VOICE_SEARCH_REQUEST_CODE && data != null)
        {
            ArrayList<String> voiceSearchArrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            String voiceSearchString = voiceSearchArrayList.get(0);

            search(voiceSearchString);
        }
    }

    // Resume activity
    @Override
    protected void onResume()
    {
        super.onResume();

        getRecentSearches();
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    // Back button
    @Override
    public void onBackPressed()
    {
        if(mToolbarSearchLayout.getVisibility() == View.VISIBLE)
        {
            mToolbarSearchLayout.setVisibility(View.GONE);
            mToolbarSearchEditText.setText("");
        }
        else
        {
            super.onBackPressed();
        }
    }

    // Search button
    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)
    {
        if(keyCode == KeyEvent.KEYCODE_SEARCH)
        {
            mToolbarSearchLayout.setVisibility(View.VISIBLE);
            mToolbarSearchEditText.requestFocus();

            mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.SHOW_IMPLICIT, 0);

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_nasjonale_retningslinjer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            case R.id.nasjonale_retningslinjer_menu_voice_search:
            {
                try
                {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nb-NO");
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
                }
                catch(Exception e)
                {
                    Log.e("NasjonaleRetningslinjer", Log.getStackTraceString(e));
                }

                return true;
            }
            case R.id.nasjonale_retningslinjer_menu_clear_recent_searches:
            {
                clearRecentSearches();
                return true;
            }
            case R.id.nasjonale_retningslinjer_menu_uri:
            {
                mTools.openUri("https://helsedirektoratet.no/retningslinjer#Default=%7B%22k%22%3A%22%22%2C%22r%22%3A%5B%7B%22n%22%3A%22HDDocumentType%22%2C%22t%22%3A%5B%22%5C%22%C7%82%C7%824e61736a6f6e616c65206661676c696765207265746e696e67736c696e6a6572%5C%22%22%2C%22equals(%5C%22Nasjonale%20faglige%20retningslinjer%5C%22)%22%5D%2C%22o%22%3A%22and%22%2C%22k%22%3Afalse%2C%22m%22%3Anull%7D%5D%7D");
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Search
    private void search(String string)
    {
        if(string.equals("")) return;

        ContentValues contentValues = new ContentValues();
        contentValues.put(NasjonaleRetningslinjerSQLiteHelper.COLUMN_STRING, string);

        mSqLiteDatabase.delete(NasjonaleRetningslinjerSQLiteHelper.TABLE, NasjonaleRetningslinjerSQLiteHelper.COLUMN_STRING+" = "+mTools.sqe(string)+" COLLATE NOCASE", null);
        mSqLiteDatabase.insert(NasjonaleRetningslinjerSQLiteHelper.TABLE, null, contentValues);

        try
        {
            Intent intent = new Intent(mContext, NasjonaleRetningslinjerWebViewActivity.class);
            intent.putExtra("search", string);
            intent.putExtra("uri", "https://helsedirektoratet.no/retningslinjer#Default=%7B%22k%22%3A%22"+URLEncoder.encode(string.toLowerCase(), "utf-8")+"%22%2C%22r%22%3A%5B%7B%22n%22%3A%22HDDocumentType%22%2C%22t%22%3A%5B%22%5C%22%C7%82%C7%824e61736a6f6e616c65206661676c696765207265746e696e67736c696e6a6572%5C%22%22%2C%22equals(%5C%22Nasjonale%20faglige%20retningslinjer%5C%22)%22%5D%2C%22o%22%3A%22and%22%2C%22k%22%3Afalse%2C%22m%22%3Anull%7D%5D%7D");
            startActivity(intent);
        }
        catch(Exception e)
        {
            Log.e("NasjonaleRetningslinjer", Log.getStackTraceString(e));
        }
    }

    private void getRecentSearches()
    {
        GetRecentSearchesTask getRecentSearchesTask = new GetRecentSearchesTask();
        getRecentSearchesTask.execute();
    }

    private void clearRecentSearches()
    {
        mSqLiteDatabase.delete(NasjonaleRetningslinjerSQLiteHelper.TABLE, null, null);

        mTools.showToast(getString(R.string.nasjonale_retningslinjer_recent_searches_removed), 0);

        getRecentSearches();
    }

    private class GetRecentSearchesTask extends AsyncTask<Void, Void, SimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(SimpleCursorAdapter simpleCursorAdapter)
        {
            mListView.setAdapter(simpleCursorAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                {
                    int index = i - 1;

                    if(mCursor.moveToPosition(index)) search(mCursor.getString(mCursor.getColumnIndexOrThrow(NasjonaleRetningslinjerSQLiteHelper.COLUMN_STRING)));
                }
            });

            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fab);
            mFloatingActionButton.startAnimation(animation);

            mFloatingActionButton.setVisibility(View.VISIBLE);

            if(mCursor.getCount() > 0)
            {
                Handler handler = new Handler();

                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mToolbarSearchLayout.setVisibility(View.VISIBLE);
                        mToolbarSearchEditText.requestFocus();

                        mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.SHOW_IMPLICIT, 0);
                    }
                }, 500);
            }
        }

        @Override
        protected SimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new NasjonaleRetningslinjerSQLiteHelper(mContext).getWritableDatabase();

            mCursor = mSqLiteDatabase.query(NasjonaleRetningslinjerSQLiteHelper.TABLE, null, null, null, null, null, NasjonaleRetningslinjerSQLiteHelper.COLUMN_ID+" DESC LIMIT 10");

            String[] fromColumns = {NasjonaleRetningslinjerSQLiteHelper.COLUMN_STRING};
            int[] toViews = {R.id.nasjonale_retningslinjer_list_item_string};

            return new SimpleCursorAdapter(mContext, R.layout.activity_nasjonale_retningslinjer_list_item, mCursor, fromColumns, toViews, 0);
        }
    }
}