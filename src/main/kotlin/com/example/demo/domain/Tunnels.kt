package com.example.demo.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class Tunnels(
    val publish_tunnels:List<childItem>
)

data class childItem(
    val name: String = "",
    val proto: String = "",
    val public_url: String = ""
)
