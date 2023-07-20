package com.esgi.infrastructure.dto.output

import com.esgi.infrastructure.dto.input.RTCDescriptionDto
import java.util.UUID

data class NewAnswerMessageResponseDto(
    val answer: RTCDescriptionDto,
    val from: UUID
)
