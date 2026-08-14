package com.kogen.androidarc.demo.notes.domain.model

/** A single demo note - just enough fields to have something to list and to look at in details. */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
)
