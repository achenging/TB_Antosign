package fycsb.gky.tb_autosign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

import fycsb.gky.tb_autosign.api.TieBaApi;
import fycsb.gky.tb_autosign.entity.UserMsg;
import fycsb.gky.tb_autosign.http.TiebaRequest;
import fycsb.gky.tb_autosign.http.VolleySingleton;
import fycsb.gky.tb_autosign.ui.AutoSignActivity;
import fycsb.gky.tb_autosign.ui.BaseActivity;
import fycsb.gky.tb_autosign.utils.PostUrlUtil;


public class MainActivity extends BaseActivity implements View.OnClickListener {
    private EditText                                    mUsername;
    private EditText                                    mPassword;
    private Button                                      mLoginButton;
    private com.android.volley.toolbox.NetworkImageView mVcodeImage;
    private EditText                                    mVcodeEditText;

    private String errorCode = "0";
    private String sign;
    private String vcode;
    private String vcodeMD5;
    private long timeStamp = new Date().getTime();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mVcodeImage = (com.android.volley.toolbox.NetworkImageView) findViewById(R.id.vcode_img);
        mVcodeEditText = (EditText) findViewById(R.id.vcode_msg);
        mLoginButton = (Button) findViewById(R.id.login_but);
        mLoginButton.setOnClickListener(this);
        mVcodeEditText.addTextChangedListener(new TextWatcherListener());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_but:

                final String username = mUsername.getText().toString();
                final String password = mPassword.getText().toString();

                if (errorCode.equals("0")) {
                    sign = PostUrlUtil.getSign(username, password, timeStamp);
                }else if (errorCode.equals("5") || errorCode.equals("6")) {

                    Log.i("VCODE>>>>>>",vcode);
                    sign = PostUrlUtil.getVcodeSign(username, password, timeStamp, vcode, vcodeMD5);
                    Log.i("SIGN2>>>>>>>>>",sign);
                }
                TiebaRequest stringRequest = null;
                stringRequest = new TiebaRequest(
                        Request.Method.POST,
                        TieBaApi.URL + TieBaApi.LOGIN,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    errorCode = jsonObject.getString("error_code");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                Log.i("errorCode>>>>>>>>>",errorCode);
                                Gson gson = new Gson();
                                UserMsg userMsg = gson.fromJson(response, UserMsg.class);
                                if (errorCode.equals("0")) {
                                    saveUser(userMsg);
                                } else if (errorCode.equals("5") || errorCode.equals("6")) {
                                    mVcodeImage.setVisibility(View.VISIBLE);
                                    mVcodeEditText.setVisibility(View.VISIBLE);
                                    String vcodeUrl = userMsg.getAnti().getVcodePicUrl();
                                    vcodeMD5 = userMsg.getAnti().getVcodeMd5();
                                    mVcodeImage.setImageUrl(vcodeUrl, VolleySingleton.getInstance(MainActivity.this).getImageLoader());
                                    vcode = mVcodeEditText.getText().toString();

                                } else {
                                    Toast.makeText(MainActivity.this, "Unknown Error!!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() {
                        try {
                            return PostUrlUtil.getParamsHashMap(username, password, timeStamp, sign, vcode, vcodeMD5);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                VolleySingleton.getInstance(MainActivity.this).addRequest(stringRequest);
                break;
        }
    }

    private void saveUser(UserMsg userMsg) {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.config) + userMsg.getUser().getName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TieBaApi.ID, userMsg.getUser().getId())
                .putString(TieBaApi.NAME, userMsg.getUser().getName())
                .putString(TieBaApi.DBUSS, userMsg.getUser().getBDUSS())
                .putString(TieBaApi.PORTRAIT, userMsg.getUser().getPortrait())
                .putString(TieBaApi.TBS, userMsg.getAnti().getTbs());
        editor.commit();
        Intent autoSignIntent = new Intent(MainActivity.this, AutoSignActivity.class);
        autoSignIntent.putExtra(TieBaApi.NAME, userMsg.getUser().getName());
        startActivity(autoSignIntent);
    }

    private class TextWatcherListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            vcode = s.toString();
        }
    }
}
