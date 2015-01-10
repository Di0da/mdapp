package net.olejon.mdapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

public class ManufacturersActivity extends ActionBarActivity
{
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

        // Layout
        setContentView(R.layout.activity_manufacturers);

        // Input manager
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.manufacturers_toolbar);
        toolbar.setTitle(getString(R.string.manufacturers_title));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbarSearchLayout = (LinearLayout) findViewById(R.id.manufacturers_toolbar_search_layout);
        mToolbarSearchEditText = (EditText) findViewById(R.id.manufacturers_toolbar_search);

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

        ImageButton imageButton = (ImageButton) findViewById(R.id.manufacturers_toolbar_clear_search);

        imageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mToolbarSearchEditText.setText("");
            }
        });

        // Floating action button
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.manufacturers_fab);

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
        mListView = (ListView) findViewById(R.id.manufacturers_list);

        // Get manufacturers
        GetManufacturersTask getManufacturersTask = new GetManufacturersTask();
        getManufacturersTask.execute();
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
        getMenuInflater().inflate(R.menu.menu_manufacturers, menu);
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
            case R.id.manufacturers_menu_uri:
            {
                mTools.openUri("http://www.felleskatalogen.no/medisin/firmaer/alle");
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get manufacturers
    private class GetManufacturersTask extends AsyncTask<Void, Void, SimpleCursorAdapter>
    {
        @Override
        protected void onPostExecute(final SimpleCursorAdapter simpleCursorAdapter)
        {
            mListView.setAdapter(simpleCursorAdapter);

            View listViewEmpty = findViewById(R.id.manufacturers_list_empty);
            mListView.setEmptyView(listViewEmpty);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id)
                {
                    Intent intent = new Intent(mContext, ManufacturerActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);
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
                        String[] queryColumns = {FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_ID, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_MEDICATIONS_COUNT};

                        if(charSequence.length() == 0) return mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, queryColumns, null, null, null, null, null);

                        String query = charSequence.toString().trim();

                        return mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, queryColumns, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_NAME+" LIKE '%"+query.replace("'", "")+"%'", null, null, null, null);
                    }

                    return null;
                }
            });

            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fab);
            mFloatingActionButton.startAnimation(animation);

            mFloatingActionButton.setVisibility(View.VISIBLE);
        }

        @Override
        protected SimpleCursorAdapter doInBackground(Void... voids)
        {
            mSqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            String[] queryColumns = {FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_ID, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_MEDICATIONS_COUNT};
            mCursor = mSqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, queryColumns, null, null, null, null, null);

            String[] fromColumns = {FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_NAME, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_MEDICATIONS_COUNT};
            int[] toViews = {R.id.manufacturers_list_item_name, R.id.manufacturers_list_item_medications_count};

            return new SimpleCursorAdapter(mContext, R.layout.activity_manufacturers_list_item, mCursor, fromColumns, toViews, 0);
        }
    }
}