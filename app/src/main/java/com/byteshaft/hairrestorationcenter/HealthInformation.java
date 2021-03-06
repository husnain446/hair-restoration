package com.byteshaft.hairrestorationcenter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.byteshaft.hairrestorationcenter.utils.AppGlobals;
import com.byteshaft.hairrestorationcenter.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class HealthInformation extends AppCompatActivity implements
        HttpRequest.OnReadyStateChangeListener, View.OnClickListener {

    private Spinner gender;
    private EditText age;
    private ProgressDialog mProgressDialog;
    private HttpRequest mRequest;
    private ListView mListView;
    private ArrayList<JSONObject> fieldData;
    private ArrayList<Integer> idsArray;
    private HashMap<Integer, String> answersList;
    private Button submitButton;
    private StringBuilder stringBuilder = new StringBuilder();
    private ArrayList<String> requiredFields;
    private int idForGender = 2;
    private static boolean sPostRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.health_information);
        Log.i("KEY", "" + AppGlobals.sEntryId);
        fieldData = new ArrayList<>();
        idsArray = new ArrayList<>();
        answersList = new HashMap<>();
        requiredFields = new ArrayList<>();
        age = (EditText) findViewById(R.id.age);
        gender = (Spinner) findViewById(R.id.gender);
        submitButton = (Button) findViewById(R.id.submit_answers);
        submitButton.setOnClickListener(this);
        mListView = (ListView) findViewById(R.id.fields_list_view);
        mProgressDialog = Helpers.getProgressDialog(HealthInformation.this);
        age.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {

                } else {
                    HashMap<Integer, String> answer = new HashMap<>();
                    answer.put(age.getId(), age.getText().toString());
                    answersList.put(age.getId(), age.getText().toString());
                    Log.i("TAG", String.valueOf(answersList));
                }
            }
        });
        gender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        getFieldsDetails();
    }


    private void getFieldsDetails() {
        sPostRequest = false;
        mProgressDialog.show();
        mRequest = new HttpRequest(getApplicationContext());
        mRequest.setOnReadyStateChangeListener(this);
        mRequest.open("GET", AppGlobals.QUESTION_LIST);
        mRequest.send();
    }

    @Override
    public void onReadyStateChange(HttpRequest request, int i) {
        switch (i) {
            case HttpRequest.STATE_DONE:
                mProgressDialog.dismiss();
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        mProgressDialog.dismiss();
                        if (sPostRequest) {
                            Log.i("TAG", mRequest.getResponseText());
                            try {
                                JSONObject jsonObject = new JSONObject(mRequest.getResponseText());
                                if (jsonObject.getString("Message").equals("Successfully")) {

                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(HealthInformation.this);
                                    alertDialogBuilder.setTitle("Success");
                                    alertDialogBuilder.setMessage("Your details have uploaded successfully.")
                                            .setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            AppGlobals.sConsultationSuccess = true;
                                            dialog.dismiss();
                                            finish();
                                        }
                                    });
                                    AlertDialog alertDialog = alertDialogBuilder.create();
                                    alertDialog.show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.e("TAG", stringBuilder.toString());
                        } else {
                            parseJsonAndSetUi(mRequest.getResponseText());
                        }
                }
        }

    }

    private void parseJsonAndSetUi(String data) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(data);
            if (jsonObject.getString("Message").equals("Successfully")) {
                JSONArray jsonArray = jsonObject.getJSONArray("details");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    if (!json.getString("title").equals("Age")) {
                        if (!json.getString("title").equals("Gender")) {
                            fieldData.add(json);
                            idsArray.add(json.getInt("id"));
                        }
                    } else if (json.getString("title").equals("Age")) {
                        idsArray.add(json.getInt("id"));
                        age.setId(json.getInt("id"));
                    } else if (json.getString("title").equals("Gender")) {
                        idForGender = json.getInt("id");
                    }
                }
            } else {
                AppGlobals.alertDialog(HealthInformation.this, "Not Found", "Nothing found");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("TAG", String.valueOf(fieldData));
        Adapter adapter = new Adapter(getApplicationContext(), fieldData, R.layout.delegate_consultation_fields);
        mListView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.submit_answers:
                submitButton.requestFocus();
                if (AppGlobals.sEntryId == 0) {
                    Toast.makeText(HealthInformation.this, "please try again process failed",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    boolean result = validateEditText();
                    Log.i("boolean", " " + result);
                    if (result) {
                        mProgressDialog.show();
                        sendConsultationData(stringBuilder.toString());
                    }
                }
                break;
        }
    }

    class Adapter extends ArrayAdapter<JSONObject> {

        private ArrayList<JSONObject> fieldsDetail;

        public Adapter(Context context, ArrayList<JSONObject> fieldsDetail, int resource) {
            super(context, resource);
            this.fieldsDetail = fieldsDetail;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.delegate_consultation_fields, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.field_title);
                holder.editText = (EditText) convertView.findViewById(R.id.field_answer);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            try {
                StringBuilder title = new StringBuilder();
                if (fieldsDetail.get(position).getInt("required") == 1) {
                    title.append("* ").append(fieldsDetail.get(position).getString("title"));
                } else {
                    title.append(fieldsDetail.get(position).getString("title"));
                }
                holder.title.setText(title);
                holder.editText.setId(fieldsDetail.get(position).getInt("id"));
                holder.editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (b) {

                        } else {
                            try {
                                Log.i(holder.editText.getText().toString(), String.valueOf(fieldsDetail.get(position).getInt("id")));
                                answersList.put(fieldsDetail.get(position).getInt("id"), holder.editText.getText().toString());
                                if (fieldsDetail.get(position).getInt("required") == 1) {
                                    if (!requiredFields.contains(String.valueOf(fieldsDetail.
                                            get(position).getInt("id")))) {
                                        requiredFields.add(String.valueOf(fieldsDetail.
                                                get(position).getInt("id")));
                                    }
                                    Log.i("REQUIRED", "fields" + requiredFields);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return convertView;
        }

        @Override
        public int getCount() {
            Log.e("TAG", "" + fieldsDetail.size());
            return fieldsDetail.size();
        }
    }

    private boolean validateEditText() {
        stringBuilder = new StringBuilder();
        boolean value = false;
        Log.i("TAG", "array" + answersList.size());
        Log.i("TAG", "required fields" + requiredFields);
        for (int id : idsArray) {
            if (requiredFields.size() == 4 && answersList.size() >= 4) {
                if (answersList.containsKey(id)) {
                    value = true;
                    stringBuilder.append(String.format("[%d]=%s&", id, answersList.get(id)));
                }
            } else if (requiredFields.size() < 4 && answersList.size() < 4) {
                value = false;
                Toast.makeText(HealthInformation.this, "All required fields must be filled", Toast.LENGTH_SHORT).show();
                break;
            } else {
                value = false;
                break;
            }
        }
        stringBuilder.append(String.format("user_id=%s&", AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_USER_ID)));
        stringBuilder.append(String.format("entry_id=%s&", AppGlobals.sEntryId));
        stringBuilder.append(String.format("[%d]=%s", idForGender, gender.getSelectedItem().toString()));
        Log.i("String", stringBuilder.toString());
        return value;
    }

    class ViewHolder {
        public TextView title;
        public EditText editText;
    }

    private void sendConsultationData(String data) {
        sPostRequest = true;
        mRequest = new HttpRequest(getApplicationContext());
        mRequest.setOnReadyStateChangeListener(this);
        mRequest.open("POST", AppGlobals.CONSULTATION_STEP_2);
        mRequest.send(data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.health_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.health_information:
                Toast.makeText(HealthInformation.this, "hello", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
