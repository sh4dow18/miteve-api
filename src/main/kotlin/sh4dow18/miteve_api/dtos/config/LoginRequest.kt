package sh4dow18.miteve_api.dtos.config

import com.fasterxml.jackson.annotation.JsonCreator

data class LoginRequest(
    var email: String,
    var password: String,
){
    @JsonCreator
    @Suppress("unused")
    constructor() : this("", "")
}
