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

data class RoomTypeCounts(
    @SerializedName("hgs") val hgs: Int = 0,
    @SerializedName("hgd") val hgd: Int = 0,
    @SerializedName("hso") val hso: Int = 0,
    @SerializedName("hsr") val hsr: Int = 0,
    @SerializedName("hpr") val hpr: Int = 0,
    @SerializedName("hts") val hts: Int = 0,
    @SerializedName("htd") val htd: Int = 0
)

data class RoomLogParseResult(
    @SerializedName("checkout") val checkout: RoomTypeCounts = RoomTypeCounts(),
    @SerializedName("stayover") val stayover: RoomTypeCounts = RoomTypeCounts()
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
            """

            [T동 특수 규칙]
            T동은 층에 따라 객실 타입이 다름:
            - 1층(1F) 객실 번호 100번대: HTD 타입
            - 2층(2F) 객실 번호 200번대: HTS 타입
            - 3층(3F) 객실 번호 300번대: HTS 타입
            - 4층(4F) 객실 번호 400번대: HTD 타입
            각 층 구역에서 노란색 형광펜 표시된 객실 수를 위 타입으로 분류해서 세어줘.
            """.trimIndent()
        } else ""

        val prompt = """
            이 사진은 호텔 객실 관리일지(하우스키핑 보고서)야. 여러 동(A동, B동, C동 등)이 한 페이지에 있을 수 있어.
            지금은 반드시 **${building}동** 섹션만 분석해줘. 다른 동의 객실은 세지 마.

            [STEP 1: ${building}동 위치 파악]
            - 표 상단에 "A동", "B동", "C동" 등 동 이름이 표시되어 있어
            - "${building}동"이라고 적힌 섹션을 찾아서 그 안의 객실만 분석해

            [STEP 2: 객실 타입 헤더 파악]
            - ${building}동 섹션 상단 헤더에 타입 코드가 있어: GS, GD, SO, SR, PR 등
            - 각 열(column)이 어떤 타입인지 먼저 파악해
            - 변환: GS→hgs, GD→hgd, SO→hso, SR→hsr, PR→hpr$tDongNote

            [STEP 3: 층별로 순서대로 객실 상태 분류]
            위층(9F, 8F...)부터 아래층(1F)까지 **한 층씩** 차례로 분석해:
            - 퇴실(checkout): 형광노랑(황색)으로만 칠해진 칸
              ※ 노랑이라도 ✓, ○, VD 표시가 있으면 checkout이 아니라 stayover!
            - 재실(stayover): 형광분홍/주황으로 칠해진 칸, 또는 노랑+체크/동그라미/VD 칸
            - 공실: 색 없는 빈 칸 → 무시

            [색상 구별 기준]
            - 노란색(황색): 밝고 선명한 노랑 → 기본적으로 퇴실
            - 분홍/마젠타색: 붉은 기가 있는 색 → 재실
            - 노랑인데 표시(✓/○/VD) 있음 → 재실
            - 색이 없거나 흰색 → 공실(무시)

            [최종 집계]
            층별 분석이 끝난 후, 타입별로 checkout과 stayover 합산해서 JSON으로만 반환.
            설명 없이 JSON만.
            형식: {"checkout": {"hgs": 숫자, "hgd": 숫자, "hso": 숫자, "hsr": 숫자, "hpr": 숫자, "hts": 숫자, "htd": 숫자}, "stayover": {"hgs": 숫자, "hgd": 숫자, "hso": 숫자, "hsr": 숫자, "hpr": 숫자, "hts": 숫자, "htd": 숫자}}
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