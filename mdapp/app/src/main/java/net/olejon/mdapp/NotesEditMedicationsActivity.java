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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

public class NotesEditMedicationsActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private SQLiteDatabase mSqLiteDatabase;
    private Cursor mCursor;

    private InputMethodManager mInputMethodManager;

    private LinearLayout mToolbarSearchLayout;
    private EditText mToolbarSearchEditText;
    private FloatingActionButton mFloatingActionButton;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Input manager
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Layout
        setContentView(R.layout.activity_notes_edit_medications);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.notes_edit_medications_toolbar);
        toolbar.setTitle(getString(R.string.notes_edit_medications_title));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbarSearchLayout = (LinearLayout) findViewById(R.id.notes_edit_medications_toolbar_search_layout);
        mToolbarSearchEditText = (EditText) findViewById(R.id.notes_edit_medications_toolbar_search);

        mToolbarSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent)
            {
                if(i == EditorInfo.IME_ACTION_DONE || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                {
                    mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                    return true;
                }

                return false;
            }
        });

        ImageButton imageButton = (ImageButton) findViewById(R.id.notes_edit_medications_toolbar_clear_search);

        imageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mToolbarSearchEditText.setText("");
            }
        });

        // Floating action button
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.notes_edit_medications_fab);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mToolbarSearchLayout.setVisibility(View.VISIBLE);
                mToolbarSearchEditText.requestFocus();

                mInputMethodManager.toggleSoftInputFromWindow(mToolbarSearchEditText.getApplicationWindowToken(), InputMethodManager.SHOW_IMPLICIT, 0);
            }
        });

        // List
        mListView = (ListView) findViewById(R.id.notes_edit_medications_list);

        // Get medications
        //GetMedicationsTask getMedicationsTask = new GetMedicationsTask();
        //getMedicationsTask.execute();
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCursor != null && !mCursor.isClosed()) mCursor.close();
        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get medications
    /*private class GetMedicationsTask extends AsyncTask<Void, Void, SimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(final SimpleCursorAdapter simpleCursorAdapter)
        {
            mListView.setAdapter(simpleCursorAdapter);

            View listViewEmpty = findViewById(R.id.notes_edit_medications_list_empty);
            mListView.setEmptyView(listViewEmpty);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id)
                {
                    String[] queryColumns = {FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI};
                    mCursor = mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, queryColumns, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID+" = "+id, null, null, null, null);

                    if(mCursor.moveToFirst())
                    {
                        String name = mCursor.getString(mCursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME));
                        String manufacturer = mCursor.getString(mCursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER));
                        String uri = mCursor.getString(mCursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI));

                        Intent intent = new Intent();
                        intent.putExtra("name", name);
                        intent.putExtra("manufacturer", manufacturer);
                        intent.putExtra("uri", uri);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
            });

            mToolbarSearchEditText.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
                {
                    simpleCursorAdapter.getFilter().filter(charSequence);
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

                @Override
                public void afterTextChanged(Editable editable) { }
            });

            simpleCursorAdapter.setFilterQueryProvider(new FilterQueryProvider()
            {
                @Override
                public Cursor runQuery(CharSequence charSequence)
                {
                    if(mSqLiteDatabase != null)
                    {
                        String[] queryColumns = {FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI};

                        if(charSequence.length() == 0) return mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, queryColumns, null, null, null, null, null);

                        String query = charSequence.toString().trim();

                        return mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, queryColumns, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME+" LIKE '%"+query.replace("'", "")+"%'", null, null, null, null);
                    }

                    return null;
                }
            });

            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fab);
            mFloatingActionButton.startAnimation(animation);

            mFloatingActionButton.setVisibility(View.VISIBLE);

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

        @Override
        protected SimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            String[] queryColumns = {FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI};
            mCursor = mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, queryColumns, null, null, null, null, null);

            String[] fromColumns = {FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER};
            int[] toViews = {R.id.notes_edit_medications_list_item_name, R.id.notes_edit_medications_list_item_manufacturer};

            return new SimpleCursorAdapter(mContext, R.layout.activity_notes_edit_medications_list_item, mCursor, fromColumns, toViews, 0);
        }
    }*/
}