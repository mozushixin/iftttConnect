package com.simtoo.aicamera.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ifttt.connect.Connection;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import com.ifttt.connect.ui.ConnectResult;
import com.ifttt.connect.ui.CredentialsProvider;
import com.simtoo.aicamera.R;
import com.simtoo.aicamera.base.BaseActivity;
import com.simtoo.aicamera.bean.IftttBackUserTokenBean;
import com.simtoo.aicamera.helper.EmailPreferencesHelper;
import com.simtoo.aicamera.net.OkHttp3Utils;
import com.simtoo.aicamera.utils.GsonObjectCallback;
import com.simtoo.aicamera.utils.ScreenSwitch;
import com.simtoo.aicamera.utils.ToastUtil;
import com.simtoo.aicamera.utils.Tools;
import com.simtoo.aicamera.view.ClearEditText;
import com.simtoo.aicamera.view.TitleLayout;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import okhttp3.Call;


public class IftttConnectionActivity extends BaseActivity implements ButtonStateChangeListener, TitleLayout.OnLeftViewClickListener {

    private ConnectButton connectBt;
    private TitleLayout titleLayout;
    private TextView connection_title;
    private EmailPreferencesHelper emailPreferencesHelper;

    private String userToken;
    private CredentialsProvider credentialsProvider;
    private String suggestedEmail = "iftttAccount@gmail.com";
    private String CONNECTION_ID = "yVtymgMp"; //位置

    private String IFTTT_SERVICE_KEY = "5F3f_1mqUgAPfel0Ol6jn9L8OzQpX1tbnMIr-YNMeqZFL0pWpEE90ZFKXCtotUrn";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ifttt_connect);
        ButterKnife.bind(this);
        initEvent();
    }

    private void initEvent() {
        connectBt = findViewById(R.id.connect_button);
        titleLayout = findViewById(R.id.tl_feedback);
        connection_title = findViewById(R.id.connection_title);
        titleLayout.setOnLeftViewClickListener(this);
        emailPreferencesHelper =  new EmailPreferencesHelper(this);
        if (emailPreferencesHelper.getEmail() == null){
            showAddEmergencyContactDialog();
        } else {
            suggestedEmail = emailPreferencesHelper.getEmail();
            credentialsProvider = new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return suggestedEmail;
                }

                @Override
                public String getUserToken() {
                    DownLoad();
                    return userToken;
                }
            };
            initConnect();
        }
    }
    private void initConnect(){
        ConnectButton.Configuration configuration = ConnectButton.Configuration.Builder.withConnectionId(
                CONNECTION_ID,
                suggestedEmail,
                credentialsProvider
                , Uri.parse("groceryexpress://connectcallback")//重定向地址，ifttt完成连接后跳回本APP
        ).setOnFetchCompleteListener(connection ->onFetchConnectionSuccessful(connection)).build();
        connectBt.setup(configuration);
    }
    public void onFetchConnectionSuccessful(Connection connection) {
        connection_title.setText(connection.name);
    }
    @Override
    public void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState) {

    }

    @Override
    public void onError(ErrorResponse errorResponse) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onLeftViewClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        ScreenSwitch.finish(this);
    }
    private void DownLoad() {
        String url = "https://grocery-express.ifttt.com/api/user_token";
        Map<String, String> params = new HashMap<>();
        params.put("code", suggestedEmail);



        OkHttp3Utils.getInstance().doPost(url, params, new GsonObjectCallback<IftttBackUserTokenBean>() {

            @Override
            public void onUi(IftttBackUserTokenBean iftttBackUserTokenBean) {
                userToken = iftttBackUserTokenBean.getUser_token();
            }

            @Override
            public void onFailed(Call call, IOException e) {

            }
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ConnectResult connectResult = ConnectResult.fromIntent(intent);
        connectBt.setConnectResult(connectResult);
    }
    private String getOAuthCode(){
        SharedPreferences sharedPreferences = IftttConnectionActivity.this.getSharedPreferences("user", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("email", suggestedEmail).apply();
        String oAuthCode = sharedPreferences.getString("email", "");
        return oAuthCode;
    }
    private void showAddEmergencyContactDialog() {
        View view = LayoutInflater.from(IftttConnectionActivity.this).inflate(R.layout.ifttt_account_contact_dialog_layout, null);
        final Dialog dialog = new Dialog(context, R.style.no_title);
        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(false);

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER);
        lp.width = (int) (Tools.getScreenWidth(context) * 0.85);
        dialogWindow.setAttributes(lp);

        final ClearEditText clearEditText = (ClearEditText) view.findViewById(R.id.clear_edit_text_contact);
        TextView btnCancel = (TextView) view.findViewById(R.id.btn_cancel);
        TextView btnConfirm = (TextView) view.findViewById(R.id.btn_confirm);
        btnCancel.setText(context.getResources().getString(R.string.cancel));
        btnConfirm.setText(context.getResources().getString(R.string.confirm));
        btnCancel.setOnClickListener(v -> {
            credentialsProvider = new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return suggestedEmail;
                }

                @Override
                public String getUserToken() {
                    DownLoad();
                    return userToken;
                }
            };
            ConnectButton.Configuration configuration = ConnectButton.Configuration.Builder.withConnectionId(
                    CONNECTION_ID,
                    suggestedEmail,
                    credentialsProvider
                    , Uri.parse("groceryexpress://connectcallback")//重定向地址，ifttt完成连接后跳回本APP
            ).setOnFetchCompleteListener(connection ->onFetchConnectionSuccessful(connection)).build();
            connectBt.setup(configuration);
            dialog.dismiss();
        });
        btnConfirm.setOnClickListener(v -> {
            String email = clearEditText.getText().toString();
            if (TextUtils.isEmpty(email)) {
                ToastUtil.showShort(IftttConnectionActivity.this, R.string.ifttt_email_text);
                return;
            }
            emailPreferencesHelper.setEmail(email);
            credentialsProvider = new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return suggestedEmail;
                }

                @Override
                public String getUserToken() {
                    DownLoad();
                    return userToken;
                }
            };
            ConnectButton.Configuration configuration = ConnectButton.Configuration.Builder.withConnectionId(
                    CONNECTION_ID,
                    email,
                    credentialsProvider
                    , Uri.parse("groceryexpress://connectcallback")//重定向地址，ifttt完成连接后跳回本APP
            ).setOnFetchCompleteListener(connection ->onFetchConnectionSuccessful(connection)).build();
            connectBt.setup(configuration);
            dialog.dismiss();
        });
        dialog.setOnShowListener(dialog1 -> {
            // 显示软键盘
            InputMethodManager imm = (InputMethodManager) IftttConnectionActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(clearEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.show();
    }
}
