package org.ganquan.musictimer

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.WAKE_LOCK
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import org.ganquan.musictimer.comp.RadioGroup
import org.ganquan.musictimer.databinding.ActivityMainBinding
import org.ganquan.musictimer.tools.Apk
import org.ganquan.musictimer.tools.Broadcast
import org.ganquan.musictimer.tools.CoroutineScopeM
import org.ganquan.musictimer.tools.Files
import org.ganquan.musictimer.tools.OneTimeWorker
import org.ganquan.musictimer.tools.Permission
import org.ganquan.musictimer.tools.Time
import org.ganquan.musictimer.tools.Utils
import org.ganquan.musictimer.tools.Utils.Companion.int2String
import java.io.File


class MainActivity : AppCompatActivity() {
    private val permission = Permission(this)
    private val oneTimeWorker: OneTimeWorker = OneTimeWorker(this)
    private var isReadPermission: Boolean = false
    private var isWakeLockPermission: Boolean = false
    private var selectMusicName:String = ""
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var binding: ActivityMainBinding
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var folderPath: String
    private var customPlayTime = 10
    private var normalTimeList: MutableList<MutableList<Int>> =
        mutableListOf(mutableListOf(9,20,10,GONE), mutableListOf(19,20,10,GONE))
    private var startTimeHour: Int = 0
    private var startTimeMunit: Int = 0
    private var playTime: Int = 0
    private var isStart: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        initPermission()
        initListener()
        initVersion()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val type = permission.result(requestCode,grantResults)
        when (type) {
            in READ_MEDIA_AUDIO,READ_EXTERNAL_STORAGE -> {
                isReadPermission = true
                initMusicList()
            }
            WRITE_EXTERNAL_STORAGE -> initMusicPath()
            WAKE_LOCK -> isWakeLockPermission = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScopeM.cleanup()
        mainEnd()
    }

    /** 主程：开始 */
    @SuppressLint("SetTextI18n")
    private fun mainStart() {
        val now = Time.get()
        when (binding.mode.checkedRadioButtonId) {
            R.id.mode_normal -> {
                val list: List<Int>? = normalTimeList.find {it -> !Time.isPass(it[0],it[1])}
                startTimeHour = (list?.get(0)) ?: -1
                startTimeMunit = (list?.get(1)) ?: 0
                playTime = (list?.get(2)) ?: 0
            }
            R.id.mode_custom -> {
                startTimeHour = binding.startTime.hour
                startTimeMunit = binding.startTime.minute
                playTime = binding.playTime.value
            }
        }

        val startMinuteCount = startTimeHour * 60 + startTimeMunit
        val nowMinuteCount = now.hour * 60 + now.minute

        if (nowMinuteCount > startMinuteCount) {
            Toast.makeText(this, getString(R.string.toast_set_time), Toast.LENGTH_SHORT).show()
        } else {
            isStart = true

            val playTime2 = if(nowMinuteCount == startMinuteCount) playTime*60 - now.second else playTime*60
            initCountDown(playTime2)
            binding.startBtn.isEnabled = false
            binding.startBtn.text = "${int2String(startTimeHour)}:${int2String(startTimeMunit)} 开始播放"
            val delay = ((startMinuteCount - nowMinuteCount) * 60 - now.second).toLong()
            val playTime1 = (playTime * 60).toLong()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            wakeLock.acquire((delay+playTime1+5)*1000L)

            val intent = initMusicIntent()
            intent.putExtra("folder_path", folderPath)
            intent.putExtra("music_name", selectMusicName)
            startForegroundService(intent)

            oneTimeWorker.request(
                playTime1,
                delay
            )
        }
    }

    /** 主程：结束 */
    private fun mainEnd() {
        oneTimeWorker.cancel()
        Broadcast.destroyLocal(this) { msg, info -> initReceiver(msg,info)}
        stopService(initMusicIntent())
        initMusicName()
        initCountDown()
        binding.startBtn.isEnabled = true
        binding.startBtn.text = getString(R.string.view_button_start)
        if(wakeLock.isHeld) wakeLock.release()
        isStart = false
    }

