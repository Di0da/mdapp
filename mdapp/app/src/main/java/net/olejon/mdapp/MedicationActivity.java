package net.olejon.mdapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class MedicationActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private SQLiteDatabase mSqLiteDatabase;

    private Spinner mSpinner;
    private ProgressBar mProgressBar;
    private WebView mWebView;

    private Menu mMenu;

    private MenuItem favoriteMenuItem;

    private long medicationId;

    private String medicationName;
    private String medicationManufacturer;
    private String medicationManufacturerUri;
    private String medicationType;
    private String medicationPrescriptionGroup;
    private String medicationPrescriptionGroupDescription;
    private String medicationContentSections;
    private String medicationUri;
    private String medicationPicturesUri;
    private String medicationPatientUri;
    private String medicationSpcUri;

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Settings
        PreferenceManager.setDefaultValues(mContext, R.xml.settings, false);

        // Database
        mSqLiteDatabase = new MedicationsFavoritesSQLiteHelper(mContext).getWritableDatabase();

        // Intent
        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND))
        {
            String uri = intent.getStringExtra(Intent.EXTRA_TEXT).replace("no/medisin/", "no/m/medisin/").replace("?json", "");

            medicationId = mTools.getMedicationIdFromUri(uri);
        }
        else if(intentAction != null && intentAction.equals(Intent.ACTION_VIEW))
        {
            String uri = intent.getData().toString().replace("no/medisin/", "no/m/medisin/").replace("?json", "");

            medicationId = mTools.getMedicationIdFromUri(uri);
        }
        else
        {
            medicationId = intent.getLongExtra("id", 0);
        }

        if(medicationId == 0)
        {
            mTools.showToast(getString(R.string.medication_could_not_find_medication), 1);

            finish();
        }
        else
        {
            // Layout
            setContentView(R.layout.activity_medication);

            // Toolbar
            Toolbar toolbar = (Toolbar) findViewById(R.id.medication_toolbar);

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Spinner
            mSpinner = (Spinner) findViewById(R.id.medication_toolbar_spinner);
            mSpinner.setOnItemSelectedListener(this);

            // Progress bar
            mProgressBar = (ProgressBar) findViewById(R.id.medication_toolbar_progressbar);

            // Web view
            mWebView = (WebView) findViewById(R.id.medication_content);

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(false);

            mWebView.setWebViewClient(new WebViewClient()
            {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    if(url.equals("http://www.felleskatalogen.no/m/medisin/interaksjon"))
                    {
                        Intent intent = new Intent(mContext, InteractionsActivity.class);
                        intent.putExtra("search", medicationName);
                        startActivity(intent);
                    }
                    else if(url.matches("^http://www.felleskatalogen.no/m/medisin/[^-]+-.*?-\\d+$") && !url.contains("/blaarev-register/"))
                    {
                        mTools.getMedicationWithFullContent(url);
                    }
                    else if(url.matches("^https?://.*?\\.pdf$"))
                    {
                        mTools.showToast(getString(R.string.medication_downloading_pdf), 1);

                        mTools.downloadFile(medicationName, url);
                    }
                    else
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("uri", url);
                        startActivity(intent);
                    }

                    return true;
                }
            });

            mWebView.addJavascriptInterface(new JavaScriptInterface(mContext), "Android");
        }

    }

    // Resume activity
    @Override
    protected void onResume()
    {
        super.onResume();

        mWebView.resumeTimers();
    }

    // Pause activity
    @Override
    protected void onPause()
    {
        super.onPause();

        mWebView.pauseTimers();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) CookieSyncManager.getInstance().sync();
    }

    // Destroy activity
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) mSqLiteDatabase.close();
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_medication, menu);

        mMenu = menu;

        favoriteMenuItem = menu.findItem(R.id.medication_menu_star);

        GetMedicationTask getMedicationTask = new GetMedicationTask();
        getMedicationTask.execute(medicationId);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                Intent intent = NavUtils.getParentActivityIntent(this);

                if(NavUtils.shouldUpRecreateTask(this, intent))
                {
                    TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities();
                }
                else
                {
                    NavUtils.navigateUpTo(this, intent);
                }

                return true;
            }
            case R.id.medication_menu_star:
            {
                addToFavorites(medicationName, medicationManufacturer, medicationType, medicationPrescriptionGroup, medicationUri);
                return true;
            }
            case R.id.medication_menu_interactions:
            {
                Intent intent = new Intent(mContext, InteractionsActivity.class);
                intent.putExtra("search", medicationName);
                startActivity(intent);
                return true;
            }
            case R.id.medication_menu_pictures_uri:
            {
                if(medicationPicturesUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_pictures), 1);
                }
                else
                {
                    Intent intent = new Intent(mContext, MedicationPicturesActivity.class);
                    intent.putExtra("name", medicationName);
                    intent.putExtra("uri", medicationPicturesUri);
                    mContext.startActivity(intent);
                }

                return true;
            }
            case R.id.medication_menu_manufacturer_uri:
            {
                getManufacturerFromUri(medicationManufacturerUri);
                return true;
            }
            case R.id.medication_menu_patient_uri:
            {
                if(medicationPicturesUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_package_inserts), 1);
                }
                else
                {
                    getPackageInserts();
                }

                return true;
            }
            case R.id.medication_menu_spc_uri:
            {
                if(medicationSpcUri.equals(""))
                {
                    mTools.showToast(getString(R.string.medication_no_spc), 1);
                }
                else
                {
                    Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                    intent.putExtra("uri", medicationSpcUri);
                    startActivity(intent);
                }

                return true;
            }
            case R.id.medication_menu_uri:
            {
                mTools.openUri(medicationUri);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    // Spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        try
        {
            if(i > 0)
            {
                int index = i - 1;

                JSONArray jsonArray = new JSONArray(medicationContentSections);

                JSONObject jsonObject = jsonArray.getJSONObject(index);

                mWebView.loadUrl("javascript:scrollToSection('"+jsonObject.getString("id")+"')");
            }
        }
        catch(Exception e)
        {
            Log.e("MedicationActivity", Log.getStackTraceString(e));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) { }

    // Favorite
    private void addToFavorites(String name, String manufacturer, String type, String prescription_group, String uri)
    {
        boolean medicationIsFavorite = medicationIsFavorite(medicationUri);

        if(medicationIsFavorite)
        {
            mSqLiteDatabase.delete(MedicationsFavoritesSQLiteHelper.TABLE, MedicationsFavoritesSQLiteHelper.COLUMN_URI+" = "+mTools.sqe(uri), null);

            favoriteMenuItem.setIcon(R.drawable.ic_star_outline_white_24dp);

            mTools.showToast(getString(R.string.medication_remove_from_favorites), 1);
        }
        else
        {
            ContentValues contentValues = new ContentValues();

            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_NAME, name);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_MANUFACTURER, manufacturer);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_TYPE, type);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_PRESCRIPTION_GROUP, prescription_group);
            contentValues.put(MedicationsFavoritesSQLiteHelper.COLUMN_URI, uri);

            mSqLiteDatabase.insert(MedicationsFavoritesSQLiteHelper.TABLE, null, contentValues);

            favoriteMenuItem.setIcon(R.drawable.ic_star_white_24dp);

            mTools.showToast(getString(R.string.medication_saved_to_favorites), 1);
        }

        mTools.updateWidget();
    }

    private boolean medicationIsFavorite(String uri)
    {
        Cursor cursor = mSqLiteDatabase.query(MedicationsFavoritesSQLiteHelper.TABLE, null, MedicationsFavoritesSQLiteHelper.COLUMN_URI+" = "+mTools.sqe(uri), null, null, null, null);

        long id = 0;

        if(cursor.moveToFirst())
        {
            try
            {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID));
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }

        cursor.close();

        return (id != 0);
    }

    // Manufacturer
    private void getManufacturerFromUri(String uri)
    {
        SQLiteDatabase sqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MANUFACTURERS, null, FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_URI+" = "+mTools.sqe(uri), null, null, null, null);

        long id = 0;

        if(cursor.moveToFirst())
        {
            try
            {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(FelleskatalogenSQLiteHelper.MANUFACTURERS_COLUMN_ID));
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }

        if(id == 0)
        {
            mTools.showToast(mContext.getString(R.string.medication_could_not_find_manufacturer), 1);
        }
        else
        {
            Intent intent = new Intent(mContext, ManufacturerActivity.class);
            intent.putExtra("id", id);
            mContext.startActivity(intent);
        }

        cursor.close();
        sqLiteDatabase.close();
    }

    // Package insert
    private void getPackageInserts()
    {
        if(mTools.isDeviceConnected())
        {
            try
            {
                mProgressBar.setVisibility(View.VISIBLE);

                RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(getString(R.string.project_website)+"api/1/felleskatalogen/medications/package-inserts/?uri="+URLEncoder.encode(medicationPatientUri, "utf-8"), new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response)
                    {
                        mProgressBar.setVisibility(View.GONE);

                        final int packageInsertsCount = response.length();

                        final String[] packageInsertsNames = new String[packageInsertsCount];
                        final String[] packageInsertsUris = new String[packageInsertsCount];

                        for(int i = 0; i < packageInsertsCount; i++)
                        {
                            try
                            {
                                JSONObject packageInsertJsonObject = response.getJSONObject(i);

                                packageInsertsNames[i] = packageInsertJsonObject.getString("name");
                                packageInsertsUris[i] = packageInsertJsonObject.getString("uri");
                            }
                            catch(Exception e)
                            {
                                Log.e("MedicationActivity", Log.getStackTraceString(e));
                            }
                        }

                        new MaterialDialog.Builder(mContext).title(getString(R.string.medication_package_inserts_dialog_title)).items(packageInsertsNames).itemsCallback(new MaterialDialog.ListCallback()
                        {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence)
                            {
                                Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                                intent.putExtra("uri", packageInsertsUris[i]);
                                startActivity(intent);
                            }
                        }).show();
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        mProgressBar.setVisibility(View.GONE);

                        Log.e("MedicationActivity", error.toString());
                    }
                });

                requestQueue.add(jsonArrayRequest);
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }
        }
        else
        {
            mTools.showToast(getString(R.string.device_not_connected), 1);
        }
    }

    // Get medication
    private class GetMedicationTask extends AsyncTask<Long, Void, HashMap<String, String>>
    {
        @Override
        protected void onPostExecute(HashMap<String, String> medication)
        {
            // Medication details
            final String medicationAtcCodes = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ATC_CODES);
            final String medicationBluePrescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_BLUE_PRESCRIPTION);
            final String medicationTriangle = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_TRIANGLE);
            final String medicationDopingStatus = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS);
            final String medicationDopingStatusDescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS_DESCRIPTION);
            final String medicationDopingStatusUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_DOPING_STATUS_URI);
            final String medicationSchengenCertificate = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_SCHENGEN_CERTIFICATE);
            final String medicationContent = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_CONTENT);
            final String medicationFullContentReference = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_FULL_CONTENT_REFERENCE);
            final String medicationPoisoningUris = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_POISONING_URIS);

            medicationName = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_NAME);
            medicationManufacturer = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER);
            medicationManufacturerUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_MANUFACTURER_URI);
            medicationType = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_TYPE);
            medicationPrescriptionGroup = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PRESCRIPTION_GROUP);
            medicationPrescriptionGroupDescription = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PRESCRIPTION_GROUP_DESCRIPTION);
            medicationContentSections = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_CONTENT_SECTIONS);
            medicationUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_URI);
            medicationPicturesUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PICTURES_URI);
            medicationPatientUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_PATIENT_URI);
            medicationSpcUri = medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_SPC_URI);

            // Medication is favorite?
            if(medicationIsFavorite(medicationUri)) favoriteMenuItem.setIcon(R.drawable.ic_star_white_24dp);

            // Medication poisoning information and ATC codes
            try
            {
                int order = 3;

                if(!medicationPoisoningUris.equals(""))
                {
                    final JSONObject medicationPoisoningObject = new JSONObject(medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_POISONING_URIS));

                    final JSONArray medicationPoisoningCodesArray = medicationPoisoningObject.getJSONArray("codes");
                    final JSONArray medicationPoisoningUrisArray = medicationPoisoningObject.getJSONArray("uris");

                    final int medicationPoisoningCodesCount = medicationPoisoningCodesArray.length();

                    for(int i = 0; i < medicationPoisoningCodesCount; i++)
                    {
                        final String poisoningCode = medicationPoisoningCodesArray.getString(i);
                        final String poisoningUri = medicationPoisoningUrisArray.getString(i);

                        order++;

                        String menuItemAppend = (medicationPoisoningCodesCount == 1) ? "" : " ("+poisoningCode+")";

                        MenuItem menuItem = mMenu.add(Menu.NONE, Menu.NONE, order, getString(R.string.medication_menu_poisoning_uri)+menuItemAppend);

                        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem)
                            {
                                Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                                intent.putExtra("uri", poisoningUri);
                                startActivity(intent);

                                return true;
                            }
                        });
                    }
                }

                if(!medicationAtcCodes.equals(""))
                {
                    final JSONArray medicationAtcCodesJsonArray = new JSONArray(medication.get(FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ATC_CODES));

                    for(int i = 0; i < medicationAtcCodesJsonArray.length(); i++)
                    {
                        final String atcCode = medicationAtcCodesJsonArray.getString(i);

                        order++;

                        MenuItem menuItem = mMenu.add(Menu.NONE, Menu.NONE, order, getString(R.string.medication_menu_atc_code_uri)+" ("+atcCode+")");

                        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem)
                            {
                                Intent intent = new Intent(mContext, AtcCodesActivity.class);
                                intent.putExtra("code", atcCode);
                                startActivity(intent);

                                return true;
                            }
                        });
                    }
                }
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }

            // Medication sections
            try
            {
                JSONArray jsonArray = new JSONArray(medicationContentSections);

                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(getString(R.string.medication_choose_section));

                for(int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    arrayList.add(jsonObject.getString("name"));
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getSupportActionBar().getThemedContext(), R.layout.toolbar_spinner_header, arrayList);
                arrayAdapter.setDropDownViewResource(R.layout.toolbar_spinner_item);

                mSpinner.setAdapter(arrayAdapter);
            }
            catch(Exception e)
            {
                Log.e("MedicationActivity", Log.getStackTraceString(e));
            }

            // Medication name
            TextView textView = (TextView) findViewById(R.id.medication_name);
            textView.setText(medicationName);

            // Medication type
            textView = (TextView) findViewById(R.id.medication_type);
            textView.setText(medicationType);

            // Medication needs Schengen certificate?
            if(medicationSchengenCertificate.equals("yes"))
            {
                textView = (TextView) findViewById(R.id.medication_schengen_certificate);
                textView.setText(getString(R.string.medication_schengen_certificate));
                textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("uri", "http://legemiddelverket.no/Bruk_og_raad/Medisiner-pa-utenlandsreise/Sider/default.aspx");
                        startActivity(intent);
                    }
                });

                textView.setVisibility(View.VISIBLE);
            }

            // Medication doping status
            textView = (TextView) findViewById(R.id.medication_doping_status);
            textView.setText(medicationDopingStatusDescription);

            if(medicationDopingStatus.equals("yellow"))
            {
                textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.medication_doping_status_orange), null, null, null);
                textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("uri", medicationDopingStatusUri);
                        startActivity(intent);
                    }
                });
            }
            else if(medicationDopingStatus.equals("red"))
            {
                textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.medication_doping_status_red), null, null, null);
                textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

                textView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(mContext, MedicationWebViewActivity.class);
                        intent.putExtra("uri", medicationDopingStatusUri);
                        startActivity(intent);
                    }
                });
            }

            // Medication prescription group
            Button button = (Button) findViewById(R.id.medication_prescription_group);
            button.setText(medicationPrescriptionGroup);

            if(medicationPrescriptionGroup.startsWith("B"))
            {
                mTools.setBackgroundDrawable(button, R.drawable.medication_prescription_group_orange);
            }
            else if(medicationPrescriptionGroup.startsWith("A"))
            {
                mTools.setBackgroundDrawable(button, R.drawable.medication_prescription_group_red);
            }

            // Medication prescription group description
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    mTools.showToast(medicationPrescriptionGroupDescription, 1);
                }
            });

            // Medication available on blue prescription?
            if(medicationBluePrescription.equals("yes"))
            {
                button = (Button) findViewById(R.id.medication_blue_prescription);

                button.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        mTools.showToast(getString(R.string.medication_blue_prescription), 1);
                    }
                });

                button.setVisibility(View.VISIBLE);
            }

            // Medication has triangle warning?
            if(medicationTriangle.equals("yes"))
            {
                ImageButton imageButton = (ImageButton) findViewById(R.id.medication_triangle);

                imageButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        mTools.showToast(getString(R.string.medication_triangle), 1);
                    }
                });

                imageButton.setVisibility(View.VISIBLE);
            }

            // Medication exists with more information?
            if(medicationFullContentReference.equals(""))
            {
                boolean hideMedicationTipDialog = mTools.getSharedPreferencesBoolean("HIDE_MEDICATION_TIP_DIALOG");

                if(!hideMedicationTipDialog)
                {
                    new MaterialDialog.Builder(mContext).title(getString(R.string.medication_tip_dialog_title)).content(getString(R.string.medication_tip_dialog_message)).positiveText(getString(R.string.medication_tip_dialog_positive_button)).callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            mTools.setSharedPreferencesBoolean("HIDE_MEDICATION_TIP_DIALOG", true);
                        }
                    }).show();
                }
            }
            else
            {
                try
                {
                    JSONObject fullContentJsonObject = new JSONObject(medicationFullContentReference);

                    final String fullContentMedicationName = fullContentJsonObject.getString("name");
                    final String fullContentMedicationManufacturer = fullContentJsonObject.getString("manufacturer");
                    final String fullContentMedicationUri = fullContentJsonObject.getString("uri");

                    new MaterialDialog.Builder(mContext).title(medicationName).content(Html.fromHtml(getString(R.string.medication_full_content_reference_dialog_message_first)+"<br><br><b>"+fullContentMedicationName+"</b><br><small>"+fullContentMedicationManufacturer+"</small><br><br>"+getString(R.string.medication_full_content_reference_dialog_message_second))).positiveText(getString(R.string.medication_full_content_reference_dialog_positive_button)).negativeText(getString(R.string.medication_full_content_reference_dialog_negative_button)).callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            mTools.getMedicationWithFullContent(fullContentMedicationUri);

                            finish();
                        }
                    }).show();
                }
                catch(Exception e)
                {
                    Log.e("MedicationActivity", Log.getStackTraceString(e));
                }
            }

            // Web view
            mWebView.loadDataWithBaseURL("file:///android_asset/", medicationContent, "text/html", "utf-8", null);
        }

        @Override
        protected HashMap<String, String> doInBackground(Long... longs)
        {
            SQLiteDatabase sqLiteDatabase = new FelleskatalogenSQLiteHelper(mContext).getReadableDatabase();

            Cursor cursor = sqLiteDatabase.query(FelleskatalogenSQLiteHelper.TABLE_MEDICATIONS, null, FelleskatalogenSQLiteHelper.MEDICATIONS_COLUMN_ID+" = "+longs[0], null, null, null, null);

            String[] columns = cursor.getColumnNames();

            HashMap<String, String> medication = new HashMap<>();

            if(cursor.moveToFirst())
            {
                for(String column : columns)
                {
                    try
                    {
                        medication.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
                    }
                    catch(Exception e)
                    {
                        Log.e("MedicationActivity", Log.getStackTraceString(e));
                    }
                }
            }

            cursor.close();
            sqLiteDatabase.close();

            return medication;
        }
    }

    // JavaScript interface
    public class JavaScriptInterface
    {
        private final Context mContext;

        JavaScriptInterface(Context context)
        {
            mContext = context;
        }

        @JavascriptInterface
        public void JSshowDialog(String title, String message)
        {
            new MaterialDialog.Builder(mContext).title(title).content(message).positiveText(getString(R.string.medication_javascript_interface_dialog_positive_button)).show();
        }

        @JavascriptInterface
        public void JSresetSpinner()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mSpinner.setSelection(0);
                }
            });
        }
    }
}