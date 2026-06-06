package com.xiaoyuezhu.app.engine

import android.content.Context
import android.graphics.Bitmap
import com.xiaoyuezhu.app.domain.model.AnswerItemJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperGenerator @Inject constructor() {

    private val layoutEngine = LayoutEngine()
    private val canvasDrawer = CanvasDrawer()
    private val json = Json { prettyPrint = true }

    suspend fun generate(
        context: Context,
        spec: PaperSpec,
        correctAnswers: List<List<String>>
    ): GenerateResult {
        Timber.i("开始生成答题卡: ${spec.title}")

        val layout = layoutEngine.calculate(spec)
        val (bitmap, templateJson) = canvasDrawer.draw(layout, spec)

        val papersDir = File(context.filesDir, "papers")
        if (!papersDir.exists()) papersDir.mkdirs()

        val imageName = "paper_${UUID.randomUUID().toString().take(8)}.png"
        val imageFile = File(papersDir, imageName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val answerItems = correctAnswers.mapIndexed { index, options ->
            AnswerItemJson(questionIndex = index, selectedOptions = options.sorted())
        }
        val correctAnswerJson = json.encodeToString(answerItems)
        val templateJsonStr = json.encodeToString(templateJson)

        Timber.i("答题卡生成完成: image=${imageFile.absolutePath}")

        return GenerateResult(
            templateJson = templateJsonStr,
            correctAnswerJson = correctAnswerJson,
            imagePath = imageFile.absolutePath
        )
    }
}
