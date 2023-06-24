package com.esgi.infrastructure.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import java.security.Principal
import java.util.UUID

@Controller
@CrossOrigin(origins = ["http://localhost:1234", "https://jxy.me"], allowCredentials = "true")
class MessagesGateway(
    @Autowired
    private val template: SimpMessagingTemplate
) {

    @MessageMapping("/{id}/messages")
    fun writeToGroup(principal: UsernamePasswordAuthenticationToken, @DestinationVariable id: UUID, message: String?) {
        println("[$id]: $message\n$principal")
        template.convertAndSend("/groups/$id/messages", "[$id]: $message")
    }
}