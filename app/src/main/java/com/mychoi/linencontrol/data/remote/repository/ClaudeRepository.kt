package com.mychoi.linencontrol.data.remote.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mychoi.linencontrol.data.remote.api.ClaudeApiService
import com.mychoi.linencontrol.data.remote.model.ClaudeRequest
import com.mychoi.linencontrol.data.remote.model.ContentItem
import com.mychoi.linencontrol.data.remote.model.ImageSource

data class InventoryParseResult(
    @SerializedName("한실이불피") val 한실이불피: Int = 0,
    @SerializedName("요피") val 요피: Int = 0,
    @SerializedName("한실베개피") val 한실베개피: Int = 0,
    @SerializedName("양실이불피") val 양실이불피: Int = 0,
    @SerializedName("시트피") val 시트피: Int = 0,
    @SerializedName("양실베개피") val 양실베개피: Int = 0,
    @SerializedName("ft") val ft: Int = 0,
    @SerializedName("bt") val bt: Int = 0,
    @SerializedName("걸레") val 걸레: Int = 0
)

data class RoomLogParseResult(
    val hgs: Int = 0,
    val hgd: Int = 0,
    val hso: Int = 0,
    val hsr: Int = 0,
    val hpr: Int = 0,
    val hts: Int = 0,
    val htd: Int = 0
)

class ClaudeRepository(private val apiService: ClaudeApiService) {

    private val gson = Gson()

    suspend fun parseInventorySheet(imageBase64: String): Result<InventoryParseResult> = runCatching {
        val prompt = """
            이 사진은 호텔 린넨 재고 시트입니다.
            아래 항목들의 수량을 읽어서 JSON 형식으로만 반환해줘. 설명 없이 JSON만.
            항목: 한실이불피, 요피, 한실베개피, 양실이불피, 시트피, 양실베개피, ft(페이스타올/FT/FIt), bt(배스타올/BT), 걸레
            형식: {"한실이불피": 숫자, "요피": 숫자, "한실베개피": 숫자, "양실이불피": 숫자, "시트피": 숫자, "양실베개피": 숫자, "ft": 숫자, "bt": 숫자, "걸레": 숫자}
        """.trimIndent()

        val response = apiService.sendMessage(buildRequest(imageBase64, prompt))
        val json = extractJson(response.getText())
        gson.fromJson(json, InventoryParseResult::class.java)
    }

    suspend fun parseRoomLog(imageBase64: String, building: String): Result<RoomLogParseResult> = runCatching {
        val tDongNote = if (building == "T") {
            "\nT동의 경우: 1층(1F)과 4층(4F) 객실은 HTD 타입, 2층(2F)과 3층(3F) 객실은 HTS 타입이야."
        } else ""
        val prompt = """
            이 사진은 호텔 객실 관리일지입니다.
            ${building}동에서 노란색 형광펜으로 표시된 체크아웃 객실을 타입별로 세어줘.
            각 구역 상단에 GS, GD, SO, SR, PR 등 타입이 표시되어 있어. 앞에 H를 붙여서 구분해: GS→HGS, GD→HGD, SO→HSO, SR→HSR, PR→HPR.$tDongNote
            JSON 형식으로만 반환해줘. 설명 없이 JSON만.
            형식: {"hgs": 숫자, "hgd": 숫자, "hso": 숫자, "hsr": 숫자, "hpr": 숫자, "hts": 숫자, "htd": 숫자}
        """.trimIndent()

        val response = apiService.sendMessage(buildRequest(imageBase64, prompt))
        val json = extractJson(response.getText())
        gson.fromJson(json, RoomLogParseResult::class.java)
    }

    private fun buildRequest(imageBase64: String, prompt: String): ClaudeRequest =
        ClaudeRequest(
            messages = listOf(
                ClaudeRequest.Message(
                    content = listOf(
                        ContentItem(
                            type = "image",
                            source = ImageSource(data = imageBase64)
                        ),
                        ContentItem(
                            type = "text",
                            text = prompt
                        )
                    )
                )
            )
        )

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1) text.substring(start, end + 1) else text
    }
}