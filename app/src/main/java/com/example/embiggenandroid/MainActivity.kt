package com.example.embiggenandroid

import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var scoreText: TextView
    private lateinit var liveText: TextView
    private lateinit var paddle: View
    private lateinit var ball: View
    private lateinit var brickContainer: LinearLayout
    private val brickRows = 7
    private val brickColumns = 6
    private val brickWidth = 111
    private val brickHeight = 40
    private val brickMargin = 4
    private var ballX = 0f
    private var ballY = 0f
    private var ballSpeedX = 0
    private var ballSpeedY = 0
    private var paddleX = 0f
    private var score = 0
    private var lives = 3
    private var execute = false
    private var firstGame = true

    // Configuración sonidos:
    private val breakSound by lazy { assets.openFd("break.mp3") }
    private val executeBreakSound by lazy {
        val m = MediaPlayer()
        m.setDataSource(
            breakSound.fileDescriptor, breakSound.startOffset, breakSound.length
        )
        breakSound.close()
        m.prepare()
        m
    }
    private val doh by lazy { assets.openFd("doh.mp3") }
    private val executeDoh by lazy {
        val m = MediaPlayer()
        m.setDataSource(
            doh.fileDescriptor, doh.startOffset, doh.length
        )
        doh.close()
        m.prepare()
        m
    }
    private val boing by lazy { assets.openFd("boing.mp3") }
    private val executeBoing by lazy {
        val m = MediaPlayer()
        m.setDataSource(
            boing.fileDescriptor, boing.startOffset, boing.length
        )
        boing.close()
        m.prepare()
        m
    }
    private val brutal by lazy { assets.openFd("brutal.mp3") }
    private val executeBrutal by lazy {
        val m = MediaPlayer()
        m.setDataSource(
            brutal.fileDescriptor, brutal.startOffset, brutal.length
        )
        brutal.close()
        m.prepare()
        m
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scoreText = findViewById(R.id.scoreText)
        liveText = findViewById(R.id.liveText)
        paddle = findViewById(R.id.paddle)
        ball = findViewById(R.id.ball)
        brickContainer = findViewById(R.id.brickContainer)
        val newgame = findViewById<Button>(R.id.newgame)
        newgame.text = "Juego nuevo"
        executeBrutal.start()
        executeBoing.start()
        newgame.setOnClickListener {
            execute = true
            lives = 3
            score = 0
            ball.visibility = View.VISIBLE
            initializeBricks()
            if (firstGame) {
                start()
                firstGame = false
            } else {
                resetBallPosition()
            }
            newgame.visibility = View.INVISIBLE
        }
    }

    private fun initializeBricks() {
        brickContainer.removeAllViews()
        for (row in 0 until brickRows) {
            val rowLayout = LinearLayout(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            rowLayout.layoutParams = params
            for (col in 0 until brickColumns) {
                val brick = View(this)
                val brickParams = LinearLayout.LayoutParams(brickWidth, brickHeight)
                brickParams.setMargins(brickMargin, brickMargin, brickMargin, brickMargin)
                brick.layoutParams = brickParams
                if (col % 2 == 0) {
                    brick.setBackgroundResource(R.drawable.redbrick)
                } else {
                    brick.setBackgroundResource(R.drawable.orangebrick)
                }
                rowLayout.addView(brick)
            }
            brickContainer.addView(rowLayout)
        }
    }

    private fun moveBall() {
        ballX += ballSpeedX
        ballY += ballSpeedY
        ball.x = ballX
        ball.y = ballY
    }

    private fun movePaddle(x: Float) {
        paddleX = x - paddle.width / 2
        paddle.x = paddleX
    }

    private fun checkCollision() {
        scoreText.text = "Puntaje: $score"
        liveText.text = "Vidas: $lives"
        // Verifica si gano:
        if (score == brickColumns * brickRows) {
            gameWin()
        }
        // Colisión con las paredes:
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        if (ballX <= 0 || ballX + ball.width >= screenWidth) {
            ballSpeedX *= -1
        }
        if (ballY <= 0) {
            ballSpeedY *= -1
        }
        // Colisión con la barra:
        if (ballY + ball.height >= paddle.y && ballY + ball.height <= paddle.y + paddle.height
            && ballX + ball.width >= paddle.x && ballX <= paddle.x + paddle.width
        ) {
            executeBoing.start()
            ballSpeedY *= -1
        }
        // Colisión con el tope de la pantalla:
        if (ballY + ball.height >= screenHeight) {
            resetBallPosition()
        }
        // Colisión con los ladrillos:
        for (row in 0 until brickRows) {
            val rowLayout = brickContainer.getChildAt(row) as LinearLayout
            val rowTop = rowLayout.y + brickContainer.y
            for (col in 0 until brickColumns) {
                val brick = rowLayout.getChildAt(col) as View
                if (brick.visibility == View.VISIBLE) {
                    val brickLeft = brick.x + rowLayout.x
                    val brickRight = brickLeft + brick.width
                    val brickTop = brick.y + rowTop
                    val brickBottom = brickTop + brick.height
                    if (ballX + ball.width >= brickLeft && ballX <= brickRight
                        && ballY + ball.height >= brickTop && ballY <= brickBottom
                    ) {
                        brick.visibility = View.INVISIBLE
                        ballSpeedY *= -1
                        score++
                        executeBreakSound.start()
                        return
                    }
                }
            }
        }
        // Colisión cuando pierdes:
        if (ballY + ball.height >= screenHeight - 100) {
            if (lives > 0) {
                lives--
                executeDoh.start()
            }
            paddle.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        movePaddle(event.rawX)
                    }
                }
                true
            }
            if (lives <= 0) {
                gameOver()
            } else {
                resetBallPosition()
                // start()
            }
        }
    }

    private fun gameOver() {
        execute = false
        resetBallPosition()
        val newgame = findViewById<Button>(R.id.newgame)
        newgame.text = "Internar nuevamente"
        newgame.visibility = View.VISIBLE
    }

    private fun gameWin() {
        execute = false
        ballSpeedY *= -1
        ball.visibility = View.INVISIBLE
        resetBallPosition()
        val newgame = findViewById<Button>(R.id.newgame)
        newgame.text = "Felicitaciones Ganaste!!! \uD83C\uDFC6 \nJuega denuevo"
        newgame.visibility = View.VISIBLE
    }

    private fun movepaddle() {
        paddle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    movePaddle(event.rawX)
                }
            }
            true
        }
    }

    private fun resetBallPosition() {
        if (execute) {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            ballX = screenWidth / 2
            ballY = screenHeight / 2
            ballSpeedX = 8
            ballSpeedY = -8
        }
    }

    private fun start() {
        if (execute) {
            movepaddle()
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            paddleX = screenWidth / 2 - paddle.width / 2
            paddle.x = paddleX
            ballX = screenWidth / 2
            ballY = screenHeight / 2
            ballSpeedX = 8
            ballSpeedY = -8
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = Long.MAX_VALUE
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener { animation ->
                moveBall()
                checkCollision()
            }
            animator.start()
        }
    }
}