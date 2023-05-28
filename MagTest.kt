package com.android.spinner

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
//import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
//import com.android.volley.Request
//import com.devjaewoo.sensormonitoringapp.databinding.ActivityMainBinding
//import com.devjaewoo.sensormonitoringapp.request.RequestHandler

import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.Color // 그래프 그리기
import android.widget.Button
////// RecycleView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_sub.*

////

class SubActivity : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    //mp 그래프
    private lateinit var lineChart: LineChart
    private var entries: ArrayList<Entry> = ArrayList()
    private lateinit var lineChart2: LineChart
    private var entries2: ArrayList<Entry> = ArrayList()
    private var handler: Handler = Handler()

    //recycleView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private val data: MutableList<String> = mutableListOf()
    //recycleView

//    private var handler2 : Handler


    // sensors
    private var mSensorLinearAcceleration: Sensor? = null
    private var mSensorMagneticField: Sensor? = null
    private var mSensorAccelerometer: Sensor? = null

    // Sensor's values
    private var mag  = FloatArray(3)
    private var line = FloatArray(3)
    private var acc  = FloatArray(3)

    private var tvLineX: TextView? = null
    private var tvLineY: TextView? = null
    private var tvValacc: TextView? = null  // 가속도 크기
    private var tvTotD: TextView? = null

    private var ValAcc = 0F



    private var nowAccX = 0F  //Float 타입임
    private var recentSpeedX:Float = 0F //A
    private var nowSpeedX:Float = 0F  //B
    private var distanceX:Float = 0F //이동거리

    private var nowAccY = 0F  //Float 타입임
    private var recentSpeedY:Float = 0F //A
    private var nowSpeedY:Float = 0F  //B
    private var distanceY:Float = 0F //이동거리

    private var totalD:Float = 0F //이동거리
    private var totalSpeed = 0F             // 추가

    private var betweenStationDis:Float = 0F // 이동거리가 저장되는 곳 ?

    private var varMag:Double = 0.0
    private var case = 0 //케이스 확인
    private var last_varMag = 0.0


    //시간계산
    private var previousTime:Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        ////// recycleView 설정 부분  /////***************
        recyclerView = findViewById(R.id.recycleView)
        adapter = MyAdapter(data) // 데이터를 가져오는 함수에 맞게 수정 필요

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        ////// recycleView 설정 부분  /////****************

        // Identify the sensors that are on a device
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //mp 그래프
        lineChart = findViewById(R.id.chart)
        lineChart2 = findViewById(R.id.chart2)
        lineChart.setNoDataText("No data available")
        // 그래프 스타일 설정

        // Assign the textViews
        tvLineX = findViewById<View>(R.id.label_lineX) as TextView
        tvLineY = findViewById<View>(R.id.label_lineY) as TextView
        tvValacc = findViewById<View>(R.id.label_totalacc) as TextView

        // sensors connection
        mSensorLinearAcceleration = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorMagneticField = mSensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        // Check if all sensors are available
        val sensor_error = resources.getString(R.string.error_no_sensor)

        if (mSensorLinearAcceleration == null) {
            tvLineX!!.text = sensor_error
            tvLineY!!.text = sensor_error
            tvTotD!!.text = sensor_error
            tvValacc!!.text = sensor_error
        }
        if (mSensorMagneticField == null) {
            label_mag.text = sensor_error
        }

        val stopButton: Button = findViewById(R.id.stop_button)
        stopButton.setOnClickListener {
            // 버튼을 클릭했을 때 실행되어야 하는 동작을 여기에 작성
            reset()
        }
    }
    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mSensorLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL)
        startDataUpdate()
    }

    override fun onStart() {
        super.onStart()
        if (mSensorLinearAcceleration != null) { mSensorManager!!.registerListener(this, mSensorLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL) }
        if (mSensorAccelerometer != null) { mSensorManager!!.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL) }
        if (mSensorMagneticField != null) { mSensorManager!!.registerListener(this, mSensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    override fun onPause() {  // onStop 을 onPause 로 대체 mp 그래프
        super.onPause()
        // Stop listening the sensors
        mSensorManager!!.unregisterListener(this)
        stopDataUpdate()   // mp 그래프
    }

    private val windowSize = 10
    private val accelerationBufferX = mutableListOf<Float>()
    private val accelerationBufferY = mutableListOf<Float>()

    private val magBuffer = mutableListOf<Float>()
    private val magBufferVar = mutableListOf<Float>()
    private val varListSize = 100

    override fun onSensorChanged(event: SensorEvent) {
        // Get sensors data when values changed
        val sensorType = event.sensor.type
        when (sensorType) {
            Sensor.TYPE_MAGNETIC_FIELD -> { //자기장
                mag = event.values
                var totalMag =sqrt((mag[0].pow(2) + mag[1].pow(2)+ mag[2].pow(2)).toDouble()).toFloat()

                magBuffer.add(totalMag)

                if (magBuffer.size > windowSize) {
                    magBuffer.removeAt(0)
                    val filteredtotalMag = magBuffer.average()
                    totalMag = filteredtotalMag.toFloat()
                }
                label_mag.text = resources.getString(R.string.label_totalMag, totalMag)
                entries.add(Entry(entries.size.toFloat(), totalMag))
                computeOrientation()

                //분산계산
                magBufferVar.add(totalMag)
                if (magBufferVar.size > varListSize) {
                    magBufferVar.removeAt(0)

                }else {
                   // varMag = calculateVariance(magBuffer)
                }
                varMag = calculateVariance(magBuffer)
                label_varMag.text = resources.getString(R.string.label_varMag, varMag)
                entries2.add(Entry(entries2.size.toFloat(), varMag.toFloat())) // mp그래프

                label_sample.text = resources.getString(R.string.label_sample, magBufferVar.size)

                if(magBufferVar.size >= varListSize){//case 확인
                    val dvarMag = last_varMag - varMag
                    last_varMag = varMag

                    if(varMag<0.1 && dvarMag<0.05){
                        case++
                        label_case.text = "정지확인중 $case"
                    }else if (varMag>2){
                        case = 0
                        label_case.text = "이동중"
                    }

                    if(case > 20){
                       label_case.text = "정지"
                    }}else{
                        label_case.text = "이동중"
                    }
            }
            Sensor.TYPE_ACCELEROMETER -> { //자기장센서값에필요
                acc = event.values
                computeOrientation()
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                line = event.values

                //X가속 평균필터
                accelerationBufferX.add(line[0])
                if (accelerationBufferX.size > windowSize) {
                    accelerationBufferX.removeAt(0)
                    val filteredAccelX = accelerationBufferX.average()
                    // 필터링된 결과를 사용하여 작업 수행
                    // 예: UI 업데이트, 다른 연산 등
                    nowAccX = filteredAccelX.toFloat()
                }
                //Y가속 평균필터
                accelerationBufferY.add(line[1])
                if (accelerationBufferY.size > windowSize) {
                    accelerationBufferY.removeAt(0)
                    val filteredAccelY = accelerationBufferY.average()
                    // 필터링된 결과를 사용하여 작업 수행
                    // 예: UI 업데이트, 다른 연산 등
                    nowAccY = filteredAccelY.toFloat()
                }


                tvLineX!!.text = resources.getString(R.string.label_lineX, nowAccX)
                tvLineY!!.text = resources.getString(R.string.label_lineY, nowAccY)

                nowAccX = round(nowAccX*10000)/10000  //소수점 네번째 까지 끊음
                nowAccY = round(nowAccY*10000)/10000

                val totalAcc =sqrt((nowAccX.pow(2) + nowAccY.pow(2)).toDouble()).toFloat()
                ValAcc =  String.format("%.4f", totalAcc).toFloat()
                tvValacc!!.text = "Total Acceleration : $ValAcc"


                getDiswT()

            }
            else -> { }
        }

    }

    private fun startDataUpdate() {  // mp 그래프 함수
        handler.post(object : Runnable {
            override fun run() {

                    updateChart()
                    updateChart2()
                    handler.postDelayed(this, 100) // 100ms마다 업데이트


            }
        })
    }

    private fun updateChart() {
        val dataSet = LineDataSet(entries, "Mag")
        dataSet.color = Color.BLUE
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)

        val lineDataSets: ArrayList<ILineDataSet> = ArrayList()
        lineDataSets.add(dataSet)

        val lineData = LineData(lineDataSets)

        lineChart.data = lineData
        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
    }

    private fun updateChart2() {
        val dataSet = LineDataSet(entries2, "MagVar")
        dataSet.color = Color.RED
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)

        val lineDataSets: ArrayList<ILineDataSet> = ArrayList()
        lineDataSets.add(dataSet)

        val lineData = LineData(lineDataSets)

        lineChart2.data = lineData
        lineChart2.notifyDataSetChanged()
        lineChart2.invalidate()
    }

    private fun stopDataUpdate() {      //mp 그래프 함수
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

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
    }

    private fun getDiswT() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - previousTime) / 1000.0

        //X
        nowSpeedX = recentSpeedX+nowAccX*(elapsedTime.toFloat())
        val avgSpeedX = (nowSpeedX+recentSpeedX) / 2
        distanceX = avgSpeedX*elapsedTime.toFloat()

        //Y
        nowSpeedY = recentSpeedY+nowAccY*elapsedTime.toFloat()
        val avgSpeedY = (nowSpeedY+recentSpeedY) / 2
        distanceY = avgSpeedY*elapsedTime.toFloat()

        //총 거리
        totalD += sqrt((distanceX).pow(2)+(distanceY).pow(2))

        //역간거리
        betweenStationDis+= sqrt((distanceX).pow(2)+(distanceY).pow(2))

        //이월
        recentSpeedX = nowSpeedX
        recentSpeedY = nowSpeedY

        previousTime = currentTime
    }


    private fun reset() {
        nowAccX = 0F
        nowAccY = 0f
        recentSpeedX = 0f
        recentSpeedY = 0f


        //현재까지 이동거리 저장
        data.add(betweenStationDis.toString())

        // 어댑터에 변경 사항 알림
        adapter.notifyDataSetChanged()

        betweenStationDis=0f
    }

    private fun calculateVariance(sensorValues: MutableList<Float>): Double {
        val mean = sensorValues.average()
        val squaredDiffSum = sensorValues.sumOf { (it - mean).pow(2) }
        val variance = squaredDiffSum / sensorValues.size
        return variance
    }

    fun calculateVariance2(list: MutableList<Float>): Double {
        val mean = list.average()
        val mean2 = mean*mean
        val squared = list.map { it * it }
        val squaredmean = squared.sum() / list.size
        val variance = squaredmean - mean2
        return variance
    }

    /*
    fun applyLowPassFilter(sensorData: List<Double>, cutoffFrequency: Double, samplingRate: Double): List<Double> {
        val filteredData = mutableListOf<Double>()
        val alpha = 2 * Math.PI * cutoffFrequency / samplingRate

        var previousOutput = 0.0

        for (i in sensorData.indices) {
            val currentInput = sensorData[i]
            val currentOutput = alpha * currentInput + (1 - alpha) * previousOutput

            filteredData.add(currentOutput)
            previousOutput = currentOutput
        }

        return filteredData
    }

     */





}


