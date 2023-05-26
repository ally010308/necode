package com.example.motionpositionsensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null

    // sensors
    private var mSensorLinearAcceleration: Sensor? = null

    //텍스트 뷰어
    private var tvLineX: TextView? = null
    private var tvLineY: TextView? = null
    private var tvLineZ: TextView? = null

    private var tvSpeedX: TextView? = null
    private var tvDisX: TextView? = null

    private var tvSpeedY: TextView? = null
    private var tvDisY: TextView? = null

    private var tvSpeedZ: TextView? = null
    private var tvDisZ: TextView? = null

    private var tvTotD: TextView? = null

    //private var tvMove: TextView? = null


    // Sensor's values 행렬에 저장됨 0>x, 1>y, 2>z
    private var line = FloatArray(3)


    // 거리 계산용
    private var nowAccX = 0F  //Float 타입임
    private var recentSpeedX:Float = 0F //A
    private var nowSpeedX:Float = 0F  //B
    private var distanceX:Float = 0F //이동거리

    private var nowAccY = 0F  //Float 타입임
    private var recentSpeedY:Float = 0F //A
    private var nowSpeedY:Float = 0F  //B
    private var distanceY:Float = 0F //이동거리

    private var nowAccZ = 0F  //Float 타입임
    private var recentSpeedZ:Float = 0F //A
    private var nowSpeedZ:Float = 0F  //B
    private var distanceZ:Float = 0F //이동거리

    private var totalD:Float = 0F //이동거리
    private var totalSpeed = 0F
    private var last_TotalSp = 0F
    private var isMoving: Boolean = false

    private var betweenStationDis:Float = 0F

    //private var stopCount = 0 //멈춤 카운트

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val dataList: MutableList<String> = mutableListOf()

    //시간계산
    private var previousTime:Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //
        listView = findViewById(R.id.listView)
        // 어댑터 초기화
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        // ListView에 어댑터 설정
        listView.adapter = adapter

        // Identify the sensors that are on a device
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Assign the textViews
        tvLineX = findViewById<View>(R.id.label_lineX) as TextView
        tvLineY = findViewById<View>(R.id.label_lineY) as TextView
        tvLineZ = findViewById<View>(R.id.label_lineZ) as TextView

        tvSpeedX = findViewById<View>(R.id.label_speedX) as TextView
        tvDisX = findViewById<View>(R.id.label_disX) as TextView

        tvSpeedY = findViewById<View>(R.id.label_speedY) as TextView
        tvDisY = findViewById<View>(R.id.label_disY) as TextView

        tvSpeedZ = findViewById<View>(R.id.label_speedZ) as TextView
        tvDisZ = findViewById<View>(R.id.label_disZ) as TextView

        tvTotD = findViewById<View>(R.id.label_totalDis) as TextView

        // sensors connection
        mSensorLinearAcceleration = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Check if all sensors are available
        val sensor_error = resources.getString(R.string.error_no_sensor)

        //센서 에러나면 출력
        if (mSensorLinearAcceleration == null) {
            tvLineX!!.text = sensor_error
            tvLineY!!.text = sensor_error
            tvLineZ!!.text = sensor_error

            tvSpeedX!!.text = sensor_error
            tvDisX!!.text = sensor_error

            tvSpeedY!!.text = sensor_error
            tvDisY!!.text = sensor_error

            tvSpeedZ!!.text = sensor_error
            tvDisZ!!.text = sensor_error

            tvTotD!!.text = sensor_error
        }
        /* 테스트용
        println("현재시간: $previousTime")
        previousTime = System.currentTimeMillis() - previousTime
        println("현재시간플롯: $previousTime")
        println("현재시간플롯: ${previousTime.toFloat()}")
        println("속도: ${line[0]}")
        println("속도더블: ${line[0].toDouble()}")
        */

        val stopButton: Button = findViewById(R.id.stop_button)
        stopButton.setOnClickListener {
            // 버튼을 클릭했을 때 실행되어야 하는 동작을 여기에 작성
            reset()
            Toast.makeText(this, "이동이 멈춤", Toast.LENGTH_LONG).show()
        }
    }


    override fun onStart() { //앱 실행시 시작하는 구간
        super.onStart()
        if (mSensorLinearAcceleration != null) { mSensorManager!!.registerListener(this, mSensorLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL) }
        handler.post(handlerTask)

    }

    override fun onStop() {
        super.onStop()
        // Stop listening the sensors
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Get sensors data when values changed
        val sensorType = event.sensor.type
        when (sensorType) {

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                line = event.values
                /*    //일단 초당 계산으로 바꿔둠. 센서당으로 바꾸려면 핸들러부분 다 삭제하고 아래 주석처리 해제하면됨
                tvLineX!!.text = resources.getString(R.string.label_lineX, line[0])
                tvLineY!!.text = resources.getString(R.string.label_lineY, line[1])
                tvLineZ!!.text = resources.getString(R.string.label_lineZ, line[2])

                nowAccX = round(line[0]*100)/100  //소수점 두번째 까지 끊음
                nowAccY = round(line[1]*100)/100
                nowAccZ = round(line[2]*100)/100

                //getDiswT()

                tvSpeedX!!.text = resources.getString(R.string.label_speedX, nowSpeedX)
                tvDisX!!.text = resources.getString(R.string.label_disX, distanceX)

                tvSpeedY!!.text = resources.getString(R.string.label_speedY, nowSpeedY)
                tvDisY!!.text = resources.getString(R.string.label_disY, distanceY)

                tvSpeedZ!!.text = resources.getString(R.string.label_speedZ, nowSpeedZ)
                tvDisZ!!.text = resources.getString(R.string.label_disZ, distanceZ)

                tvTotD!!.text = resources.getString(R.string.label_totalDis, totalD)

                 */

                testStop()

            }
            else -> { }
        }

    }

    //시간대비 계산
    private fun getDiswT() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - previousTime) / 1000.0

        //X
        nowSpeedX = recentSpeedX+nowAccX*(elapsedTime.toFloat())
        distanceX = checkSignCal(recentSpeedX, nowSpeedX, elapsedTime)
        //val avgSpeedX = (nowSpeedX+recentSpeedX) / 2
        //distanceX = avgSpeedX*elapsedTime.toFloat()

        //Y
        nowSpeedY = recentSpeedY+nowAccY*elapsedTime.toFloat()
        distanceY = checkSignCal(recentSpeedY, nowSpeedY, elapsedTime)
        //val avgSpeedY = (nowSpeedY+recentSpeedY) / 2
        //distanceY = avgSpeedY*elapsedTime.toFloat()

        //Z
        nowSpeedZ = recentSpeedZ+nowAccZ*elapsedTime.toFloat()
        distanceZ = checkSignCal(recentSpeedZ, nowSpeedZ, elapsedTime)
        //val avgSpeedZ = (nowSpeedZ+recentSpeedZ) / 2
        //distanceZ = avgSpeedZ*elapsedTime.toFloat()

        //총 거리
        totalD += sqrt((distanceX).pow(2)+(distanceY).pow(2)+(distanceZ).pow(2))

        //역간거리
        betweenStationDis+= sqrt((distanceX).pow(2)+(distanceY).pow(2)+(distanceZ).pow(2))

        //시간 확인용
        //label_time.text = currentTime.toString()
        //label_dTime.text = elapsedTime.toString()

        //이월
        recentSpeedX = nowSpeedX
        recentSpeedY = nowSpeedY
        recentSpeedZ = nowSpeedZ
        previousTime = currentTime

    }
    private fun checkSignCal(a: Float, b: Float, elapsedTime: Double): Float {
        val t = elapsedTime.toFloat()
        return if ((a < 0 && b > 0) || (a > 0 && b < 0)) { //속도의 부호가 바뀔 경우 거리계산
            val aA = getAbsoluteValue(a)
            val bA = getAbsoluteValue(b)
            val c= aA+bA
            val disA = aA*t*(aA/c)/2
            val disB = bA*t*(bA/c)/2
            return disA + disB
            //diffGetD(a, b)
        } else {
            return t*(a+b)/2
        }
    }

    private fun getAbsoluteValue(A: Float): Float {
        return if (A < 0) -A else A
    }
