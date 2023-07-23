package com.esgi.infrastructure.persistence.entities

import com.esgi.infrastructure.persistence.listeners.AuditableDates
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import java.util.*

@Entity
@DynamicUpdate
@Table(name = "T_PLAYERS")
class PlayerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "PLAYER_ID", nullable = false, columnDefinition = "UUID default gen_random_uuid()")
    val id: UUID? = null,

    @ManyToOne
    @JoinColumn(name = "PLAYER_USER_ID", referencedColumnName = "USER_ID")
    val user: UserEntity,

    @ManyToOne
    @JoinColumn(name = "PLAYER_ROOM_ID", referencedColumnName = "ROOM_ID")
    val room: RoomEntity,

    @Column(name = "PLAYER_INDEX", nullable = false, columnDefinition = "INT default NULL")
    var playerIndex: Int?,
) : AuditableDates()