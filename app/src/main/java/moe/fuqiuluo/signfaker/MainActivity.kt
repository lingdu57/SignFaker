package moe.fuqiuluo.signfaker

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.beacon.event.UserAction
import com.tencent.mobileqq.channel.ChannelManager
import com.tencent.mobileqq.channel.ChannelProxy
import com.tencent.mobileqq.dt.app.Dtc
import com.tencent.mobileqq.fe.FEKit
import com.tencent.mobileqq.qsec.qsecurity.QSecConfig
import com.tencent.mobileqq.sign.QQSecuritySign
import com.tencent.qphone.base.BaseConstants
import com.tencent.qphone.base.util.CodecWarpper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.fuqiuluo.signfaker.http.HttpServer
import moe.fuqiuluo.signfaker.logger.TextLogger
import moe.fuqiuluo.signfaker.logger.TextLogger.log
import moe.fuqiuluo.signfaker.proxy.ProxyContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.lang.ref.WeakReference
import java.security.Security
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val isInit = AtomicBoolean(false)

    lateinit var input: EditText
    lateinit var send: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val text = findViewById<TextView>(R.id.text)
        TextLogger.updateTextHandler = object: Handler(mainLooper) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.what == TextLogger.WHAT_INFO) {
                    text.text = text.text.toString() + "\n" + msg.obj
                }
            }
        }

        input = findViewById(R.id.input)
        send = findViewById(R.id.send)

        if (isInit.compareAndSet(false, true)) {
            GlobalScope.launch {
                initServer()
                //initCodec()
                initFEKit()
                initCommand()
            }
        }
    }

    private fun initCommand() {
        send.setOnClickListener {
            TextLogger.input(">>> ${input.text}")
            val command = input.text.toString()
            if (command == "refresh_token" || command == "token" || command == "request_token") {
                QQSecuritySign.requestToken()
                log("Call QQSecuritySign.requestToken()")
            }
            input.setText("")
        }
    }

    private suspend fun initServer() {
        log("请输入启动在哪个服务器端口：")

        var port = 0
        send.setOnClickListener {
            TextLogger.input(">>> ${input.text}")
            kotlin.runCatching {
                val myPort = input.text.toString().toInt()
                if (myPort !in 1000 .. 64000) {
                    throw IllegalArgumentException()
                } else {
                    port = myPort
                }
            }.onFailure {
                input.setText("")
                log("错误输入，请重新尝试")
            }
        }

        while (port == 0) {
            delay(3000)
            log("期待输入中 (3s) ...... ")
        }

        log("服务器将运行在端口：$port")

        HttpServer(port)
    }

    private suspend fun initFEKit() {
        val ctx = ProxyContext(this)
        Dtc.ctx = WeakReference(ctx)
        UserAction.initUserAction(ctx, false)
        UserAction.setAppKey("0S200MNJT807V3E")
        UserAction.setAppVersion("8.9.68")

        var qimei = ""
        UserAction.getQimei {
            log("QIMEI FETCH 成功： $it")
            qimei = it
            QSecConfig.business_q36 = it
        }

        val qua = "V1_AND_SQ_8.9.68_4264_YYB_D"

        val cs = contentResolver
        log("预设androidId为：${Settings.System.getString(cs, "android_id")}")

        log("请输入你的androidId：")
        var androidId = ""
        send.setOnClickListener {
            TextLogger.input(">>> ${input.text}")
            androidId = input.text.toString()
            input.setText("")
        }
        while (androidId.isEmpty()) {
            delay(3000)
            log("期许输入中 (3s) ...... ")
        }
        log("你的androidId输入为[$androidId]")
        Settings.System.putString(cs, "android_id", androidId)
        Dtc.androidId = androidId

        FEKit.init(qua, qimei, androidId, ctx)
        ChannelManager.setChannelProxy(object: ChannelProxy() {
            override fun sendMessage(cmd: String, buffer: ByteArray, id: Long) {
                log("ChannelProxy.sendMessage($cmd, $buffer, $id)")
            }
        })
        ChannelManager.initReport(QSecConfig.business_qua, "7.0.300", Build.VERSION.RELEASE, Build.BRAND + Build.MODEL, QSecConfig.business_q36, QSecConfig.business_guid)
        ChannelManager.setCmdWhiteListChangeCallback {
            it.forEach {
                log("Register for cmd: $it")
            }
        }
    }

    /*
    private fun initCodec() {
        val codec = object: CodecWarpper() {
            override fun onInvalidData(i2: Int, i3: Int, str: String) {
                log("onInvalidData")
            }

            override fun onInvalidSign() {
                log("onInvalidSign")
            }

            override fun onResponse(i2: Int, obj: Any?, i3: Int) {
                log("onResponse1")
            }

            override fun onResponse(i2: Int, obj: Any?, i3: Int, bArr: ByteArray?) {
                log("onResponse2")
            }

            override fun onSSOPingResponse(bArr: ByteArray?, i2: Int): Int {
                return 0
            }
        }
        CodecWarpper.getFileStoreKey()
        val hashSet: HashSet<String> = hashSetOf()
        hashSet.add(BaseConstants.CMD_LOGIN_AUTH)
        hashSet.add(BaseConstants.CMD_LOGIN_CHANGEUIN_AUTH)
        hashSet.add("GrayUinPro.Check")
        hashSet.add(BaseConstants.CMD_WT_LOGIN_AUTH)
        hashSet.add(BaseConstants.CMD_WT_LOGIN_NAME2UIN)
        hashSet.add("wtlogin.exchange_emp")
        hashSet.add("wtlogin.trans_emp")
        hashSet.add("wtlogin.register")
        hashSet.add(BaseConstants.CMD_WT_LOGIN_ADDCONTACTS)
        hashSet.add(BaseConstants.CMD_WT_LOGIN_REQUESTREBINDMBL)
        hashSet.add(BaseConstants.CMD_CONNAUTHSVR_GETAPPINFO)
        hashSet.add(BaseConstants.CMD_CONNAUTHSVR_GETAUTHAPILIST)
        hashSet.add(BaseConstants.CMD_CONNAUTHSVR_GETAUTHAPI)
        hashSet.add("QQConnectLogin.pre_auth_emp")
        hashSet.add("QQConnectLogin.auth_emp")
        hashSet.add(BaseConstants.CMD_REQ_CHECKSIGNATURE)

        hashSet.add("trpc.o3.ecdh_access.EcdhAccess.SsoEstablishShareKey")
        hashSet.add("trpc.o3.ecdh_access.EcdhAccess.SsoSecureAccess")

        hashSet.add("trpc.o3.mobile_security.MobileSecurity.SsoCheckSwitch")
        hashSet.add(BaseConstants.CMD_REPORTSTAT)
        hashSet.add("trpc.qqlog.qqlog_push.Portal.SsoPullReportRule")
        hashSet.add("trpc.login.account_logic.AccountLogicService.SsoThirdPartQueryEncryptedBind")
        CodecWarpper.nativeInitNoLoginWhiteList(hashSet)
    }*/
}