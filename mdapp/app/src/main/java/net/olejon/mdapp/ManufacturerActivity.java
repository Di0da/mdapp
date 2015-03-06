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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class ManufacturerActivity extends ActionBarActivity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private Toolbar mToolbar;
    private ListView mListView;

    private String manufacturerUri;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Intent
        Intent intent = getIntent();
        final long manufacturerId = intent.getLongExtra("id", 0);

        // Layout
        setContentView(R.layout.activity_manufacturer);

        // Toolbar
        mToolbar = (Toolbar) findViewById(R.id.manufacturer_toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // List
        mListView = (ListView) findViewById(R.id.manufacturer_list);

        View listViewHeader = getLayoutInflater().inflate(R.layout.activity_manufacturer_list_header, mListView, false);
        mListView.addHeaderView(listViewHeader, null, false);

        // Get manufacturer
        //GetManufacturerTask getManufacturerTask = new GetManufacturerTask();
        //getManufacturerTask.execute(manufacturerId);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_manufacturer, menu);
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
            case R.id.manufacturer_menu_uri:
            {
                mTools.openUri(manufacturerUri);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Get manufacturer
    /*private class GetManufacturerTask extends AsyncTask<Long, Void, HashMap<String, String>>
    {
        @Override
        protected void onPostExecute(HashMap<String, String> manufacturer)
        {
            final String manufacturerName = manufacturer.get(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_NAME);
            final String manufacturerInformation = manufacturer.get(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_INFORMATION);
            final String manufacturerMedications = manufacturer.get(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_MEDICATIONS);
            final String manufacturerMedicationsCount = manufacturer.get(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_MEDICATIONS_COUNT);

            manufacturerUri = manufacturer.get(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_URI);

            mToolbar.setTitle(manufacturerName);

            TextView textView = (TextView) findViewById(R.id.manufacturer_medications_count);
            textView.setText(manufacturerMedicationsCount+" - "+getString(R.string.manufacturer_source));

            textView = (TextView) findViewById(R.id.manufacturer_information);
            textView.setText(Html.fromHtml(manufacturerInformation));
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            if(!manufacturerMedications.equals(""))
            {
                try
                {
                    final JSONArray medicationsJsonArray = new JSONArray(manufacturerMedications);

                    String[] fromColumns = new String[] {"name"};
                    int[] toViews = new int[] {R.id.manufacturer_list_item_name};

                    ArrayList<HashMap<String, String>> medications = new ArrayList<>();

                    for(int i = 0; i < medicationsJsonArray.length(); i++)
                    {
                        HashMap<String, String> medication = new HashMap<>();

                        JSONObject jsonObject = medicationsJsonArray.getJSONObject(i);

                        medication.put("name", jsonObject.getString("name"));

                        medications.add(medication);
                    }

                    SimpleAdapter simpleAdapter = new SimpleAdapter(mContext, medications, R.layout.activity_manufacturer_list_item, fromColumns, toViews);

                    mListView.setAdapter(simpleAdapter);

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                    {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                        {
                            try
                            {
                                int index = i - 1;

                                String uri = medicationsJsonArray.getJSONObject(index).getString("uri");

                                mTools.getMedicationWithFullContent(uri);
                            }
                            catch(Exception e)
                            {
                                mTools.showToast(getString(R.string.manufacturer_could_not_find_medication), 1);

                                Log.e("ManufacturerActivity", Log.getStackTraceString(e));
                            }
                        }
                    });
                }
                catch(Exception e)
                {
                    Log.e("ManufacturerActivity", Log.getStackTraceString(e));
                }
            }
        }

        @Override
        protected HashMap<String, String> doInBackground(Long... longs)
        {
            SQLiteDatabase sqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            Cursor cursor = sqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, null, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_ID+" = "+longs[0], null, null, null, null);

            String[] columns = cursor.getColumnNames();

            HashMap<String, String> manufacturer = new HashMap<>();

            if(cursor.moveToFirst())
            {
                for(String column : columns)
                {
                    try
                    {
                        manufacturer.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
                    }
                    catch(Exception e)
                    {
                        Log.e("ManufacturerActivity", Log.getStackTraceString(e));
                    }
                }
            }

            cursor.close();
            sqLiteDatabase.close();

            return manufacturer;
        }
    }*/
}