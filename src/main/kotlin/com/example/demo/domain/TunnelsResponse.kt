package com.example.demo.domain

data class TunnelsResponse(
    val data: TunnelsData = TunnelsData()
)

data class TunnelsData(
    val items: List<Tunnels> = emptyList()
)
