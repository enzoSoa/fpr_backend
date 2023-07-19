package com.esgi.infrastructure.dto.mappers

import com.esgi.domainmodels.Room
import com.esgi.infrastructure.persistence.entities.RoomEntity
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface RoomMapper {
    fun toDomain(user: RoomEntity): Room
}