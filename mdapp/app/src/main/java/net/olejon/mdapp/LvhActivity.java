package net.olejon.mdapp;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

public class LvhActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Connected?
        if(!mTools.isDeviceConnected())
        {
            mTools.showToast(getString(R.string.device_not_connected), 1);

            finish();

            return;
        }

        // Layout
        setContentView(R.layout.activity_lvh);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.lvh_toolbar);
        toolbar.setTitle(getString(R.string.lvh_title));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.lvh_toolbar_progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        // Refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.lvh_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent_blue, R.color.accent_green, R.color.accent_purple, R.color.accent_orange);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                getCategories(false);
            }
        });

        // Recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.lvh_cards);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(new LvhAdapter(mContext, new JSONArray()));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        // Get categories
        getCategories(true);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_lvh, menu);
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
            case R.id.lvh_menu_uri:
            {
                mTools.openUri("http://www.lvh.no/");
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get categories
    private void getCategories(boolean cache)
    {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);

        String apiUri = getString(R.string.project_website)+"api/1/lvh/";

        if(!cache) requestQueue.getCache().remove(apiUri);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(apiUri, new Response.Listener<JSONArray>()
        {
            @Override
            public void onResponse(JSONArray response)
            {
                mRecyclerView.setAdapter(new LvhAdapter(mContext, response));

                mProgressBar.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                mTools.showToast(getString(R.string.lvh_could_not_load_lvh), 1);

                mProgressBar.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                finish();
            }
        });

        requestQueue.add(jsonArrayRequest);
    }
}