    private fun initView() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

<<<<<<< HEAD
        val normalTimeList1 = Utils.sharedPrefer(this, NormalTimeAdapter.sharedPreferKey)
        if(normalTimeList1 != "" && (normalTimeList1 as MutableList<*>).isNotEmpty())
            normalTimeList = normalTimeList1 as MutableList<MutableList<Int>>
=======
        val normalTimeList1: MutableList<MutableList<Int>> =
            Utils.sharedPrefer(this, NormalTimeAdapter.sharedPreferKey) as MutableList<MutableList<Int>>
        if(normalTimeList1.isNotEmpty()) normalTimeList = normalTimeList1
>>>>>>> 101396dc3ccc81115521ba1b19781db4c22eec25
        binding.normalTimeList.layoutManager = GridLayoutManager(this, 2)
        initNormalModeList()
        binding.startTime.setIs24HourView(true)
        binding.playTime.minValue = 1
        binding.playTime.maxValue = 120
        binding.playTime.value = customPlayTime
        binding.mode.checkedRadioButtonId = R.id.mode_normal
        initNormalMode()
    }

    private fun initVersion() {
        val codeUrl = getString(R.string.code_url)

        Apk.checkVersion(
            this,
            codeUrl,
        ) { version ->
            if(version == "") return@checkVersion
            Apk.alertDialog(this, version, R.style.CustomDialog) {
                val apk = Apk(this)
                apk.down(codeUrl, version, getString(R.string.app_name)) { apkPath ->
                    apk.install(File(apkPath))
                }
            }
        }
    }

    private fun initPermission() {
        initMusicPath(true)
        initMusicList()
        isWakeLockPermission = permission.check(WAKE_LOCK)
        if(!isReadPermission) binding.startBtn.text = getString(R.string.view_button_permission)
    }

    @SuppressLint("SetTextI18n")
    private fun initMusicPath(isCheck: Boolean = false) {
        val name = getString(R.string.view_music_path_name)
        val path = Files.createFolder(Files.getMusicFile(name))
        if(path == "") {
            if(isCheck) {
                val isWrite = permission.check(WRITE_EXTERNAL_STORAGE)
                if (isWrite) initMusicPath()
            } else {
                folderPath = Files.getMusicFile().path
            }
        } else {
            binding.musicPath.text = "${getString(R.string.view_music_path)}/${name}"
            folderPath = path
        }
    }

    private fun initMusicList() {
        if(binding.musicList.adapter != null) return

        if(!isReadPermission) {
            isReadPermission = permission.check(READ_MEDIA_AUDIO)
            if(!isReadPermission) return
        }

        val list:List<File> = Files.getList(folderPath.toString())
        if(!list.isNotEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_music), Toast.LENGTH_LONG).show()
            if(!isReadPermission) binding.startBtn.text = getString(R.string.view_button_permission)
            return
        }

        val defaultName = getString(R.string.view_music_default)
        var names = list.map { it.nameWithoutExtension }.toMutableList()
        names.add(0, defaultName)

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.musicList.adapter = adapter
        binding.musicList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectMusicName = parent.getItemAtPosition(position).toString()
                if(selectMusicName == defaultName) selectMusicName = ""
                mainEnd()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectMusicName = ""
                mainEnd()
            }
        }

        binding.startBtn.text = getString(R.string.view_button_start)
    }

    private fun initMusicName(name: String = "") {
        if(selectMusicName != "") binding.musicName.visibility = GONE
        else {
            binding.musicName.text = name
            binding.musicName.visibility = if (name == "") GONE else VISIBLE
        }
    }

    private fun initMusicIntent(action: String = ""): Intent {
        val intent = Intent(this, MusicService::class.java)
        if(action != "") intent.action = action
        return intent
    }

    private fun initCountDown(second: Int = 0) {
        if (second != 0) {
            countDownTimer = object : CountDownTimer((second*1000).toLong(), 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    val hms: Triple<Int,Int,Int> = Time.toHourMinuteSecond(millisUntilFinished)
                    binding.startBtn.text =
                        "${int2String(hms.second)}:${int2String(hms.third)}  ${this@MainActivity.getString(R.string.view_button_end)}"
                }

                override fun onFinish() {}
            }
        } else if(isStart){
            countDownTimer.cancel()
        }
    }

    private fun initNormalMode(isEdit: Boolean = false) {
        if(isEdit) {
            initTimeView(10)
            binding.modeCustomDetail.visibility = VISIBLE
            binding.normalTimeEdit.text = getString(R.string.view_time_btn_done)
            binding.startBtn.text = getString(R.string.view_button_add)
            binding.modeCustom.visibility = GONE
            initNormalModeList(VISIBLE)
        } else {
            binding.modeCustomDetail.visibility = GONE
            binding.normalTimeEdit.text = getString(R.string.view_time_btn_edit)
            binding.startBtn.text = getString(R.string.view_button_start)
            binding.modeCustom.visibility = VISIBLE
            initNormalModeList(GONE)
        }

        binding.normalTimeList.visibility = VISIBLE
        binding.normalTimeEdit.visibility = VISIBLE
        binding.startBtn.isEnabled = true
    }

    private fun initCustomMode() {
        initTimeView()
        binding.normalTimeList.visibility = GONE
        binding.normalTimeEdit.visibility = GONE
        binding.modeCustomDetail.visibility = VISIBLE
        binding.startBtn.isEnabled = true
        binding.startBtn.text = getString(R.string.view_button_start)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initNormalModeList(isDeleteVisibility: Int = GONE) {
        normalTimeList.forEach { it -> it[3] = isDeleteVisibility}
        normalTimeList.sortWith(compareBy<List<Int>> { it[0] }.thenBy { it[1] })
        binding.normalTimeList.adapter = NormalTimeAdapter(normalTimeList)
        binding.normalTimeList.adapter?.notifyDataSetChanged()
    }

    private fun initTimeView(pTime: Int = 0) {
        val now = Time.get()
        binding.startTime.hour = now.hour
        binding.startTime.minute = now.minute
        binding.playTime.value = pTime or customPlayTime
    }

    private fun initListener() {
        binding.startBtn.setOnClickListener {
            when (binding.startBtn.text.split(" ").last()) {
                getString(R.string.view_button_start) -> mainStart()
                getString(R.string.view_button_permission) -> initPermission()
                getString(R.string.view_button_add) -> handleAddNormalTime()
                getString(R.string.view_button_end) -> mainEnd()
            }
        }

        binding.startTime.setOnTimeChangedListener { _, h, m ->
            if(isStart) mainEnd()
        }

        binding.playTime.setOnValueChangedListener { _, oldV, newV ->
            if(binding.mode.checkedRadioButtonId == R.id.mode_custom) customPlayTime = newV
            if(isStart) mainEnd()
        }

        binding.normalTimeEdit.setOnClickListener {
            if(isStart) mainEnd()
            when (binding.normalTimeEdit.text) {
                getString(R.string.view_time_btn_edit) -> initNormalMode(true)
                getString(R.string.view_time_btn_done) -> initNormalMode()
            }
        }

        binding.mode.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
                if(checkedId == binding.mode.checkedRadioButtonId) return
                binding.mode.check(checkedId)
                when (checkedId) {
                    R.id.mode_normal -> initNormalMode()
                    R.id.mode_custom -> initCustomMode()
                }
                mainEnd()
            }
        })

        Broadcast.receiveLocal (this) { msg, info -> initReceiver(msg, info) }
    }

    private fun initReceiver(msg: String, info: String) {
        when (msg) {
            "start worker" -> handlerPlaying()
            "end worker" -> mainEnd()
            "new music" -> initMusicName(info)
        }
    }

    private fun handleAddNormalTime() {
        if(normalTimeList.size >= 10) {
            Toast.makeText(this, getString(R.string.toast_normal_time_limit), Toast.LENGTH_SHORT).show()
            return
        }
        val newList = mutableListOf<Int>(
            binding.startTime.hour,
            binding.startTime.minute,
            binding.playTime.value,
            VISIBLE
        )
        val isExists = normalTimeList.find { it[0] == newList[0] && it[1] == newList[1] }
        if(isExists != null) {
            Toast.makeText(this, getString(R.string.toast_normal_time_exists), Toast.LENGTH_SHORT).show()
        } else {
            normalTimeList.add(newList)
            initNormalModeList(VISIBLE)
        }
    }

    private fun handlerPlaying() {
        startForegroundService(initMusicIntent("ACTION_PLAY"))

        countDownTimer.start()
        binding.startBtn.isEnabled = true
        binding.startBtn.text = getString(R.string.view_button_end)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}