/*
    private fun diffGetD (a: Float, b: Float): Float {
        val aA = getAbsoluteValue(a)
        val bA = getAbsoluteValue(b)
        val c= aA+bA
        val disA = aA*(aA/c)/2
        val disB = bA*(bA/c)/2
        val dis = disA + disB
        println("거리는 $disA + $disB = $dis")
        return dis
    }
    private fun sameGetD (a: Float, b: Float): Float {
        val dis = (a+b)/2
        println("거리는 $dis")
        return dis
    }

 */

    private fun testStop() {

        val x = nowSpeedX
        val y = nowSpeedY
        val z = nowSpeedZ

        last_TotalSp = totalSpeed
        totalSpeed = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val deltaSpeed = totalSpeed - last_TotalSp

        if (deltaSpeed > 1.5f) {
            // 이동 감지
            //tvMove!!.text = resources.getString(R.string.moving)
            isMoving = true
        } else if (isMoving && totalSpeed < 10) {
            Toast.makeText(this, "이동이 멈춤", Toast.LENGTH_LONG).show()

            isMoving = false
            //stopCount++
            //tvCount!!.text = resources.getString(R.string.label_count, stopCount)
            reset()
        }
    }
    private fun reset() {
        //tvMove!!.text = resources.getString(R.string.stop)
        nowAccX = 0F
        nowAccY = 0f
        nowAccZ = 0f
        recentSpeedX = 0f
        recentSpeedY = 0f
        recentSpeedZ = 0f


        //현재까지 이동거리 저장

        dataList.add(betweenStationDis.toString())

        // 어댑터에 변경 사항 알림
        adapter.notifyDataSetChanged()

        betweenStationDis=0f
    /*
        val adapter = listView.adapter as ArrayAdapter<String>(applicationContext, android.R.layout.simple_list_item_1, items)
        adapter.add(betweenStationDis.toString())
        adapter.notifyDataSetChanged()

         */
    }

    //하단 단순 필터링은 일단 주석처리

    val handler = Handler()


    val millisTime = 1000  //1000=1초에 한번씩 실행
    private val handlerTask = object : Runnable {
        override fun run() {

            /*
            recentSpeedX=filter(recentSpeedX)
            recentSpeedY=filter(recentSpeedY)
            recentSpeedZ=filter(recentSpeedZ)
             */
            tvLineX!!.text = resources.getString(R.string.label_lineX, line[0])
            tvLineY!!.text = resources.getString(R.string.label_lineY, line[1])
            tvLineZ!!.text = resources.getString(R.string.label_lineZ, line[2])

            nowAccX = round(line[0]*100)/100  //소수점 두번째 까지 끊음
            nowAccY = round(line[1]*100)/100
            nowAccZ = round(line[2]*100)/100

            getDiswT()
            tvSpeedX!!.text = resources.getString(R.string.label_speedX, nowSpeedX)
            tvDisX!!.text = resources.getString(R.string.label_disX, distanceX)

            tvSpeedY!!.text = resources.getString(R.string.label_speedY, nowSpeedY)
            tvDisY!!.text = resources.getString(R.string.label_disY, distanceY)

            tvSpeedZ!!.text = resources.getString(R.string.label_speedZ, nowSpeedZ)
            tvDisZ!!.text = resources.getString(R.string.label_disZ, distanceZ)

            tvTotD!!.text = resources.getString(R.string.label_totalDis, totalD)
            handler.postDelayed(this, millisTime.toLong()) // millisTiem 이후 다시
        }
    }
/*
    fun filter (filteringVal:Float): Float {
        if(filteringVal < 0.3){
            /*
            Handler(Looper.getMainLooper()).postDelayed({
                if(filteringVal <0.3){
                    return@postDelayed 0
                }
                else {
                    return@postDelayed filteringVal
                }
            }, 3000)

             */
            Toast.makeText(this@MainActivity, "filtered!", Toast.LENGTH_SHORT).show()
            return 0.0F
        }
        else {
            return filteringVal
        }


    }

     */

    /*
    //거리 계산용
    private fun getDistanceX() {
        nowSpeedY = recentSpeedY+(nowAccY/100)
        nowDistanceY = ((nowSpeedY+recentSpeedY)/2)/100
        distanceY += nowDistanceY
        recentSpeedY = nowSpeedY

        nowSpeedZ = recentSpeedZ+(nowAccZ/100)
        nowDistanceZ = ((nowSpeedZ+recentSpeedZ)/2)/100
        distanceZ += nowDistanceZ
        recentSpeedZ = nowSpeedZ

        totalD += sqrt((nowDistanceX).pow(2)+(nowDistanceY).pow(2)+(nowDistanceZ).pow(2))

    }
     */




    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    /*
    fun computeOrientation() {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, acc, mag)

        val orientationAngles = FloatArray(3)
        var radian = SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Convert angles from radians to degree
        val angles = FloatArray(3)
        angles[0] = (radian[0].toDouble() * 180 / 3.14).toFloat()
        angles[1] = (radian[1].toDouble() * 180 / 3.14).toFloat()
        angles[2] = (radian[2].toDouble() * 180 / 3.14).toFloat()

        tvAzimuth!!.text = resources.getString(R.string.label_azimuth, angles[0])
        tvPitch!!.text = resources.getString(R.string.label_pitch, angles[1])
        tvRoll!!.text = resources.getString(R.string.label_roll, angles[2])
    }
     */
}
